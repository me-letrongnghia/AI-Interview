# genq_pipeline.py
# All-in-one: refine -> balance -> grammar polish -> enrich meta -> stats + HTML report
# + optional LLM-powered rewrite & follow-up generation (falls back gracefully if no API key)
#
# Usage examples:
#   py genq_pipeline.py --input GenQ.jsonl --out GenQ_v3.jsonl --followups 2000 \
#       --llm-provider openrouter --llm-model qwen2.5:7b-instruct
#
#   # Fallback (no LLM): still refines + naive followups
#   py genq_pipeline.py --input GenQ.jsonl --out GenQ_v3.jsonl --followups 2000
#
# Outputs:
#   - <out>                       : refined dataset
#   - <out>.followups.jsonl       : follow-ups (LLM if available, else naive)
#   - g2.lang.rejects.jsonl       : Gate2 rejects
#   - stats_v3_domain.json / ...  : stats
#   - report_v3.html              : HTML summary

import os, re, json, argparse, random, time
from collections import defaultdict, Counter

# ---------- Optional deps ----------
try:
    from rapidfuzz import fuzz
    HAS_RAPIDFUZZ = True
except Exception:
    HAS_RAPIDFUZZ = False

try:
    import language_tool_python
    HAS_LANG = True
except Exception:
    HAS_LANG = False

try:
    import requests
    HAS_REQ = True
except Exception:
    HAS_REQ = False

random.seed(42)

# ---------- Config (chỉnh tuỳ ý) ----------

# Domain niche cần cap (tránh bias)
NICHE_DOMAINS = {
    "Quantum Computing","Autonomous Drones","Energy Systems / SCADA","Smart Cities",
    "Neurotech/BCI","Metaverse","SpaceTech/Satellite","Aerospace/Avionics","Autonomous Ships",
    "Green IT & Sustainable Computing","Blockchain & Web3","Blockchain/Web3","AR/VR & Metaverse",
    "AR/VR","AR/VR Advanced","Digital Twins","Space Exploration","Wearables & Health IoT",
    "Bioinformatics AI","PharmaTech","Robotics","Cyber-Physical Systems","Smart Grid Security",
    "Human-Robot Interaction","Synthetic Data/Simulation AI","High-Performance C++","Nanotech",
    "Telecom/5G/Edge","5G/6G Research","Edge AI","Edge AI & IoT","Edge Networking/CDN",
    "Gaming Backend","Game Development","Autonomy"
}
NICHE_CAP = 120  # None = bỏ cap

# Cap category “thống trị”
CATEGORY_CAP_RATIO = {
    "Security": 0.25,
    "Performance": 0.22,
    "Architecture": 0.22
}

# Cân level (downsample)
TARGET_LEVEL_RATIO = {
    "intern": 0.20,
    "junior": 0.40,
    "senior": 0.35,
    "mid":    0.05
}

# Gate2 – Lọc ngôn ngữ
MIN_WORDS = 7
MAX_WORDS = 35
BANNED_PATTERNS = [
    r"^compare different approaches.*",
    r".*describe would you ensure.*",
    r".*plan flight in identity platform.*",
    r"^what approach would you take to deploy pipeline for performance\??$",
    r"^explain .* in identity platform\??$",
    r"^compare .* describe.*\?$",
    r".*\bthe the\b.*"
]

# Grammar polish
ENABLE_GRAMMAR = True
GRAMMAR_MAX = 2000
GRAMMAR_MIN_LEN = 8
GRAMMAR_MAX_LEN = 42

# Enrich meta
ENABLE_METADATA_ENRICH = True

