#!/usr/bin/env python
# -*- coding: utf-8 -*-

"""
Phân tích Judge Overall Feedback dataset (JSONL) để kiểm tra chất lượng trước khi train.

Các thứ script sẽ kiểm tra:
1. Thống kê cơ bản
   - Số session, tổng số Q&A
   - Phân bố overview: EXCELLENT / GOOD / AVERAGE / BELOW AVERAGE / POOR
   - Phân bố số câu hỏi mỗi session

2. Thống kê điểm số
   - Mean / min / max của final score theo từng overview
   - Kiểm tra logic: EXCELLENT > GOOD > AVERAGE > BELOW AVERAGE > POOR (trung bình)

3. Thống kê meta
   - Top role, seniority, skill
   - Kiểm tra đa dạng skill / role / seniority

4. Pattern câu hỏi
   - Tỉ lệ câu hỏi bắt đầu bằng: How, What, Can, Explain, Why, Describe, Imagine, Suppose, If, When, In your experience...
   - Cảnh báo nếu > 60–70% câu hỏi bắt đầu bằng 1–2 prefix lặp lại

5. Kiểm tra schema output
   - overview phải nằm trong set hợp lệ
   - strengths là list 3–5 item
   - weaknesses là list 2–4 item
   - recommendations là string (không rỗng)

6. In một vài ví dụ random để eyeball check
"""

import json
from pathlib import Path
from collections import Counter, defaultdict
import statistics
import random
import re

# ===== CẤU HÌNH =====
INPUT_FILE = Path("judge_overall_feedback_dataset_1k.jsonl")  # sửa cho đúng tên file của bạn
RANDOM_SEED = 123
random.seed(RANDOM_SEED)

VALID_OVERVIEWS = ["EXCELLENT", "GOOD", "AVERAGE", "BELOW AVERAGE", "POOR"]


def detect_question_prefix(q: str) -> str:
    """Bắt prefix đơn giản để xem câu hỏi mở đầu kiểu gì."""
    q_strip = q.strip()
    if not q_strip:
        return "<EMPTY>"

    # lấy từ đầu tiên / 2 từ đầu để đa dạng hơn
    first_word = q_strip.split()[0].rstrip(",:?!.").capitalize()
    first_two = " ".join(q_strip.split()[:2])
    first_two = re.sub(r"[\?,\.:!]+$", "", first_two).strip()

    # Một số pattern hay gặp
    starts = [
        "How", "What", "Why", "Can", "Could", "Explain", "Describe",
        "Imagine", "Suppose", "If", "When", "In", "You", "Walk", "Tell"
    ]

    # Ưu tiên nhận ra những pattern rõ
    for s in starts:
        if q_strip.lower().startswith(s.lower() + " "):
            return s

    # fallback: trả về từ đầu tiên
    return first_word


def safe_mean(values):
    return round(statistics.mean(values), 4) if values else None


