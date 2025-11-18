# Fix "Paging File Too Small" Error

## Tăng Virtual Memory trên Windows

### Bước 1: Mở System Properties
1. Nhấn `Win + R`
2. Gõ: `sysdm.cpl`
3. Enter

### Bước 2: Advanced Settings
1. Tab **Advanced**
2. Section **Performance** → Click **Settings**
3. Tab **Advanced** (trong cửa sổ mới)
4. Section **Virtual memory** → Click **Change**

### Bước 3: Tăng Paging File Size
1. ✅ Bỏ tick **"Automatically manage paging file size for all drives"**
2. Chọn ổ C: (hoặc ổ có nhiều dung lượng)
3. Chọn **Custom size**
4. Nhập:
   - **Initial size**: `12288` MB (12GB)
   - **Maximum size**: `16384` MB (16GB)
5. Click **Set**
6. Click **OK** tất cả cửa sổ
7. **Restart máy**

### Bước 4: Restart Service
```powershell
py .\main.py
```

---

## Giải pháp thay thế: Chạy Models riêng biệt

### Option 1: Chỉ chạy GenQ Service (Generate Questions)
Comment dòng load Judge trong `src/api/app.py`:
```python
# judge_model_manager.load()  # Disable Judge model
```

### Option 2: Chạy 2 Services riêng biệt
1. **Service 1 - GenQ** (Port 8000): Generate questions
2. **Service 2 - Judge** (Port 8001): Evaluate answers

Tạo file `main_judge_only.py`:
```python
from src.api.app import create_app
from src.services.model_loader import judge_model_manager
import uvicorn

app = create_app()

@app.on_event("startup")
async def startup():
    judge_model_manager.load()

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8001)
```

---

## RAM Requirements

- **GenQ Model**: ~3GB
- **Judge Model**: ~3GB
- **Total**: ~6-8GB
- **Recommended**: 16GB RAM + 16GB Virtual Memory

---

## Quick Check RAM Usage

```powershell
# Check available RAM
Get-CimInstance Win32_OperatingSystem | Select-Object FreePhysicalMemory,TotalVisibleMemorySize
```