# Follow-ups
FOLLOWUP_PREV_TPL = "In your recent {domain} work, what was the main challenge you faced and how did you approach it?"
FOLLOWUP_ANS = {
    "security":"We hardened authN/Z and secrets, enforcing least privilege and policy tests.",
    "performance":"We profiled hotspots, added caching, and set latency budgets with KPIs.",
    "scalability":"We introduced horizontal scaling, partitioning, and backpressure.",
    "reliability":"We added timeouts/retries/circuit breakers and DR playbooks with SLOs.",
    "data":"We redesigned schema, added CDC, and improved data contracts/tests.",
    "testing":"We expanded unit/integration/E2E, regression gates and hermetic envs.",
    "observability":"We instrumented tracing/metrics/logging with alerts and SLO dashboards.",
    "teamwork":"We aligned stakeholders, clarified ownership, and improved handoffs."
}
FOLLOWUP_DEFAULT_ANS = "We iterated on the design, validated trade-offs, and documented decisions."

# LLM prompts (rewrite & follow-up)
REWRITE_SYS = "You are a senior interviewer. Rewrite the question so it sounds like a natural, concise interview question. Keep the meaning."
REWRITE_USER_TPL = "Original: {q}\nConstraints: one question, 9-28 words, professional tone."

FOLLOWUP_SYS = (
 "You are a seasoned technical interviewer. Based on the previous Q and the candidate's answer, "
 "write ONE natural follow-up question that probes deeper. Keep it concise (<= 28 words)."
)
FOLLOWUP_USER_TPL = (
 "Previous question: {prev}\nCandidate answer (short gist): {ans}\n"
 "Now produce ONE follow-up question tied to this context. Avoid generic phrasing. Focus on {focus}."
)

# ---------- IO helpers ----------
def load_jsonl(path):
    items=[]
    with open(path,"r",encoding="utf-8") as f:
        for i,line in enumerate(f,1):
            s=line.strip()
            if not s: continue
            try:
                obj=json.loads(s); items.append(obj)
            except Exception as e:
                print(f"[WARN] JSON parse error at line {i}: {e}")
    return items

def save_jsonl(path, items):
    with open(path,"w",encoding="utf-8") as f:
        for o in items:
            f.write(json.dumps(o,ensure_ascii=False)+"\n")

# ---------- Core helpers ----------
def normalize_q(q:str)->str:
    q=q.strip().lower()
    q=re.sub(r"\s+"," ",q)
    q=re.sub(r"[?.,!;:]+$","",q)
    return q

def deduplicate(items):
    # exact
    seen=set(); uniq=[]
    for it in items:
        q=(it.get("question") or "").strip()
        key=normalize_q(q)
        if not key: continue
        if key in seen: continue
        seen.add(key); uniq.append(it)

    if not HAS_RAPIDFUZZ:
        return uniq,0

    # near-dup per domain
    buckets=defaultdict(list)
    for it in uniq:
        d=(it.get("meta",{}) or {}).get("domain","UNKNOWN")
        buckets[d].append(it)

    final=[]; removed=0
    for d,arr in buckets.items():
        kept=[]; texts=[]
        for it in arr:
            q=normalize_q(it["question"])
            dup=False
            for t in texts:
                if fuzz.token_set_ratio(q,t)>=90:
                    dup=True; break
            if not dup:
                kept.append(it); texts.append(q)
            else:
                removed+=1
        final.extend(kept)
    return final, removed

def filter_language(items):
    keep=[]; rej=[]
    for it in items:
        q=(it.get("question") or "").strip()
        wc=len(q.split())
        if wc<MIN_WORDS or wc>MAX_WORDS:
            it["_reason"]=f"len={wc}"; rej.append(it); continue
        bad=False
        for pat in BANNED_PATTERNS:
            if re.search(pat,q,flags=re.I): bad=True; break
        if bad:
            it["_reason"]="banned_pattern"; rej.append(it)
        else:
            keep.append(it)
    return keep, rej

def downsample_niche(items):
    if NICHE_CAP is None: return items,{}
    by_dom=defaultdict(list)
    for it in items:
        d=(it.get("meta",{}) or {}).get("domain","UNKNOWN")
        by_dom[d].append(it)
    out=[]; cut={}
    for d,arr in by_dom.items():
        if d in NICHE_DOMAINS and len(arr)>NICHE_CAP:
            random.shuffle(arr); out.extend(arr[:NICHE_CAP])
            cut[d]=len(arr)-NICHE_CAP
        else:
            out.extend(arr)
    return out, cut