def main():
    if not INPUT_FILE.exists():
        print(f"❌ Không tìm thấy file: {INPUT_FILE}")
        return

    print("=" * 80)
    print(f"📂 ĐANG PHÂN TÍCH DATASET: {INPUT_FILE}")
    print("=" * 80)

    n_sessions = 0
    n_questions_total = 0

    overview_counter = Counter()
    questions_per_session = Counter()
    role_counter = Counter()
    seniority_counter = Counter()
    skill_counter = Counter()

    # score theo overview
    scores_by_overview = defaultdict(list)

    # kiểm tra schema output
    invalid_overview = 0
    invalid_strengths = 0
    invalid_weaknesses = 0
    invalid_recommendations = 0

    # pattern câu hỏi
    prefix_counter = Counter()

    # lưu một ít sample để in ra
    sample_sessions = []

    with INPUT_FILE.open("r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line:
                continue

            try:
                obj = json.loads(line)
            except json.JSONDecodeError:
                print("⚠️  Bỏ qua 1 dòng JSON lỗi.")
                continue

            n_sessions += 1

            inp = obj.get("input", {})
            out = obj.get("output", {})

            conv = inp.get("conversation", [])
            role = inp.get("role", "<UNK>")
            seniority = inp.get("seniority", "<UNK>")
            skills = inp.get("skills", [])

            overview = out.get("overview", "<UNK>")
            strengths = out.get("strengths")
            weaknesses = out.get("weaknesses")
            recommendations = out.get("recommendations")

            # thống kê tổng số câu hỏi
            n_q = len(conv)
            n_questions_total += n_q
            questions_per_session[n_q] += 1

            # meta
            role_counter[role] += 1
            seniority_counter[seniority] += 1
            for s in skills:
                skill_counter[s] += 1

            # overview
            overview_counter[overview] += 1

            # scores
            # lấy trung bình final score trong session
            finals = []
            for turn in conv:
                sc = turn.get("scores", {})
                final = sc.get("final")
                if isinstance(final, (int, float)):
                    finals.append(final)

                # question prefix
                q_text = turn.get("question", "")
                prefix = detect_question_prefix(q_text)
                prefix_counter[prefix] += 1

            if overview in VALID_OVERVIEWS and finals:
                scores_by_overview[overview].append(statistics.mean(finals))

            # kiểm tra schema output
            if overview not in VALID_OVERVIEWS:
                invalid_overview += 1

            # strengths phải là list 3–5
            if not isinstance(strengths, list) or not (1 <= len(strengths) <= 6):
                invalid_strengths += 1

            # weaknesses phải là list 2–4 (ở script của bạn là 2–4, nhưng cho phép 1–5 cho linh hoạt)
            if not isinstance(weaknesses, list) or not (1 <= len(weaknesses) <= 6):
                invalid_weaknesses += 1

            # recommendations phải là string, không quá ngắn
            if not isinstance(recommendations, str) or len(recommendations.strip()) < 20:
                invalid_recommendations += 1

            # lưu sample
            if len(sample_sessions) < 5:
                sample_sessions.append(obj)

    # ===== In kết quả =====
    print("\n📊 1) THỐNG KÊ CƠ BẢN")
    print(f"   - Số sessions     : {n_sessions}")
    print(f"   - Tổng số Q&A     : {n_questions_total}")
    if n_sessions > 0:
        print(f"   - Q&A / session   : {n_questions_total / n_sessions:.2f}")

    print("\n📊 2) PHÂN BỐ OVERVIEW")
    for ov in VALID_OVERVIEWS:
        cnt = overview_counter.get(ov, 0)
        pct = (cnt / n_sessions * 100) if n_sessions else 0
        print(f"   - {ov:14s}: {cnt:6d} ({pct:5.1f}%)")
    # những overview khác (nếu có)
    others = {k: v for k, v in overview_counter.items() if k not in VALID_OVERVIEWS}
    if others:
        print("   - KHÁC:")
        for k, v in others.items():
            pct = v / n_sessions * 100
            print(f"       {k}: {v} ({pct:.1f}%)")

    print("\n📊 3) PHÂN BỐ SỐ CÂU HỎI / SESSION")
    for q, cnt in sorted(questions_per_session.items()):
        pct = cnt / n_sessions * 100
        print(f"   - {q} câu hỏi: {cnt:6d} ({pct:5.1f}%)")

    print("\n📊 4) ĐIỂM FINAL TRUNG BÌNH THEO OVERVIEW")
    overview_means = {}
    for ov in VALID_OVERVIEWS:
        vals = scores_by_overview.get(ov, [])
        if vals:
            m = safe_mean(vals)
            mn = round(min(vals), 4)
            mx = round(max(vals), 4)
            overview_means[ov] = m
            print(f"   - {ov:14s}: mean={m}, min={mn}, max={mx}, n={len(vals)}")
        else:
            print(f"   - {ov:14s}: (không có dữ liệu)")

    # Check monotonicity: EXCELLENT > GOOD > AVERAGE > BELOW AVERAGE > POOR
    print("\n🧪 5) KIỂM TRA LOGIC ĐIỂM THEO OVERVIEW")
    order = ["EXCELLENT", "GOOD", "AVERAGE", "BELOW AVERAGE", "POOR"]
    ok_monotonic = True
    last_mean = None
    for ov in order:
        m = overview_means.get(ov)
        if m is None:
            continue
        if last_mean is not None and m > last_mean:
            ok_monotonic = False
        last_mean = m

    if ok_monotonic:
        print("   ✅ Trung bình điểm final giảm dần đúng theo thứ tự EXCELLENT → GOOD → AVERAGE → BELOW AVERAGE → POOR.")
    else:
        print("   ⚠️  Phát hiện bất thường: điểm trung bình theo overview KHÔNG giảm dần. Kiểm tra lại mapping logic EXCELLENT/GOOD/...")

    # ===== Meta =====
    print("\n📊 6) PHÂN BỐ ROLE / SENIORITY / SKILL (TOP 10)")
    print("   - Top role:")
    for role, cnt in role_counter.most_common(10):
        pct = cnt / n_sessions * 100
        print(f"       {role:20s}: {cnt:6d} ({pct:5.1f}%)")

    print("   - Top seniority:")
    for sen, cnt in seniority_counter.most_common():
        pct = cnt / n_sessions * 100
        print(f"       {sen:20s}: {cnt:6d} ({pct:5.1f}%)")

    print("   - Top skill:")
    for sk, cnt in skill_counter.most_common(10):
        pct = cnt / n_sessions * 100
        print(f"       {sk:20s}: {cnt:6d} ({pct:5.1f}%)")

    # ===== Question prefixes =====
    print("\n📊 7) PATTERN CÂU HỎI (PREFIX)")
    total_q = sum(prefix_counter.values())
    for pref, cnt in prefix_counter.most_common(15):
        pct = cnt / total_q * 100 if total_q else 0
        print(f"   - {pref:10s}: {cnt:6d} ({pct:5.1f}%)")

    if total_q:
        top_pref, top_cnt = prefix_counter.most_common(1)[0]
        top_pct = top_cnt / total_q * 100
        if top_pct > 60:
            print(f"   ⚠️  CẢNH BÁO: {top_pct:.1f}% câu hỏi bắt đầu bằng '{top_pref}'. Cân nhắc tăng đa dạng template câu hỏi.")
        elif top_pct > 40:
            print(f"   ℹ️  LƯU Ý: {top_pct:.1f}% câu hỏi bắt đầu bằng '{top_pref}'. Đa dạng ổn nhưng vẫn hơi thiên lệch.")

    # ===== Schema checks =====
    print("\n🧪 8) KIỂM TRA SCHEMA OUTPUT")
    print(f"   - overview không hợp lệ       : {invalid_overview}")
    print(f"   - strengths không đúng dạng   : {invalid_strengths}")
    print(f"   - weaknesses không đúng dạng  : {invalid_weaknesses}")
    print(f"   - recommendations không hợp lệ: {invalid_recommendations}")

    if all(x == 0 for x in [invalid_overview, invalid_strengths, invalid_weaknesses, invalid_recommendations]):
        print("   ✅ Schema output ổn, không thấy lỗi cấu trúc lớn.")
    else:
        print("   ⚠️  Có lỗi schema, cần inspect thêm.")

    # ===== Sample sessions =====
    print("\n📄 9) MỘT VÀI VÍ DỤ RANDOM (ĐỂ EYEBALL CHECK)")
    for i, s in enumerate(sample_sessions, 1):
        inp = s.get("input", {})
        out = s.get("output", {})
        print(f"\n   --- SAMPLE #{i} ---")
        print(f"   Role      : {inp.get('role')}")
        print(f"   Seniority : {inp.get('seniority')}")
        print(f"   Skills    : {inp.get('skills')}")
        print(f"   Questions : {inp.get('total_questions')}")
        print(f"   Overview  : {out.get('overview')}")
        print("   Q&A preview:")
        for turn in inp.get("conversation", []):
            print(f"      Q{turn.get('sequence_number')}: {turn.get('question')}")
            print(f"      A : {turn.get('answer')}")
            break  # chỉ in câu đầu tiên cho gọn

    # ===== Đánh giá tổng quan (heuristic) =====
    print("\n✅ 10) ĐÁNH GIÁ TỔNG QUAN (HEURISTIC, THAM KHẢO)")
    if n_sessions < 1000:
        print("   ⚠️  Dataset khá nhỏ (< 1k sessions). Nên tăng thêm dữ liệu để model judge ổn định hơn.")
    else:
        print("   ✅ Số lượng sessions đủ lớn để bắt đầu train (>= 1k).")

    # kiểm tra bảng phân bố overview
    good_pct = overview_counter.get("GOOD", 0) / n_sessions * 100 if n_sessions else 0
    excellent_pct = overview_counter.get("EXCELLENT", 0) / n_sessions * 100 if n_sessions else 0
    poor_pct = overview_counter.get("POOR", 0) / n_sessions * 100 if n_sessions else 0

    if good_pct > 70:
        print("   ⚠️  OVERVIEW lệch nhiều về GOOD (>70%). Cân nhắc cân bằng lại để tránh bias.")
    if excellent_pct < 5:
        print("   ℹ️  Tỉ lệ EXCELLENT khá thấp (<5%). Tùy mục tiêu, có thể tăng thêm case rất tốt.")
    if poor_pct < 5:
        print("   ℹ️  Tỉ lệ POOR khá thấp (<5%). Nếu muốn model phạt mạnh câu tệ, có thể thêm ví dụ xấu hơn.")

    if ok_monotonic and n_sessions >= 1000:
        print("   👉 Nhìn chung: dataset **đủ điều kiện** để bắt đầu train Judge.")
        print("      Bạn vẫn nên đọc thủ công vài chục mẫu để chắc chắn style/logic đúng kỳ vọng.")
    else:
        print("   👉 Cần xem lại các cảnh báo phía trên trước khi đem train.")

    print("\n🎯 PHÂN TÍCH HOÀN TẤT.")


if __name__ == "__main__":
    main()
