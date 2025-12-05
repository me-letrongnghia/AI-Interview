import time
import logging
import torch
import torch.nn as nn
import math
from pathlib import Path
from transformers import AutoModelForCausalLM, AutoTokenizer
import sentencepiece as spm

from src.core.config import MODEL_PATH, JUDGE_MODEL_PATH, CUSTOM_JUDGE_MODEL_PATH, MAX_TOKENS_DEFAULT, TEMPERATURE_DEFAULT

logger = logging.getLogger(__name__)


# Device types
DEVICE_CUDA = "cuda"
DEVICE_MPS = "mps"
DEVICE_CPU = "cpu"

# Memory limits
CPU_MAX_MEMORY = "6GB"

# Performance estimates (seconds per question)
PERF_GPU = "2-4s"
PERF_MPS = "5-8s"
PERF_CPU = "12-15s"


class ModelManager:
    """Quản lý việc tải model GenQ và vòng đời của nó"""
    
    def __init__(self):
        self.model = None
        self.tokenizer = None
        self.model_path = str(MODEL_PATH)
        self.device = None
        
    def _detect_device(self):
        """Tự động phát hiện thiết bị tốt nhất (GPU hoặc CPU)"""
        if torch.cuda.is_available():
            gpu_name = torch.cuda.get_device_name(0)
            gpu_memory = torch.cuda.get_device_properties(0).total_memory / (1024**3)
            logger.info(f"Phat hien GPU: {gpu_name}")
            logger.info(f"VRAM: {gpu_memory:.2f} GB")
            return DEVICE_CUDA
        elif hasattr(torch.backends, 'mps') and torch.backends.mps.is_available():
            logger.info("Phat hien Apple Silicon GPU (MPS)")
            return DEVICE_MPS
        else:
            logger.info("Khong phat hien GPU, su dung CPU")
            return DEVICE_CPU
    
    def _load_cuda_model(self):
        """Load model for CUDA GPU"""
        logger.info("Dang tai model cho GPU (float16 de tiet kiem VRAM)")
        self.model = AutoModelForCausalLM.from_pretrained(
            self.model_path,
            torch_dtype=torch.float16,
            device_map="auto",
            trust_remote_code=True
        ).eval()
    
    def _load_mps_model(self):
        """Load model for Apple Silicon MPS"""
        logger.info("Dang tai model cho Apple Silicon GPU")
        self.model = AutoModelForCausalLM.from_pretrained(
            self.model_path,
            torch_dtype=torch.float32,
            trust_remote_code=True
        ).to(DEVICE_MPS).eval()
    
    def _load_cpu_model(self):
        """Load model for CPU with memory optimization"""
        logger.info("Dang tai model cho CPU (toi uu bo nho)")
        logger.info(f"Toi uu hoa: max_tokens={MAX_TOKENS_DEFAULT}, temp={TEMPERATURE_DEFAULT}, num_beams=1")
        logger.info("Dang su dung che do tai tung phan de giam ap luc RAM...")
        
        # Create offload directory in project (D: drive instead of C:)
        project_root = Path(__file__).parent.parent.parent
        offload_path = project_root / "offload_temp"
        offload_path.mkdir(exist_ok=True)
        logger.info(f"Offload folder: {offload_path}")
        
        self.model = AutoModelForCausalLM.from_pretrained(
            self.model_path,
            torch_dtype=torch.float32,
            low_cpu_mem_usage=True,
            trust_remote_code=True,
            max_memory={0: CPU_MAX_MEMORY},
            offload_folder=str(offload_path),
            offload_state_dict=True
        ).eval()
    
    def _log_gpu_memory(self):
        """Log GPU memory usage"""
        allocated = torch.cuda.memory_allocated(0) / (1024**3)
        reserved = torch.cuda.memory_reserved(0) / (1024**3)
        logger.info(f"VRAM su dung: {allocated:.2f} GB / Dat truoc: {reserved:.2f} GB")
        
    def load(self):
        """Tải model và tokenizer"""
        logger.info("Khoi dong AI Interview GenQ Service...")
        logger.info(f"Dang tai model tu: {self.model_path}")
        
        try:
            start_time = time.time()
            
            # Tự động phát hiện device
            self.device = self._detect_device()
            
            # Tải tokenizer
            self.tokenizer = AutoTokenizer.from_pretrained(
                self.model_path, 
                use_fast=True, 
                trust_remote_code=True
            )
            logger.info("Tokenizer da tai thanh cong")
            
            # Cấu hình tải model dựa trên device
            if self.device == DEVICE_CUDA:
                self._load_cuda_model()
                logger.info(f"Toi uu hoa: Du kien {PERF_GPU}/cau hoi tren GPU")
                
            elif self.device == DEVICE_MPS:
                self._load_mps_model()
                logger.info(f"Toi uu hoa: Du kien {PERF_MPS}/cau hoi tren MPS")
                
            else:
                self._load_cpu_model()
                logger.info(f"Toi uu hoa: Du kien {PERF_CPU}/cau hoi tren CPU (cai thien tu 32s)")
            
            load_time = time.time() - start_time
            logger.info(f"Model da tai xong trong {load_time:.2f}s")
            logger.info(f"Thiet bi dang su dung: {self.device}")
            
            # Hiển thị thông tin bộ nhớ nếu là GPU
            if self.device == DEVICE_CUDA:
                self._log_gpu_memory()
            
        except Exception as e:
            logger.error(f"Loi khi tai model: {e}")
            raise
    
    def cleanup(self):
        """Dọn dẹp tài nguyên model"""
        logger.info("Dang tat GenQ Service...")
        if self.model:
            del self.model
        if self.tokenizer:
            del self.tokenizer
        
        # Dọn dẹp cache GPU nếu có
        if self.device == DEVICE_CUDA and torch.cuda.is_available():
            torch.cuda.empty_cache()
            logger.info("Da don dep VRAM cache")
        elif self.device == DEVICE_MPS and hasattr(torch.backends, 'mps'):
            if hasattr(torch.mps, 'empty_cache'):
                torch.mps.empty_cache()
                logger.info("Da don dep MPS cache")
    
    def is_loaded(self) -> bool:
        """Kiểm tra xem model đã được tải chưa"""
        return self.model is not None and self.tokenizer is not None
    
    def get_model(self):
        """Lấy model đã tải"""
        if not self.is_loaded():
            raise RuntimeError("Model chua duoc tai")
        return self.model
    
    def get_tokenizer(self):
        """Lấy tokenizer đã tải"""
        if not self.is_loaded():
            raise RuntimeError("Tokenizer chua duoc tai")
        return self.tokenizer
    
    def get_device(self):
        """Lấy thông tin device đang sử dụng"""
        return self.device if self.device else "unknown"