def cap_categories(items):
    # Cap overrepresented categories by simple downsample to ratio
    total = len(items)
    caps_abs = {k: int(total * v) for k, v in CATEGORY_CAP_RATIO.items()}

    # Gom item theo category (kể cả những category không nằm trong danh sách cap)
    by_cat = defaultdict(list)
    for it in items:
        cat = (it.get("meta") or {}).get("category", "unknown")
        by_cat[cat].append(it)

    kept = []
    report = {}
    for cat, arr in by_cat.items():
        if cat in caps_abs:
            cap = caps_abs[cat]
            if len(arr) > cap:
                random.shuffle(arr)
                kept.extend(arr[:cap])
                report[cat] = f"capped {cap} of {len(arr)} (-{len(arr)-cap})"
            else:
                kept.extend(arr)
                report[cat] = f"keep {len(arr)} (<= cap {cap})"
        else:
            kept.extend(arr)  # category không bị cap -> giữ nguyên

    return kept, report

def balance_levels(items):
    total=len(items)
    lev_map=defaultdict(list)
    for it in items:
        lvl=(it.get("meta",{}) or {}).get("level","unknown").lower()
        lev_map[lvl].append(it)
    targets={k:int(round(total*v)) for k,v in TARGET_LEVEL_RATIO.items()}
    out=[]; rep={}
    for lvl,arr in lev_map.items():
        if lvl in targets:
            if len(arr)>targets[lvl]:
                random.shuffle(arr); keep=arr[:targets[lvl]]
                out.extend(keep); rep[lvl]=f"down {len(arr)-targets[lvl]}"
            else:
                out.extend(arr); rep[lvl]=f"keep {len(arr)} (below {targets[lvl]})"
        else:
            out.extend(arr); rep[lvl]=f"keep {len(arr)} (no target)"
    return out, rep

def grammar_polish(items, limit=GRAMMAR_MAX):
    if not (ENABLE_GRAMMAR and HAS_LANG): return 0
    tool=language_tool_python.LanguageTool('en-US')
    cand=[]; fixed=0
    for i,it in enumerate(items):
        q=(it.get("question") or "").strip()
        wc=len(q.split())
        if GRAMMAR_MIN_LEN<=wc<=GRAMMAR_MAX_LEN:
            cand.append((i,it,q))
        if len(cand)>=limit: break
    for i,it,q in cand:
        try:
            ms=tool.check(q)
            if ms:
                corrected=language_tool_python.utils.correct(q,ms)
                if corrected and corrected!=q:
                    it["question"]=corrected; fixed+=1
        except Exception: pass
    return fixed

# --------- Enrich meta ----------
INTENT_RULES=[
    ("how would you","scenario"),
    ("walk me through","probe"),
    ("why did you choose","contrast"),
    ("trade-off","contrast"),
    ("compare","contrast"),
    ("explain","clarify"),
    ("design","scenario"),
    ("what would you do if","scenario"),
    ("how do you ensure","challenge"),
    ("how would you handle","challenge"),
]
FOCUS_KEYWORDS={
    "security":["jwt","oauth","xss","csrf","encrypt","secret","vault","iam","policy","opa"],
    "performance":["latency","throughput","qps","cache","profil","optimiz","load","benchmark"],
    "scalability":["scale","shard","partition","replica","autoscal","kafka","cluster","horizontal"],
    "reliability":["failover","circuit breaker","retry","timeout","slo","sla","disaster","dr"],
    "data":["modeling","etl","elt","bigquery","warehouse","spark","flink","schema","cdc"],
    "testing":["test","ci/cd","unit","integration","e2e","mock","regression"],
    "observability":["logging","metrics","tracing","otel","prometheus","grafana"],
    "teamwork":["collaborat","stakeholder","team","communication","conflict","leadership"]
}
def infer_intent(q, cat):
    x=q.lower()
    for kw,tag in INTENT_RULES:
        if kw in x: return tag
    c=(cat or "").lower()
    if c in ["architecture","system design","scalability","resilience"]: return "scenario"
    if c in ["security","compliance","governance"]: return "challenge"
    if c in ["testing","devops","performance"]: return "probe"
    return "clarify"
