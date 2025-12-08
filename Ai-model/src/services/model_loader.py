"""
Model Loader - Multitask Judge Model Only
Custom Transformer trained on 400K samples with 3 tasks: GENERATE, EVALUATE, REPORT
"""
import time
import logging
import warnings
import torch
import torch.nn as nn
import math
from pathlib import Path, PurePosixPath
import pathlib
import platform
import sentencepiece as spm

# Suppress PyTorch transformer warnings
warnings.filterwarnings("ignore", message=".*enable_nested_tensor.*")
warnings.filterwarnings("ignore", message=".*Support for mismatched key_padding_mask.*")

# Fix PosixPath issue when loading model saved on Linux/Mac to Windows
if platform.system() == 'Windows':
    pathlib.PosixPath = pathlib.WindowsPath

from src.core.config import MULTITASK_JUDGE_MODEL_PATH

logger = logging.getLogger(__name__)


# Device types
DEVICE_CUDA = "cuda"
DEVICE_MPS = "mps"
DEVICE_CPU = "cpu"


class MultitaskJudgeTransformer(nn.Module):
    """
    Multitask Judge Transformer model
    Trained on 400K samples with 3 tasks: GENERATE, EVALUATE, REPORT
    Architecture: d_model=512, nhead=8, num_layers=8
    """
    def __init__(self, vocab_size=11555, d_model=512, nhead=8, num_layers=8, max_len=2000, dropout=0.1):
        super().__init__()
        self.d_model = d_model
        self.max_len = max_len
        self.embedding = nn.Embedding(vocab_size, d_model)
        self.pos_encoder = nn.Parameter(torch.zeros(1, max_len, d_model))
        
        self.transformer = nn.Transformer(
            d_model=d_model, nhead=nhead,
            num_encoder_layers=num_layers, num_decoder_layers=num_layers,
            dim_feedforward=d_model*4, dropout=dropout,
            batch_first=True, norm_first=True
        )
        self.fc_out = nn.Linear(d_model, vocab_size)
        
    def forward(self, src, tgt):
        # Create padding masks and convert to float to match attn_mask dtype
        src_mask = (src == 0).float().masked_fill((src == 0), float('-inf'))
        tgt_mask = (tgt == 0).float().masked_fill((tgt == 0), float('-inf'))
        tgt_causal_mask = self.transformer.generate_square_subsequent_mask(tgt.size(1)).to(src.device)
        
        src_emb = self.embedding(src) * math.sqrt(self.d_model) + self.pos_encoder[:, :src.size(1), :]
        tgt_emb = self.embedding(tgt) * math.sqrt(self.d_model) + self.pos_encoder[:, :tgt.size(1), :]
        
        out = self.transformer(src_emb, tgt_emb, tgt_mask=tgt_causal_mask,
                               src_key_padding_mask=src_mask, tgt_key_padding_mask=tgt_mask)
        return self.fc_out(out)


