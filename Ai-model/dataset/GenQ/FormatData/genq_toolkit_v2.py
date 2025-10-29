#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import json, re, sys, argparse, random, math, hashlib
from collections import Counter, defaultdict

# ---------- Helpers shared with v1 ----------
TONES = ["friendly","neutral","probing","formal","challenging"]
LEVELS = ["Intern","Junior","Mid","Senior","Lead"]
CATEGORIES = ["Technical","Behavioral","Project","Situational","Motivational"]

TOPIC_HINTS = {
  "database": "Database Optimization",
  "index": "Database Optimization",
  "cache": "Caching",
  "latency": "Performance & Reliability",
  "monitor": "Observability",
  "log": "Observability",
  "trace": "Observability",
  "oauth": "Security & Auth",
  "jwt": "Security & Auth",
  "kafka": "Messaging & Streaming",
  "rabbitmq": "Messaging & Streaming",
  "react": "Frontend Architecture",
  "accessibility": "Frontend Accessibility",
  "spark": "Distributed Data",
  "airflow": "Orchestration",
  "kubernetes": "Platform & Orchestration",
  "microservice": "System Design"
}

def normalize_space(s): return re.sub(r"\s+"," ", s or "").strip()
def strip_punct(s): return re.sub(r"[\s\.,;:!?\-–—\"'`]+"," ", s.lower()).strip()

def stable_hash(text):
    return hashlib.sha1(strip_punct(text).encode("utf-8")).hexdigest()[:16]

def infer_difficulty(q, level):
    L = level or ""
    qlen = len(q.split())
    if L in ("Intern","Junior"): 
        return "easy" if qlen < 20 else "medium"
    if L == "Mid":
        return "medium" if qlen < 28 else "hard"
    if L in ("Senior","Lead"):
        return "hard" if qlen < 32 else "expert"
    return "medium"

def infer_topic(q):
    s = q.lower()
    for k,v in TOPIC_HINTS.items():
        if k in s: return v
    return "General"

def infer_framework(meta, q):
    stack = meta.get("tech_stack") or []
    low = q.lower()
    for t in stack:
        if t.lower() in low: return t
    return stack[0] if stack else ""

def infer_qtype(q):
    s = q.lower()
    if any(w in s for w in ["debug","incident","outage","failure","broken","error"]):
        return "debugging"
    if any(w in s for w in ["design","architect","plan","diagram","trade-off","tradeoff"]):
        return "practical"
    if any(w in s for w in ["explain","what is","define","difference between","walk me through"]):
        return "conceptual"
    return "open-ended"

def infer_goal(q, meta):
    cat = meta.get("category","")
    if cat in ("Behavioral","Motivational"):
        return "experience-based"
    if cat == "Technical":
        return "reasoning"
    return "knowledge-check"

def quality_heuristic(q):
    txt = q.strip()
    words = len(txt.split())
    endq = txt.endswith("?")
    if words < 8 or words > 60 or not endq:
        return 0.0
    # lightweight heuristic
    return min(1.0, 0.4 + 0.01*max(0, 60-abs(30-words)))  # peak near ~30 words

def jaccard(a, b):
    A = set(strip_punct(a).split())
    B = set(strip_punct(b).split())
    if not A or not B: return 0.0
    return len(A & B) / len(A | B)

# ---------- Steps ----------
def step_normalize(items):
    out = []
    for it in items:
        meta = it.get("meta", {})
        q = (it.get("output") or {}).get("question","")
        # infer fields
        meta.setdefault("difficulty", infer_difficulty(q, meta.get("level","")))
        meta.setdefault("topic", infer_topic(q))
        meta.setdefault("framework", infer_framework(meta, q))
        meta.setdefault("type", infer_qtype(q))
        meta.setdefault("goal", infer_goal(q, meta))
        # ensure tone allowed
        if meta.get("tone") not in TONES:
            meta["tone"] = "neutral"
        it["meta"] = meta
        # aux
        aux = it.get("aux", {})
        aux["hash"] = stable_hash(q)
        aux.setdefault("quality", quality_heuristic(q))  # keep previous if exists
        it["aux"] = aux
        out.append(it)
    return out

def step_filter_basic(items, min_quality=0.7, dedup_jaccard=0.9):
    # same as v1 but separated
    tmp, seen = [], set()
    for it in items:
        q = (it.get("output",{}) or {}).get("question","").strip()
        if not q or len(q.split()) < 8 or len(q) > 500: 
            continue
        if not q.endswith("?"): 
            continue
        qual = (it.get("aux",{}).get("quality", 0.0))
        if qual < min_quality:
            continue
        h = it.get("aux",{}).get("hash") or stable_hash(q)
        if h in seen: 
            continue
        seen.add(h)
        tmp.append(it)
    out = []
    for it in tmp:
        q = it["output"]["question"]
        dup = False
        for jt in out[-200:]:
            if jaccard(q, jt["output"]["question"]) >= dedup_jaccard:
                dup = True; break
        if not dup: out.append(it)
    return out

