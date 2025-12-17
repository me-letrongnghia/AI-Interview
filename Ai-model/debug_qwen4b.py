import sys
import os
from pathlib import Path
sys.path.insert(0, os.getcwd())

print("Testing Qwen4BModelManager...")
try:
    from src.services.model_loader import Qwen4BModelManager
    print("Import successful")
    manager = Qwen4BModelManager()
    print("Instantiation successful")
    print(f"Max length: {manager.max_seq_length}")
    
    # Check if get_model_info works (even if not loaded)
    # Note: get_model_info accesses self.tokenizer which is None initially
    # Code handles this: "vocab_size": len(self.tokenizer) if self.tokenizer else 151936
    info = manager.get_model_info()
    print(f"Info keys: {list(info.keys())}")
    print("Test PASSED")
except Exception as e:
    print(f"TEST FAILED: {e}")
    import traceback
    traceback.print_exc()
