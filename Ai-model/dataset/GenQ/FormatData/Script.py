import json, collections
path = "GenQ_v6_top80_balanced.jsonl" 
cnt = collections.Counter()
with open(path, encoding="utf-8") as f:
    for line in f:
        if not line.strip(): continue
        try:
            obj = json.loads(line)
            t = obj.get("input", {}).get("type", "UNKNOWN")
            cnt[t] += 1
        except: pass
for k,v in cnt.items():
    print(f"{k:12} : {v}")