class JudgeModelManager:
    """Quản lý việc tải model Judge và vòng đời của nó"""
    
    def __init__(self):
        self.model = None
        self.tokenizer = None
        self.model_path = str(JUDGE_MODEL_PATH)
        self.device = None
        
    def _detect_device(self):
        """Tự động phát hiện thiết bị tốt nhất (GPU hoặc CPU)"""
        if torch.cuda.is_available():
            gpu_name = torch.cuda.get_device_name(0)
            gpu_memory = torch.cuda.get_device_properties(0).total_memory / (1024**3)
            logger.info(f"[Judge] Phat hien GPU: {gpu_name}")
            logger.info(f"[Judge] VRAM: {gpu_memory:.2f} GB")
            return DEVICE_CUDA
        elif hasattr(torch.backends, 'mps') and torch.backends.mps.is_available():
            logger.info("[Judge] Phat hien Apple Silicon GPU (MPS)")
            return DEVICE_MPS
        else:
            logger.info("[Judge] Khong phat hien GPU, su dung CPU")
            return DEVICE_CPU
    
    def _load_cuda_model(self):
        """Load model for CUDA GPU"""
        logger.info("[Judge] Dang tai model cho GPU (float16 de tiet kiem VRAM)")
        self.model = AutoModelForCausalLM.from_pretrained(
            self.model_path,
            torch_dtype=torch.float16,
            device_map="auto",
            trust_remote_code=True
        ).eval()
    
    def _load_mps_model(self):
        """Load model for Apple Silicon MPS"""
        logger.info("[Judge] Dang tai model cho Apple Silicon GPU")
        self.model = AutoModelForCausalLM.from_pretrained(
            self.model_path,
            torch_dtype=torch.float32,
            trust_remote_code=True
        ).to(DEVICE_MPS).eval()
    
    def _load_cpu_model(self):
        """Load model for CPU with memory optimization"""
        logger.info("[Judge] Dang tai model cho CPU (toi uu bo nho)")
        logger.info(f"[Judge] Toi uu hoa: max_tokens={MAX_TOKENS_DEFAULT}, temp={TEMPERATURE_DEFAULT}, num_beams=1")
        logger.info("[Judge] Dang su dung che do tai tung phan de giam ap luc RAM...")
        
        # Create offload directory in project (D: drive instead of C:)
        project_root = Path(__file__).parent.parent.parent
        offload_path = project_root / "offload_temp" / "judge"
        offload_path.mkdir(parents=True, exist_ok=True)
        logger.info(f"[Judge] Offload folder: {offload_path}")
        
        self.model = AutoModelForCausalLM.from_pretrained(
            self.model_path,
            torch_dtype=torch.float32,
            low_cpu_mem_usage=True,
            trust_remote_code=True,
            max_memory={0: CPU_MAX_MEMORY},
            offload_folder=str(offload_path),
            offload_state_dict=True
        ).eval()
    
    def _log_gpu_memory(self):
        """Log GPU memory usage"""
        allocated = torch.cuda.memory_allocated(0) / (1024**3)
        reserved = torch.cuda.memory_reserved(0) / (1024**3)
        logger.info(f"[Judge] VRAM su dung: {allocated:.2f} GB / Dat truoc: {reserved:.2f} GB")
        
    def load(self):
        """Tải model và tokenizer"""
        logger.info("Khoi dong AI Interview Judge Service...")
        logger.info(f"[Judge] Dang tai model tu: {self.model_path}")
        
        try:
            start_time = time.time()
            
            # Tự động phát hiện device
            self.device = self._detect_device()
            
            # Tải tokenizer
            self.tokenizer = AutoTokenizer.from_pretrained(
                self.model_path, 
                use_fast=True, 
                trust_remote_code=True
            )
            logger.info("[Judge] Tokenizer da tai thanh cong")
            
            # Cấu hình tải model dựa trên device
            if self.device == DEVICE_CUDA:
                self._load_cuda_model()
                logger.info(f"[Judge] Toi uu hoa: Du kien {PERF_GPU}/answer tren GPU")
                
            elif self.device == DEVICE_MPS:
                self._load_mps_model()
                logger.info(f"[Judge] Toi uu hoa: Du kien {PERF_MPS}/answer tren MPS")
                
            else:
                self._load_cpu_model()
                logger.info(f"[Judge] Toi uu hoa: Du kien {PERF_CPU}/answer tren CPU")
            
            load_time = time.time() - start_time
            logger.info(f"[Judge] Model da tai xong trong {load_time:.2f}s")
            logger.info(f"[Judge] Thiet bi dang su dung: {self.device}")
            
            # Hiển thị thông tin bộ nhớ nếu là GPU
            if self.device == DEVICE_CUDA:
                self._log_gpu_memory()
            
        except Exception as e:
            logger.error(f"[Judge] Loi khi tai model: {e}")
            raise
    
    def cleanup(self):
        """Dọn dẹp tài nguyên model"""
        logger.info("[Judge] Dang tat Judge Service...")
        if self.model:
            del self.model
        if self.tokenizer:
            del self.tokenizer
        
        # Dọn dẹp cache GPU nếu có
        if self.device == DEVICE_CUDA and torch.cuda.is_available():
            torch.cuda.empty_cache()
            logger.info("[Judge] Da don dep VRAM cache")
        elif self.device == DEVICE_MPS and hasattr(torch.backends, 'mps'):
            if hasattr(torch.mps, 'empty_cache'):
                torch.mps.empty_cache()
                logger.info("[Judge] Da don dep MPS cache")
    
    def is_loaded(self) -> bool:
        """Kiểm tra xem model đã được tải chưa"""
        return self.model is not None and self.tokenizer is not None
    
    def get_model(self):
        """Lấy model đã tải"""
        if not self.is_loaded():
            raise RuntimeError("Judge model chua duoc tai")
        return self.model
    
    def get_tokenizer(self):
        """Lấy tokenizer đã tải"""
        if not self.is_loaded():
            raise RuntimeError("Judge tokenizer chua duoc tai")
        return self.tokenizer
    
    def get_device(self):
        """Lấy thông tin device đang sử dụng"""
        return self.device if self.device else "unknown"


