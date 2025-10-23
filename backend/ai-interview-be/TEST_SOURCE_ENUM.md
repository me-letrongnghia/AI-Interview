# Test Case: Source Enum Case-Insensitive Deserialization

## Problem
Frontend gửi `source: "cv"` (lowercase) nhưng backend enum chỉ chấp nhận `Custom`, `JD`, `CV` (PascalCase).

## Solution
Thêm `@JsonCreator` và `@JsonValue` vào enum `Source` để support case-insensitive deserialization.

## Test Cases

### ✅ Valid Inputs (All should work)

```json
// Lowercase
{"source": "custom"} → Source.Custom
{"source": "jd"}     → Source.JD
{"source": "cv"}     → Source.CV

// Uppercase
{"source": "CUSTOM"} → Source.Custom
{"source": "JD"}     → Source.JD
{"source": "CV"}     → Source.CV

// PascalCase (Original)
{"source": "Custom"} → Source.Custom
{"source": "JD"}     → Source.JD
{"source": "CV"}     → Source.CV

// Mixed case
{"source": "CuStOm"} → Source.Custom
{"source": "Jd"}     → Source.JD
{"source": "Cv"}     → Source.CV

// Null or missing
{"source": null}     → Source.Custom (default)
{}                   → Source.Custom (default)
```

### ✅ Invalid Inputs (Should default to Custom)

```json
{"source": "invalid"} → Source.Custom
{"source": ""}        → Source.Custom
{"source": "123"}     → Source.Custom
```

## Example Request

```json
POST /api/interviews/sessions
{
  "userId": 1,
  "role": "Java Developer",
  "level": "Mid-level",
  "skill": ["Spring Boot", "Java"],
  "language": "English",
  "source": "cv"  ← lowercase now works!
}
```

## Response

```json
{
  "sessionId": 123
}
```

## Verification

```bash
# Test with curl
curl -X POST http://localhost:8080/api/interviews/sessions \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "role": "Java Developer",
    "level": "Mid-level",
    "skill": ["Spring Boot"],
    "language": "English",
    "source": "cv"
  }'
```

Expected: HTTP 201 Created ✅
Before: HTTP 500 JSON parse error ❌

