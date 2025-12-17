"""Interface cho tất cả AI models"""

from abc import ABC, abstractmethod
from typing import Dict, Any, Optional
import logging

logger = logging.getLogger(__name__)


class BaseModelManager(ABC):
    """Base class cho tất cả model managers"""
    
    @abstractmethod
    def load(self) -> None:
        """Load model vào memory"""
        pass
    
    @abstractmethod
    def cleanup(self) -> None:
        """Giải phóng resources"""
        pass
    
    @abstractmethod
    def is_loaded(self) -> bool:
        """Check model đã load chưa"""
        pass
    
    @abstractmethod
    def generate(
        self, 
        input_text: str, 
        max_new_tokens: int = 256,
        temperature: float = 0.7,
        use_greedy: bool = False
    ) -> str:
        """Sinh text từ input"""
        pass
    
    @abstractmethod
    def get_device(self) -> str:
        """Trả về device đang dùng (cuda/mps/cpu)"""
        pass
    
    @abstractmethod
    def get_model_info(self) -> Dict[str, Any]:
        """Trả về thông tin model"""
        pass
    
    def encode(self, text: str, max_length: int = 512) -> Any:
        """Encode text thành tokens (optional)"""
        raise NotImplementedError(f"{self.__class__.__name__} does not implement encode()")
    
    def decode(self, tokens: Any) -> str:
        """Decode tokens thành text (optional)"""
        raise NotImplementedError(f"{self.__class__.__name__} does not implement decode()")
    
    def __repr__(self) -> str:
        info = self.get_model_info() if self.is_loaded() else {"model_name": "Unknown"}
        return f"{self.__class__.__name__}(model={info.get('model_name')}, loaded={self.is_loaded()})"