def infer_focus(q,cat):
    x=q.lower()
    for f,kws in FOCUS_KEYWORDS.items():
        if any(k in x for k in kws): return f
    return (cat or "general").lower().replace(" ","-")
def infer_tone(level,cat):
    lvl=(level or "").lower()
    if lvl in ["intern","junior"]: return "friendly"
    if lvl=="mid": return "professional"
    if lvl=="senior":
        if (cat or "").lower() in ["security","architecture","system design","performance"]:
            return "technical"
        return "professional"
    return "professional"
def enrich_meta(items):
    upd=0
    for it in items:
        meta=it.get("meta") or {}
        q=(it.get("question") or "")
        cat=meta.get("category","")
        lvl=meta.get("level","")
        if ENABLE_METADATA_ENRICH:
            meta.setdefault("intent", infer_intent(q,cat))
            meta.setdefault("tone",   infer_tone(lvl,cat))
            meta.setdefault("focus",  infer_focus(q,cat))
            it["meta"]=meta; upd+=1
    return upd

# --------- Stats + HTML ---------
def stats(items, prefix="stats_v3"):
    dom=Counter([(it.get("meta") or {}).get("domain","unknown") for it in items])
    lev=Counter([(it.get("meta") or {}).get("level","unknown") for it in items])
    cat=Counter([(it.get("meta") or {}).get("category","unknown") for it in items])
    with open(f"{prefix}_domain.json","w",encoding="utf-8") as f: json.dump(dom.most_common(),f,ensure_ascii=False,indent=2)
    with open(f"{prefix}_level.json","w",encoding="utf-8") as f: json.dump(lev.most_common(),f,ensure_ascii=False,indent=2)
    with open(f"{prefix}_category.json","w",encoding="utf-8") as f: json.dump(cat.most_common(),f,ensure_ascii=False,indent=2)
    return dom,lev,cat

def html_escape(s): return (s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;")
                            .replace('"',"&quot;").replace("'","&#39;"))

def html_table(title, counter, total):
    rows=[]
    for k,v in counter.most_common():
        pct=f"{(v/total)*100:.2f}%"
        rows.append(f"<tr><td>{html_escape(str(k))}</td><td style='text-align:right'>{v}</td><td style='text-align:right'>{pct}</td></tr>")
    return f"<h3>{title}</h3><table border='1' cellspacing='0' cellpadding='6'><tr><th>Key</th><th>Count</th><th>Percent</th></tr>{''.join(rows)}</table>"

def write_report(path, dom, lev, cat, total):
    html=f"""<!doctype html><html><head><meta charset="utf-8"/>
<title>GenQ v3 Dataset Report</title>
<style>body{{font-family:system-ui,Segoe UI,Roboto,Arial,sans-serif;max-width:980px;margin:24px auto;line-height:1.45}}
h1,h2,h3{{margin:8px 0}} .badge{{display:inline-block;background:#eef;padding:2px 8px;border-radius:999px;margin-right:8px}}
table{{width:100%;border-collapse:collapse;margin:12px 0}} th{{background:#f5f5f5}}</style></head>
<body>
<h1>GenQ v3 Dataset Report</h1>
<p><span class="badge">Total: <b>{total}</b></span>
<span class="badge">Domains: <b>{len(dom)}</b></span>
<span class="badge">Levels: <b>{len(lev)}</b></span>
<span class="badge">Categories: <b>{len(cat)}</b></span></p>
{html_table("Domain Breakdown",dom,total)}
{html_table("Level Breakdown",lev,total)}
{html_table("Category Breakdown",cat,total)}
<p style="color:#777;margin-top:24px">Generated by genq_pipeline.py</p>
</body></html>"""
    with open(path,"w",encoding="utf-8") as f: f.write(html)