class MultitaskJudgeModelManager:
    """
    Quản lý Multitask Judge model (trained from scratch with 400K samples)
    Supports 3 tasks:
        - GENERATE: Generate follow-up questions
        - EVALUATE: Evaluate answer quality with JSON scores
        - REPORT: Generate overall feedback report
    """
    
    def __init__(self):
        self.model = None
        self.tokenizer = None
        self.model_path = MULTITASK_JUDGE_MODEL_PATH
        self.device = None
        self.vocab_size = 11555
        self.d_model = 512
        self.nhead = 8
        self.num_layers = 8
        self.max_len = 1124
        
    def _detect_device(self):
        """Tự động phát hiện thiết bị tốt nhất"""
        if torch.cuda.is_available():
            gpu_name = torch.cuda.get_device_name(0)
            gpu_memory = torch.cuda.get_device_properties(0).total_memory / (1024**3)
            logger.info(f"[MultitaskJudge] Detected GPU: {gpu_name}")
            logger.info(f"[MultitaskJudge] VRAM: {gpu_memory:.2f} GB")
            return DEVICE_CUDA
        elif hasattr(torch.backends, 'mps') and torch.backends.mps.is_available():
            logger.info("[MultitaskJudge] Detected Apple Silicon GPU (MPS)")
            return DEVICE_MPS
        else:
            logger.info("[MultitaskJudge] No GPU detected, using CPU")
            return DEVICE_CPU
    
    def load(self):
        """Tải Multitask Judge model và SentencePiece tokenizer"""
        logger.info("="*60)
        logger.info("Loading Multitask Judge Model...")
        logger.info(f"[MultitaskJudge] Model path: {self.model_path}")
        logger.info("="*60)
        
        try:
            start_time = time.time()
            
            # Detect device
            self.device = self._detect_device()
            
            # Load SentencePiece tokenizer
            tokenizer_path = self.model_path / "tokenizer.model"
            if not tokenizer_path.exists():
                raise FileNotFoundError(f"Tokenizer not found: {tokenizer_path}")
            
            self.tokenizer = spm.SentencePieceProcessor()
            self.tokenizer.load(str(tokenizer_path))
            actual_vocab_size = self.tokenizer.vocab_size()
            logger.info(f"[MultitaskJudge] SentencePiece tokenizer loaded (vocab_size={actual_vocab_size})")
            
            # Use actual vocab size from tokenizer
            self.vocab_size = actual_vocab_size
            
            # Load weights
            weights_path = self.model_path / "judge_model.pth"
            if not weights_path.exists():
                # Try alternative names
                for alt_name in ["judge_epoch_4.pth", "judge_epoch_3.pth", "model.pth"]:
                    alt_path = self.model_path / alt_name
                    if alt_path.exists():
                        weights_path = alt_path
                        break
            
            if not weights_path.exists():
                raise FileNotFoundError(f"Model weights not found in: {self.model_path}")
            
            logger.info(f"[MultitaskJudge] Loading weights from: {weights_path.name}")
            checkpoint = torch.load(weights_path, map_location=self.device, weights_only=False)
            
            # Handle different checkpoint formats
            if isinstance(checkpoint, dict):
                if "model_state_dict" in checkpoint:
                    state_dict = checkpoint["model_state_dict"]
                    if "config" in checkpoint:
                        config = checkpoint["config"]
                        logger.info(f"[MultitaskJudge] Checkpoint: epoch={checkpoint.get('epoch')}, val_loss={checkpoint.get('val_loss'):.4f}")
                        if "d_model" in config:
                            self.d_model = config["d_model"]
                        if "nhead" in config:
                            self.nhead = config["nhead"]
                        if "num_layers" in config:
                            self.num_layers = config["num_layers"]
                        if "vocab_size" in config:
                            self.vocab_size = config["vocab_size"]
                        if "max_len" in config:
                            self.max_len = config["max_len"]
                else:
                    state_dict = checkpoint
            else:
                state_dict = checkpoint
            
            # Remove 'module.' prefix if exists (from DataParallel training)
            state_dict = {k.replace("module.", ""): v for k, v in state_dict.items()}
            
            # Infer max_len from pos_encoder shape
            if "pos_encoder" in state_dict:
                pos_encoder_shape = state_dict["pos_encoder"].shape
                self.max_len = pos_encoder_shape[1]
                logger.info(f"[MultitaskJudge] Inferred max_len: {self.max_len}")
            
            # Initialize model
            self.model = MultitaskJudgeTransformer(
                vocab_size=self.vocab_size,
                d_model=self.d_model,
                nhead=self.nhead,
                num_layers=self.num_layers,
                max_len=self.max_len,
                dropout=0.1
            )
            
            self.model.load_state_dict(state_dict)
            self.model = self.model.to(self.device)
            self.model.eval()
            
            load_time = time.time() - start_time
            logger.info(f"[MultitaskJudge] Model loaded in {load_time:.2f}s")
            logger.info(f"[MultitaskJudge] Architecture: d_model={self.d_model}, nhead={self.nhead}, layers={self.num_layers}")
            logger.info(f"[MultitaskJudge] Device: {self.device}")
            logger.info(f"[MultitaskJudge] Tasks: GENERATE, EVALUATE, REPORT")
            
            # Log GPU memory
            if self.device == DEVICE_CUDA:
                allocated = torch.cuda.memory_allocated(0) / (1024**3)
                reserved = torch.cuda.memory_reserved(0) / (1024**3)
                logger.info(f"[MultitaskJudge] VRAM: {allocated:.2f} GB / {reserved:.2f} GB reserved")
            
            # Count parameters
            total_params = sum(p.numel() for p in self.model.parameters())
            logger.info(f"[MultitaskJudge] Total parameters: {total_params:,}")
            
        except Exception as e:
            logger.error(f"[MultitaskJudge] Failed to load model: {e}")
            raise
    
    def cleanup(self):
        """Dọn dẹp tài nguyên"""
        logger.info("[MultitaskJudge] Cleaning up...")
        if self.model:
            del self.model
            self.model = None
        if self.tokenizer:
            del self.tokenizer
            self.tokenizer = None
        
        if self.device == DEVICE_CUDA and torch.cuda.is_available():
            torch.cuda.empty_cache()
            logger.info("[MultitaskJudge] VRAM cache cleared")
        elif self.device == DEVICE_MPS and hasattr(torch.backends, 'mps'):
            if hasattr(torch.mps, 'empty_cache'):
                torch.mps.empty_cache()
    
    def is_loaded(self) -> bool:
        """Kiểm tra xem model đã được tải chưa"""
        return self.model is not None and self.tokenizer is not None
    
    def get_model(self):
        """Lấy model đã tải"""
        if not self.is_loaded():
            raise RuntimeError("Model not loaded")
        return self.model
    
    def get_tokenizer(self):
        """Lấy SentencePiece tokenizer đã tải"""
        if not self.is_loaded():
            raise RuntimeError("Tokenizer not loaded")
        return self.tokenizer
    
    def get_device(self):
        """Lấy thông tin device đang sử dụng"""
        return self.device if self.device else "unknown"
    
    def encode(self, text: str, max_length: int = 512) -> torch.Tensor:
        """Encode text to tensor using SentencePiece"""
        if not self.is_loaded():
            raise RuntimeError("Tokenizer not loaded")
        
        tokens = self.tokenizer.encode(text)
        if len(tokens) > max_length:
            tokens = tokens[:max_length]
        
        return torch.tensor([tokens], dtype=torch.long, device=self.device)
    
    def decode(self, tokens) -> str:
        """Decode tokens to text using SentencePiece"""
        if not self.is_loaded():
            raise RuntimeError("Tokenizer not loaded")
        
        if isinstance(tokens, torch.Tensor):
            tokens = tokens.squeeze().tolist()
        if isinstance(tokens, int):
            tokens = [tokens]
            
        return self.tokenizer.decode(tokens)
    
    @torch.no_grad()
    def generate(self, input_text: str, max_new_tokens: int = 256, temperature: float = 0.7, use_greedy: bool = False) -> str:
        """
        Generate output for a given input text with task prefix.
        Optimized with encoder output caching.
        
        Args:
            input_text: Input with task prefix, e.g., "[TASK:EVALUATE] Question: ... Answer: ..."
            max_new_tokens: Maximum tokens to generate
            temperature: Sampling temperature (higher = more random)
            use_greedy: If True, use greedy decoding (deterministic) instead of sampling
            
        Returns:
            Generated text
        """
        if not self.is_loaded():
            raise RuntimeError("Model not loaded")
        
        # Encode input
        src = self.encode(input_text)
        
        # Get special token IDs
        bos_id = self.tokenizer.bos_id() if self.tokenizer.bos_id() >= 0 else 1
        eos_id = self.tokenizer.eos_id() if self.tokenizer.eos_id() >= 0 else 2
        
        # Pre-compute encoder output (cache for reuse)
        # Convert boolean mask to float mask to match attn_mask dtype
        src_padding = (src == 0)
        src_mask = src_padding.float().masked_fill(src_padding, float('-inf'))
        src_emb = self.model.embedding(src) * math.sqrt(self.model.d_model) + self.model.pos_encoder[:, :src.size(1), :]
        encoder_output = self.model.transformer.encoder(src_emb, src_key_padding_mask=src_mask)
        
        # Start with BOS token
        tgt = torch.tensor([[bos_id]], dtype=torch.long, device=self.device)
        generated_tokens = []
        
        for _ in range(max_new_tokens):
            # Compute decoder with cached encoder output
            # Convert boolean mask to float mask to match attn_mask dtype
            tgt_padding = (tgt == 0)
            tgt_mask = tgt_padding.float().masked_fill(tgt_padding, float('-inf'))
            tgt_causal_mask = self.model.transformer.generate_square_subsequent_mask(tgt.size(1)).to(self.device)
            tgt_emb = self.model.embedding(tgt) * math.sqrt(self.model.d_model) + self.model.pos_encoder[:, :tgt.size(1), :]
            
            decoder_output = self.model.transformer.decoder(
                tgt_emb, encoder_output,
                tgt_mask=tgt_causal_mask,
                tgt_key_padding_mask=tgt_mask,
                memory_key_padding_mask=src_mask
            )
            
            # Get logits for last token
            logits = self.model.fc_out(decoder_output[:, -1, :])
            
            # Select next token
            if use_greedy:
                # Greedy decoding: always pick the most likely token (deterministic)
                next_token = logits.argmax(dim=-1, keepdim=True)
            else:
                # Sampling with temperature
                logits = logits / temperature
                probs = torch.softmax(logits, dim=-1)
                next_token = torch.multinomial(probs, num_samples=1)
            
            # Check for EOS
            if next_token.item() == eos_id:
                break
            
            generated_tokens.append(next_token.item())
            tgt = torch.cat([tgt, next_token], dim=1)
        
        return self.decode(generated_tokens)


# Global instance
multitask_judge_manager = MultitaskJudgeModelManager()
