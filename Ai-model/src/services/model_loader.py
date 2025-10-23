import time
import logging
import torch
from transformers import AutoModelForCausalLM, AutoTokenizer

from src.core.config import MODEL_PATH

logger = logging.getLogger(__name__)


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
            device = "cuda"
            gpu_name = torch.cuda.get_device_name(0)
            gpu_memory = torch.cuda.get_device_properties(0).total_memory / (1024**3)
            logger.info(f"Phat hien GPU: {gpu_name}")
            logger.info(f"VRAM: {gpu_memory:.2f} GB")
        elif hasattr(torch.backends, 'mps') and torch.backends.mps.is_available():
            device = "mps"  # Apple Silicon GPU
            logger.info("Phat hien Apple Silicon GPU (MPS)")
        else:
            device = "cpu"
            logger.info("Khong phat hien GPU, su dung CPU")
        
        return device
        
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
            if self.device == "cuda":
                # GPU: Sử dụng float16 để tiết kiệm VRAM
                logger.info("Dang tai model cho GPU (float16 de tiet kiem VRAM)")
                self.model = AutoModelForCausalLM.from_pretrained(
                    self.model_path,
                    torch_dtype=torch.float16,
                    device_map="auto",  # Tự động phân bổ lên GPU
                    trust_remote_code=True
                ).eval()
                logger.info("Toi uu hoa: Du kien 2-4s/cau hoi tren GPU")
                
            elif self.device == "mps":
                # Apple Silicon: Sử dụng float32 (MPS không hỗ trợ float16 tốt)
                logger.info("Dang tai model cho Apple Silicon GPU")
                self.model = AutoModelForCausalLM.from_pretrained(
                    self.model_path,
                    torch_dtype=torch.float32,
                    trust_remote_code=True
                ).to(self.device).eval()
                logger.info("Toi uu hoa: Du kien 5-8s/cau hoi tren MPS")
                
            else:
                # CPU: Sử dụng float32 với tối ưu hóa bộ nhớ
                logger.info("Dang tai model cho CPU (toi uu bo nho)")
                logger.info("Toi uu hoa: max_tokens=48, temp=0.75, num_beams=1")
                self.model = AutoModelForCausalLM.from_pretrained(
                    self.model_path,
                    torch_dtype=torch.float32,
                    low_cpu_mem_usage=True,
                    trust_remote_code=True
                ).eval()
                logger.info("Toi uu hoa: Du kien 12-15s/cau hoi tren CPU (cai thien tu 32s)")
            
            load_time = time.time() - start_time
            logger.info(f"Model da tai xong trong {load_time:.2f}s")
            logger.info(f"Thiet bi dang su dung: {self.device}")
            
            # Hiển thị thông tin bộ nhớ nếu là GPU
            if self.device == "cuda":
                allocated = torch.cuda.memory_allocated(0) / (1024**3)
                reserved = torch.cuda.memory_reserved(0) / (1024**3)
                logger.info(f"VRAM su dung: {allocated:.2f} GB / Dat truoc: {reserved:.2f} GB")
            
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
        if self.device == "cuda" and torch.cuda.is_available():
            torch.cuda.empty_cache()
            logger.info("Da don dep VRAM cache")
        elif self.device == "mps" and hasattr(torch.backends, 'mps'):
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


# Instance global của model manager
model_manager = ModelManager()