# --------- LLM client (optional) ---------
def call_llm(provider, model, system, user, max_tokens=64):
    # Requires requests + API key in env
    if not HAS_REQ: return None
    try:
        if provider=="openrouter":
            key=os.environ.get("OPENROUTER_API_KEY")
            if not key: return None
            url="https://openrouter.ai/api/v1/chat/completions"
            headers={"Authorization":f"Bearer {key}","Content-Type":"application/json"}
            payload={"model":model,"messages":[{"role":"system","content":system},{"role":"user","content":user}],
                     "max_tokens":max_tokens,"temperature":0.7}
            r=requests.post(url,headers=headers,json=payload,timeout=60)
            if r.status_code==200:
                return r.json()["choices"][0]["message"]["content"].strip()
            return None
        elif provider=="openai":
            key=os.environ.get("OPENAI_API_KEY")
            if not key: return None
            url="https://api.openai.com/v1/chat/completions"
            headers={"Authorization":f"Bearer {key}","Content-Type":"application/json"}
            payload={"model":model,"messages":[{"role":"system","content":system},{"role":"user","content":user}],
                     "max_tokens":max_tokens,"temperature":0.7}
            r=requests.post(url,headers=headers,json=payload,timeout=60)
            if r.status_code==200:
                return r.json()["choices"][0]["message"]["content"].strip()
            return None
    except Exception:
        return None

def rewrite_question_with_llm(q, provider, model):
    # Ask LLM to rewrite naturally; fallback to original
    sys_prompt=REWRITE_SYS
    usr=REWRITE_USER_TPL.format(q=q)
    out=call_llm(provider, model, sys_prompt, usr, max_tokens=64)
    if out:
        # strip quotes/bullets if any
        out=re.sub(r'^\s*["•\-]+\s*','',out.strip())
        # enforce single sentence
        out=out.split("\n")[0].strip()
        return out
    return q

def make_followups_llm(items, n_pairs, provider, model, out_path):
    pool = random.sample(items, min(n_pairs, len(items)))
    out = []
    total = len(pool)
    for i, it in enumerate(pool, 1):
        q = it.get("question","").strip()
        meta = it.get("meta",{})
        domain = meta.get("domain","General")
        focus = meta.get("focus","general")
        prev = FOLLOWUP_PREV_TPL.replace("{domain}", domain)
        ans = FOLLOWUP_ANS.get(focus, FOLLOWUP_DEFAULT_ANS)
        usr = FOLLOWUP_USER_TPL.format(prev=prev, ans=ans, focus=focus)

        new = call_llm(provider, model, FOLLOWUP_SYS, usr, max_tokens=64)
        if new:
            new = re.sub(r'^\s*["•\-]+\s*','', new.strip()).split("\n")[0].strip()
            wc = len(new.split())
            if 7 <= wc <= 35:
                out.append({"previous_question":prev,"candidate_answer":ans,"next_question":new,"meta":meta})
            else:
                out.append({"previous_question":prev,"candidate_answer":ans,"next_question":q,"meta":meta})
        else:
            out.append({"previous_question":prev,"candidate_answer":ans,"next_question":q,"meta":meta})

        if i % 50 == 0:
            print(f"    → follow-ups generated: {i}/{total} ({i/total*100:.1f}%)")

        time.sleep(0.2)

    save_jsonl(out_path, out)
    return len(out)


def make_followups_naive(items, n_pairs, out_path):
    pool=random.sample(items,min(n_pairs,len(items)))
    out=[]
    for it in pool:
        q=it.get("question","").strip()
        meta=it.get("meta",{})
        domain=meta.get("domain","General")
        focus=meta.get("focus","general")
        prev=FOLLOWUP_PREV_TPL.replace("{domain}",domain)
        ans=FOLLOWUP_ANS.get(focus, FOLLOWUP_DEFAULT_ANS)
        out.append({"previous_question":prev,"candidate_answer":ans,"next_question":q,"meta":meta})
    save_jsonl(out_path,out)
    return len(out)

