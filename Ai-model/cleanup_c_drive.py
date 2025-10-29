"""
Cleanup script to remove temp/cache files from C: drive
Run this BEFORE starting the model to free up space
"""
import os
import shutil
from pathlib import Path

def get_size_mb(path):
    """Get directory size in MB"""
    total = 0
    try:
        for entry in os.scandir(path):
            if entry.is_file():
                total += entry.stat().st_size
            elif entry.is_dir():
                total += get_size_mb(entry.path)
    except (PermissionError, FileNotFoundError):
        pass
    return total / (1024 * 1024)

def cleanup_directory(path, description):
    """Remove directory and show size freed"""
    if not path.exists():
        print(f"   ⏭️  {description}: Not found")
        return 0
    
    try:
        size = get_size_mb(path)
        shutil.rmtree(path, ignore_errors=True)
        print(f"   ✅ {description}: Freed {size:.1f} MB")
        return size
    except Exception as e:
        print(f"   ❌ {description}: Error - {e}")
        return 0

def main():
    print("=" * 80)
    print("🧹 CLEANUP C: DRIVE - Remove Python temp/cache files")
    print("=" * 80)
    
    # Common locations for Python cache on Windows
    user_home = Path.home()
    temp_dir = Path(os.environ.get('TEMP', user_home / 'AppData' / 'Local' / 'Temp'))
    
    total_freed = 0
    
    print("\n📂 Cleaning Python temp directories:")
    
    # 1. Windows TEMP folder - offload files
    offload_dirs = list(temp_dir.glob('offload*'))
    for offload_dir in offload_dirs:
        total_freed += cleanup_directory(offload_dir, f"TEMP/{offload_dir.name}")
    
    # 2. HuggingFace cache
    hf_cache = user_home / '.cache' / 'huggingface'
    if hf_cache.exists():
        print(f"\n⚠️  HuggingFace cache found: {hf_cache}")
        print(f"   Size: {get_size_mb(hf_cache):.1f} MB")
        response = input("   Remove? (y/N): ")
        if response.lower() == 'y':
            total_freed += cleanup_directory(hf_cache, "HuggingFace cache")
    
    # 3. Torch cache
    torch_cache = user_home / '.cache' / 'torch'
    if torch_cache.exists():
        print(f"\n⚠️  PyTorch cache found: {torch_cache}")
        print(f"   Size: {get_size_mb(torch_cache):.1f} MB")
        response = input("   Remove? (y/N): ")
        if response.lower() == 'y':
            total_freed += cleanup_directory(torch_cache, "PyTorch cache")
    
    # 4. Python pip cache
    pip_cache = user_home / 'AppData' / 'Local' / 'pip' / 'cache'
    if pip_cache.exists():
        size = get_size_mb(pip_cache)
        if size > 100:  # Only suggest if > 100MB
            print(f"\n⚠️  Pip cache found: {pip_cache}")
            print(f"   Size: {size:.1f} MB")
            response = input("   Remove? (y/N): ")
            if response.lower() == 'y':
                total_freed += cleanup_directory(pip_cache, "Pip cache")
    
    # 5. Python __pycache__
    print("\n📂 Cleaning __pycache__ directories...")
    project_root = Path(__file__).parent
    pycache_dirs = list(project_root.rglob('__pycache__'))
    for pycache_dir in pycache_dirs:
        total_freed += cleanup_directory(pycache_dir, f"__pycache__/{pycache_dir.parent.name}")
    
    print("\n" + "=" * 80)
    print(f"✅ CLEANUP COMPLETE!")
    print(f"   Total space freed: {total_freed:.1f} MB")
    print("=" * 80)
    
    # Show current disk space
    print("\n💾 Current disk space:")
    try:
        import psutil
        for partition in psutil.disk_partitions():
            if 'C:' in partition.device or 'D:' in partition.device:
                usage = psutil.disk_usage(partition.mountpoint)
                free_gb = usage.free / (1024**3)
                total_gb = usage.total / (1024**3)
                percent = usage.percent
                print(f"   {partition.device:4} {free_gb:6.1f} GB free / {total_gb:6.1f} GB total ({percent:.1f}% used)")
    except ImportError:
        print("   (Install psutil to see disk usage: pip install psutil)")
    
    print("\n🚀 Now you can run: python main.py")
    print("   All temp/cache will be created in D: drive")

if __name__ == "__main__":
    main()

