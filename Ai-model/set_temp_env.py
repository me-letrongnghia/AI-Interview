"""
Set environment variables to redirect ALL temp/cache to D: drive
MUST be imported BEFORE any other imports to take effect
"""
import os
from pathlib import Path

# Get project root (D:\Code\NCKH\AI-Interview\Ai-model)
PROJECT_ROOT = Path(__file__).parent.absolute()

# Create temp directories in project (ổ D:)
TEMP_DIR = PROJECT_ROOT / "temp"
CACHE_DIR = PROJECT_ROOT / "cache"
HF_CACHE_DIR = PROJECT_ROOT / "hf_cache"

# Create directories
TEMP_DIR.mkdir(exist_ok=True)
CACHE_DIR.mkdir(exist_ok=True)
HF_CACHE_DIR.mkdir(exist_ok=True)

# Set Windows TEMP/TMP to D: drive
os.environ["TEMP"] = str(TEMP_DIR)
os.environ["TMP"] = str(TEMP_DIR)
os.environ["TMPDIR"] = str(TEMP_DIR)  # Unix-style

# Set HuggingFace cache to D: drive
os.environ["HF_HOME"] = str(HF_CACHE_DIR)
os.environ["HUGGINGFACE_HUB_CACHE"] = str(HF_CACHE_DIR)
os.environ["TRANSFORMERS_CACHE"] = str(HF_CACHE_DIR)

# Set PyTorch cache to D: drive
os.environ["TORCH_HOME"] = str(CACHE_DIR / "torch")

# Set NumPy/SciPy temp to D: drive
os.environ["NUMPY_TMPDIR"] = str(TEMP_DIR)
os.environ["SCIPY_TMPDIR"] = str(TEMP_DIR)

# Disable Windows Store Python redirection (optional)
os.environ["PYTHONNOUSERSITE"] = "1"

print("=" * 80)
print("🔧 ENVIRONMENT VARIABLES SET:")
print(f"   TEMP/TMP:              {TEMP_DIR}")
print(f"   HF_HOME:               {HF_CACHE_DIR}")
print(f"   TRANSFORMERS_CACHE:    {HF_CACHE_DIR}")
print(f"   TORCH_HOME:            {CACHE_DIR / 'torch'}")
print("=" * 80)
print(f"✅ All temp/cache files will be created in ổ D: (not C:)")
print("=" * 80)