# --------- MAIN ---------
def main():
    ap=argparse.ArgumentParser()
    ap.add_argument("--input", required=True)
    ap.add_argument("--out", required=True)
    ap.add_argument("--followups", type=int, default=0, help="Generate N follow-ups into <out>.followups.jsonl")
    ap.add_argument("--llm-provider", choices=["openrouter","openai"], default=None)
    ap.add_argument("--llm-model", default=None)
    ap.add_argument("--rewrite", action="store_true", help="Use LLM to rewrite questions before saving (if API key provided)")
    args=ap.parse_args()

    print(f"[Gate0] Load {args.input}")
    items=load_jsonl(args.input)
    n0=len(items); print("  loaded:",n0)

    print("[Gate1] Dedup")
    items,near_rm=deduplicate(items)
    print(f"  after dedup: {len(items)} (near-dup removed: {near_rm})")

    print("[Gate2] Language filter")
    items, rejects=filter_language(items)
    save_jsonl("g2.lang.rejects.jsonl", rejects)
    print(f"  after gate2: {len(items)} (rejected {len(rejects)})")

    print("[Gate3] Niche cap + Category cap + Level balance")
    items,cut=downsample_niche(items)
    if cut: print("  niche caps:", cut)
    items,caprep=cap_categories(items)
    print("  category caps:", caprep)
    items,lvrep=balance_levels(items)
    print("  level balance:", lvrep)
    print("  after gate3:",len(items))

    if ENABLE_GRAMMAR:
        print("[Gate4] Grammar polish (fallback if no LLM)")
        fixed=grammar_polish(items, limit=GRAMMAR_MAX)
        print("  polished:", fixed)

    if args.rewrite and args.llm_provider and args.llm_model:
        print("[Gate4b] LLM rewrite questions (natural tone)")
    rewrite_limit = min(2000, len(items))
    sampled = random.sample(items, rewrite_limit)
    rewrite_count = 0

    for i, it in enumerate(sampled, 1):
        q = it.get("question", "")
        new = rewrite_question_with_llm(q, args.llm_provider, args.llm_model)
        if new and new != q:
            it["question"] = new
            rewrite_count += 1

        # log tiến độ mỗi 50 câu
        if i % 50 == 0:
            print(f"    → rewritten {rewrite_count}/{i} items ({i/rewrite_limit*100:.1f}%)")

        time.sleep(0.15)  # tránh spam API

    print(f"  ✅ Done rewrite: {rewrite_count}/{rewrite_limit} items\n")


    print("[Gate5] Enrich meta (intent/tone/focus)")
    upd=enrich_meta(items)
    print("  enriched:", upd)

    print("[Save] Refined ->", args.out)
    save_jsonl(args.out, items)

    print("[Stats] JSON + HTML report")
    dom,lev,cat=stats(items, prefix="stats_v3")
    write_report("report_v3.html", dom, lev, cat, len(items))
    print("  wrote: stats_v3_*.json, report_v3.html")

    if args.followups>0:
        fu_path=f"{os.path.splitext(args.out)[0]}.followups.jsonl"
        if args.llm_provider and args.llm_model and (os.environ.get("OPENROUTER_API_KEY") or os.environ.get("OPENAI_API_KEY")):
            print(f"[Bonus] LLM follow-ups x{args.followups}")
            made=make_followups_llm(items, args.followups, args.llm_provider, args.llm_model, fu_path)
        else:
            print(f"[Bonus] Naive follow-ups x{args.followups} (no LLM/API key)")
            made=make_followups_naive(items, args.followups, fu_path)
        print(f"  wrote {made} -> {fu_path}")

    print("\n=== DONE ===")
    print("Input:", n0)
    print("Output:", len(items))
    print("Artifacts:")
    print(" -", args.out)
    print(" - report_v3.html")
    print(" - stats_v3_domain.json / stats_v3_level.json / stats_v3_category.json")
    print(" - g2.lang.rejects.jsonl")
    if args.followups>0:
        print(" -", f"{os.path.splitext(args.out)[0]}.followups.jsonl")

if __name__=="__main__":
    main()