class CustomJudgeTransformer(nn.Module):
    """Custom Transformer model cho Judge (trained from scratch)"""
    def __init__(self, vocab_size=6209, d_model=512, nhead=8, num_layers=10, dropout=0.1):
        super().__init__()
        self.d_model = d_model
        self.embedding = nn.Embedding(vocab_size, d_model)
        self.pos_encoder = nn.Parameter(torch.zeros(1, 2000, d_model))
        
        self.transformer = nn.Transformer(
            d_model=d_model, nhead=nhead,
            num_encoder_layers=num_layers, num_decoder_layers=num_layers,
            dim_feedforward=d_model*4, dropout=dropout,
            batch_first=True, norm_first=True
        )
        self.fc_out = nn.Linear(d_model, vocab_size)
        
    def forward(self, src, tgt):
        src_mask = (src == 0)
        tgt_mask = (tgt == 0)
        tgt_causal_mask = self.transformer.generate_square_subsequent_mask(tgt.size(1)).to(src.device)
        
        src_emb = self.embedding(src) * math.sqrt(self.d_model) + self.pos_encoder[:, :src.size(1), :]
        tgt_emb = self.embedding(tgt) * math.sqrt(self.d_model) + self.pos_encoder[:, :tgt.size(1), :]
        
        out = self.transformer(src_emb, tgt_emb, tgt_mask=tgt_causal_mask,
                               src_key_padding_mask=src_mask, tgt_key_padding_mask=tgt_mask)
        return self.fc_out(out)