def step_ingest_scores(items, score_file):
    # score_file is JSONL lines: {"hash":"...","score": float, "explanation":"..."}
    scores = {}
    with open(score_file, "r", encoding="utf-8") as f:
        for line in f:
            line=line.strip()
            if not line: continue
            try:
                row = json.loads(line)
            except:
                continue
            h = row.get("hash"); s=row.get("score")
            if isinstance(h, str) and isinstance(s, (int,float)):
                scores[h]=float(s)
    out = []
    for it in items:
        q = (it.get("output") or {}).get("question","")
        h = it.get("aux",{}).get("hash") or stable_hash(q)
        aux = it.setdefault("aux", {})
        if h in scores:
            aux["judge_score"] = max(0.0, min(1.0, scores[h]))
        out.append(it)
    return out

def step_filter_top_percent(items, percent=0.8, prefer_field="judge_score", fallback_field="quality"):
    # keep top X% by judge_score if present, else by quality heuristic
    scored = []
    for it in items:
        aux = it.get("aux", {})
        s = aux.get(prefer_field, None)
        if s is None:
            s = aux.get(fallback_field, 0.0)
        scored.append((s, it))
    scored.sort(key=lambda x: x[0], reverse=True)
    k = int(len(scored) * percent)
    k = max(1, k)
    return [it for _, it in scored[:k]]

def step_balance_by(items, dims=("role","category","level"), per_bucket=300, min_per_bucket=0):
    # dims example: ("role","category","level")
    buckets = defaultdict(list)
    for it in items:
        meta = it.get("meta",{})
        key = tuple(meta.get(d,"") for d in dims)
        buckets[key].append(it)
    out = []
    for key, arr in buckets.items():
        random.shuffle(arr)
        take = per_bucket if per_bucket>0 else len(arr)
        # if bucket too small and min_per_bucket>0, keep all (or could upsample optionally)
        if len(arr) < min_per_bucket and min_per_bucket>0:
            out.extend(arr)
        else:
            out.extend(arr[:take])
    return out

# ---------- I/O ----------
def read_jsonl(path):
    with open(path, "r", encoding="utf-8") as f:
        for line in f:
            line=line.strip()
            if not line: continue
            try:
                yield json.loads(line)
            except:
                continue

def write_jsonl(path, items):
    with open(path, "w", encoding="utf-8") as f:
        for it in items:
            f.write(json.dumps(it, ensure_ascii=False) + "\n")

def show_stats(items, dims=("category","level","role")):
    c = Counter()
    n = 0
    for it in items:
        n += 1
        meta = it.get("meta",{})
        for d in dims:
            c[(d, meta.get(d,""))] += 1
    print(f"Count: {n}")
    for (k,v), cnt in c.most_common(100):
        print(f"{k:>10} | {v:<25} : {cnt}")
    return n, c

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--in", dest="inp", required=True)
    ap.add_argument("--out", dest="out", required=True)
    ap.add_argument("--steps", default="normalize,filter_basic,ingest_scores,filter_top,balance")
    ap.add_argument("--min-quality", type=float, default=0.7)
    ap.add_argument("--dedup-jaccard", type=float, default=0.9)  # not used here; kept for compat
    ap.add_argument("--score-file", type=str, default=None, help="JSONL with {hash,score}")
    ap.add_argument("--top-percent", type=float, default=0.8)
    ap.add_argument("--balance-dims", type=str, default="role,category,level")
    ap.add_argument("--per-bucket", type=int, default=300)
    ap.add_argument("--min-per-bucket", type=int, default=0)
    args = ap.parse_args()

    items = list(read_jsonl(args.inp))
    print("Loaded:", len(items))

    steps = [s.strip() for s in args.steps.split(",") if s.strip()]
    for s in steps:
        if s == "normalize":
            items = step_normalize(items)
        elif s == "filter_basic":
            items = step_filter_basic(items, args.min_quality, args.dedup_jaccard)
        elif s == "ingest_scores":
            if not args.score_file:
                print("WARN: --score-file not provided, skipping ingest_scores")
            else:
                items = step_ingest_scores(items, args.score_file)
        elif s == "filter_top":
            items = step_filter_top_percent(items, args.top_percent)
        elif s == "balance":
            dims = tuple([x.strip() for x in args.balance_dims.split(",") if x.strip()])
            items = step_balance_by(items, dims=dims, per_bucket=args.per_bucket, min_per_bucket=args.min_per_bucket)
        else:
            print("Unknown step:", s)
        print(f"After {s}: {len(items)} items")

    show_stats(items, dims=("role","category","level"))
    write_jsonl(args.out, items)
    print("Saved ->", args.out)

if __name__ == "__main__":
    main()
