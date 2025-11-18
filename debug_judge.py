"""
Debug script to test Judge model inference directly
"""
import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).parent / "Ai-model"))

import torch
from transformers import AutoTokenizer, AutoModelForCausalLM

print("=" * 60)
print("Judge Model Debug Test")
print("=" * 60)

# Check CUDA
print(f"\n1. CUDA Check:")
print(f"   CUDA available: {torch.cuda.is_available()}")
if torch.cuda.is_available():
    print(f"   GPU: {torch.cuda.get_device_name(0)}")
    print(f"   VRAM: {torch.cuda.get_device_properties(0).total_memory / 1024**3:.2f} GB")

# Load model
print(f"\n2. Loading Judge Model...")
model_path = "Ai-model/model/Judge_merge"

try:
    tokenizer = AutoTokenizer.from_pretrained(model_path, trust_remote_code=True)
    print(f"   ✓ Tokenizer loaded")
    
    # Try GPU first
    if torch.cuda.is_available():
        print(f"   Loading to GPU...")
        model = AutoModelForCausalLM.from_pretrained(
            model_path,
            torch_dtype=torch.float16,
            device_map="auto",
            trust_remote_code=True
        ).eval()
        print(f"   ✓ Model loaded to: {model.device}")
    else:
        print(f"   Loading to CPU...")
        model = AutoModelForCausalLM.from_pretrained(
            model_path,
            torch_dtype=torch.float32,
            trust_remote_code=True
        ).eval()
        print(f"   ✓ Model loaded to CPU")
    
    # Test inference
    print(f"\n3. Testing Inference...")
    
    prompt = """<|system|>
Evaluate technical interview answer. Score 0.0-1.0 on: Correctness, Coverage, Depth, Clarity, Practicality.

Output JSON:
{"scores": {"correctness": 0.75, "coverage": 0.70, "depth": 0.65, "clarity": 0.80, "practicality": 0.60, "final": 0.71}, "feedback": ["Point 1", "Point 2", "Point 3"], "improved_answer": "Better version..."}
<|user|>
Context: Position: Java Backend Developer | Level: Mid-level

Question: Explain dependency injection in Spring Boot

Candidate's Answer: DI is a design pattern where Spring automatically injects dependencies using @Autowired. It promotes loose coupling and easier testing.

Provide evaluation in JSON format.
<|assistant|>
"""
    
    print(f"   Input prompt length: {len(prompt)} chars")
    
    inputs = tokenizer(prompt, return_tensors="pt")
    if torch.cuda.is_available():
        inputs = inputs.to("cuda")
    
    print(f"   Input tokens: {inputs['input_ids'].shape[1]}")
    print(f"   Generating (max 512 tokens)...")
    
    import time
    start = time.time()
    
    with torch.inference_mode():
        outputs = model.generate(
            **inputs,
            max_new_tokens=512,
            do_sample=True,
            temperature=0.3,
            top_p=0.95,
            repetition_penalty=1.1,
            pad_token_id=tokenizer.eos_token_id,
            eos_token_id=tokenizer.eos_token_id
        )
    
    gen_time = time.time() - start
    
    response = tokenizer.decode(
        outputs[0][inputs["input_ids"].shape[1]:],
        skip_special_tokens=True
    )
    
    print(f"\n4. Results:")
    print(f"   Generation time: {gen_time:.2f}s")
    print(f"   Output tokens: {outputs[0].shape[0] - inputs['input_ids'].shape[1]}")
    print(f"   Response length: {len(response)} chars")
    print(f"\n   Response preview:")
    print(f"   {response[:500]}")
    
    # Try JSON parsing
    print(f"\n5. JSON Parsing:")
    import json
    import re
    
    # Try direct parse
    try:
        result = json.loads(response)
        print(f"   ✓ Direct JSON parse SUCCESS")
        print(f"   Scores: {result.get('scores', {})}")
    except:
        print(f"   ✗ Direct JSON parse failed, trying regex...")
        
        # Try regex
        match = re.search(r'\{[\s\S]*"scores"[\s\S]*\}', response)
        if match:
            try:
                result = json.loads(match.group(0))
                print(f"   ✓ Regex JSON parse SUCCESS")
                print(f"   Scores: {result.get('scores', {})}")
            except:
                print(f"   ✗ Regex JSON parse also failed")
                print(f"   Full response:\n{response}")
        else:
            print(f"   ✗ No JSON pattern found in response")
            print(f"   Full response:\n{response}")

except Exception as e:
    print(f"\n✗ ERROR: {e}")
    import traceback
    traceback.print_exc()

print("\n" + "=" * 60)