class CustomJudgeModelManager:
    """Quản lý custom Judge model (trained from scratch)"""
    
    def __init__(self):
        self.model = None
        self.tokenizer = None
        self.model_path = CUSTOM_JUDGE_MODEL_PATH
        self.device = None
        
    def _detect_device(self):
        """Tự động phát hiện thiết bị tốt nhất"""
        if torch.cuda.is_available():
            gpu_name = torch.cuda.get_device_name(0)
            gpu_memory = torch.cuda.get_device_properties(0).total_memory / (1024**3)
            logger.info(f"[CustomJudge] Phat hien GPU: {gpu_name}")
            logger.info(f"[CustomJudge] VRAM: {gpu_memory:.2f} GB")
            return DEVICE_CUDA
        elif hasattr(torch.backends, 'mps') and torch.backends.mps.is_available():
            logger.info("[CustomJudge] Phat hien Apple Silicon GPU (MPS)")
            return DEVICE_MPS
        else:
            logger.info("[CustomJudge] Khong phat hien GPU, su dung CPU")
            return DEVICE_CPU
    
    def load(self):
        """Tải custom Judge model và SentencePiece tokenizer"""
        logger.info("Khoi dong Custom Judge Service...")
        logger.info(f"[CustomJudge] Dang tai model tu: {self.model_path}")
        
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
            logger.info(f"[CustomJudge] SentencePiece tokenizer da tai (vocab_size={self.tokenizer.vocab_size()})")
            
            # Initialize model architecture
            self.model = CustomJudgeTransformer(
                vocab_size=self.tokenizer.vocab_size(),
                d_model=512,
                nhead=8,
                num_layers=10,
                dropout=0.1
            )
            
            # Load weights
            weights_path = self.model_path / "judge_model_weights.pth"
            if not weights_path.exists():
                raise FileNotFoundError(f"Model weights not found: {weights_path}")
            
            state_dict = torch.load(weights_path, map_location=self.device)
            # Remove 'module.' prefix if exists (from DataParallel)
            state_dict = {k.replace("module.", ""): v for k, v in state_dict.items()}
            self.model.load_state_dict(state_dict)
            
            # Move to device and set to eval mode
            self.model = self.model.to(self.device)
            self.model.eval()
            
            load_time = time.time() - start_time
            logger.info(f"[CustomJudge] Model da tai xong trong {load_time:.2f}s")
            logger.info(f"[CustomJudge] Thiet bi dang su dung: {self.device}")
            
            # Log GPU memory if applicable
            if self.device == DEVICE_CUDA:
                allocated = torch.cuda.memory_allocated(0) / (1024**3)
                reserved = torch.cuda.memory_reserved(0) / (1024**3)
                logger.info(f"[CustomJudge] VRAM su dung: {allocated:.2f} GB / Dat truoc: {reserved:.2f} GB")
            
        except Exception as e:
            logger.error(f"[CustomJudge] Loi khi tai model: {e}")
            raise
    
    def cleanup(self):
        """Dọn dẹp tài nguyên"""
        logger.info("[CustomJudge] Dang tat Custom Judge Service...")
        if self.model:
            del self.model
        if self.tokenizer:
            del self.tokenizer
        
        if self.device == DEVICE_CUDA and torch.cuda.is_available():
            torch.cuda.empty_cache()
            logger.info("[CustomJudge] Da don dep VRAM cache")
        elif self.device == DEVICE_MPS and hasattr(torch.backends, 'mps'):
            if hasattr(torch.mps, 'empty_cache'):
                torch.mps.empty_cache()
                logger.info("[CustomJudge] Da don dep MPS cache")
    
    def is_loaded(self) -> bool:
        """Kiểm tra xem model đã được tải chưa"""
        return self.model is not None and self.tokenizer is not None
    
    def get_model(self):
        """Lấy model đã tải"""
        if not self.is_loaded():
            raise RuntimeError("Custom Judge model chua duoc tai")
        return self.model
    
    def get_tokenizer(self):
        """Lấy SentencePiece tokenizer đã tải"""
        if not self.is_loaded():
            raise RuntimeError("Custom Judge tokenizer chua duoc tai")
        return self.tokenizer
    
    def get_device(self):
        """Lấy thông tin device đang sử dụng"""
        return self.device if self.device else "unknown"


# Instance global của model managers
model_manager = ModelManager()
judge_model_manager = JudgeModelManager()
custom_judge_manager = CustomJudgeModelManager()  # NEW: Custom Judge model
