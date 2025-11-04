import os
import json
import random
import requests
import time  # Thêm thư viện time để xử lý rate limit
from tqdm import tqdm
from faker import Faker

# ===== Config =====
# !!! Đảm bảo bạn đã đặt biến môi trường OPENROUTER_API_KEY
API_KEY = os.getenv("OPENROUTER_API_KEY")
if not API_KEY:
    print("Lỗi: Vui lòng đặt biến môi trường OPENROUTER_API_KEY")
    exit()

API_URL = "https://openrouter.ai/api/v1/chat/completions"

MODEL = "qwen/qwen-2.5-72b-instruct:free"
OUT_FILE = "genq_dataset_full.jsonl"
N_SAMPLES = 200 # Đặt số lượng mẫu là 200
TEMPERATURE = 0.7

fake = Faker()

# ===== Dữ liệu cấu hình (Đã mở rộng) =====

ROLES = [
    ("Backend Developer", "Software Engineering", ["Java", "Spring Boot", "MySQL", "Redis", "Kafka"]),
    ("Frontend Developer", "Web Development", ["React", "TypeScript", "CSS", "Next.js", "GraphQL"]),
    ("Fullstack Developer", "Software Engineering", ["Node.js", "React", "MongoDB", "Express", "TypeScript"]),
    ("Data Analyst", "Data Science", ["Python", "Pandas", "Power BI", "SQL", "Tableau"]),
    ("Data Scientist", "Data Science", ["Python", "TensorFlow", "PyTorch", "Scikit-learn", "SQL"]),
    ("Security Engineer", "Security", ["Nmap", "Metasploit", "OWASP", "SIEM", "Pentesting"]),
    ("Site Reliability Engineer (SRE)", "Infrastructure", ["Kubernetes", "Prometheus", "Go", "Terraform", "Ansible"]),
    ("Business Intelligence Analyst", "Data Science", ["SQL", "Tableau", "SSIS", "Python", "Data Warehousing"]),
    ("Database Administrator (DBA)", "Infrastructure", ["MySQL", "PostgreSQL", "Database Tuning", "Backup/Recovery", "SQL"]),
    ("Network Engineer", "Infrastructure", ["Cisco", "TCP/IP", "BGP", "VPN", "Firewalls"]),
    ("Technical Program Manager (TPM)", "Product", ["Agile", "Scrum", "Project Management", "JIRA", "Roadmapping"]),
    ("Scrum Master", "Product", ["Agile", "Scrum", "Kanban", "JIRA", "Servant Leadership"]),
    ("Interaction Designer", "Design", ["Figma", "Sketch", "User Flows", "Wireframing", "Usability Testing"]),
    ("Solutions Architect", "Software Engineering", ["AWS", "Azure", "Microservices", "System Design", "GCP"])
]

LEVELS = ["Intern", "Junior", "Mid", "Senior", "Lead"]
CATEGORIES = ["Technical", "Behavioral", "Project", "Situational", "Motivational"]
TONES = ["friendly", "neutral", "probing", "formal", "challenging"]

# ===== Hàm gọi API (ĐÃ CẬP NHẬT VỚI LOGIC RETRY) =====

def call_openrouter(prompt):
    """
    Gọi API OpenRouter với prompt được cung cấp.
    Tự động thử lại (retry) nếu gặp lỗi rate limit (429) hoặc câu trả lời rỗng.
    """
    headers = {
        "Authorization": f"Bearer {API_KEY}",
        "Content-Type": "application/json"
    }
    payload = {
        "model": MODEL,
        "temperature": TEMPERATURE,
        "max_tokens": 200,
        "messages": [
            {"role": "system", "content": "You are a professional AI interviewer. Your sole task is to generate a single, realistic, high-quality English interview question based on the user's prompt. Do NOT include any preamble, introduction (like 'Certainly, here is...'), or markdown formatting (like 'Question:' or quotes). Just return the question text directly."},
            {"role": "user", "content": prompt}
        ]
    }
    
    max_retries = 5
    base_delay = 5  # Bắt đầu với 5 giây (vì model free có rate limit nghiêm ngặt)

    for attempt in range(max_retries):
        try:
            r = requests.post(API_URL, headers=headers, json=payload, timeout=60)
            
            # Kiểm tra lỗi 429 (Rate Limit) trước khi raise_for_status
            if r.status_code == 429:
                # Ném lỗi RequestException để kích hoạt logic retry
                raise requests.exceptions.RequestException(f"429 Client Error: Too Many Requests")

            r.raise_for_status()  # Báo lỗi cho các status code 4xx/5xx khác
            
            response_text = r.json()["choices"][0]["message"]["content"].strip()
            
            # Làm sạch các ký tự đặc biệt của model
            response_text = response_text.strip("<s>").strip("</s>").strip("[/s]").strip()

            if not response_text:
                # Nếu model trả về rỗng, coi đây là một lỗi và thử lại
                raise requests.exceptions.RequestException("Empty response from model")

            return response_text  # Thành công, trả về kết quả

        except requests.exceptions.RequestException as e:
            delay = base_delay * (2 ** attempt) + random.uniform(0, 1)  # Exponential backoff + jitter
            
            # Kiểm tra xem có phải lỗi 429 không
            is_rate_limit = "429" in str(e)
            is_empty_response = "Empty response" in str(e)

            # Chỉ thử lại nếu là lỗi 429 hoặc câu hỏi rỗng, và chưa hết số lần thử
            if (is_rate_limit or is_empty_response) and attempt < max_retries - 1:
                if is_rate_limit:
                    tqdm.write(f"\nLỗi 429: Đã đạt giới hạn. Đang thử lại sau {delay:.2f} giây... (lần {attempt + 1}/{max_retries})")
                else:
                    tqdm.write(f"\nLỗi: Câu hỏi rỗng. Đang thử lại sau {delay:.2f} giây... (lần {attempt + 1}/{max_retries})")
                
                time.sleep(delay)
            
            else: 
                # Lỗi không phải 429/rỗng, hoặc đã hết số lần thử
                error_message = f"Error: Could not generate question. (Details: {e})"
                tqdm.write(f"\n{error_message}") # Log lỗi
                return error_message # Trả về lỗi để vòng lặp chính bỏ qua
    
    # Hết vòng lặp mà vẫn lỗi
    final_error = f"Error: Đã thất bại sau {max_retries} lần thử."
    tqdm.write(f"\n{final_error}")
    return final_error


# ===== Các hàm sinh dữ liệu thực tế hơn =====

def generate_jd(role, domain, tech_stack, level):
    """Tạo một Job Description (JD) thực tế hơn."""
    company = fake.company()
    responsibilities = [
        f"Design, develop, and maintain high-quality {domain} solutions.",
        f"Collaborate with cross-functional teams to define and ship new features.",
        f"Optimize applications for scalability, performance, and reliability.",
        f"Write clean, testable, and maintainable code using {random.choice(tech_stack)}.",
        f"Participate in code reviews and advocate for software development best practices."
    ]
    preferred_quals = [
        f"Experience with cloud platforms (e.g., {random.choice(['AWS', 'GCP', 'Azure'])}).",
        f"{random.randint(2, 5)}+ years of experience in a similar role.",
        f"Strong understanding of {random.choice(['Agile methodologies', 'microservices architecture', 'data structures'])}.",
        f"Bachelor's degree in Computer Science or related field."
    ]
    
    jd = (
        f"**Company:** {company}\n"
        f"**About Us:** We are a fast-growing tech company focused on {fake.bs()}.\n"
        f"**Role:** {level} {role}\n\n"
        f"**Key Responsibilities:**\n"
        f"- {random.choice(responsibilities)}\n"
        f"- {random.choice(responsibilities)}\n\n"
        f"**Required Qualifications:**\n"
        f"- Proven experience with {', '.join(random.sample(tech_stack, k=min(len(tech_stack), 3)))}.\n"
        f"- Strong understanding of best practices in {domain}.\n"
        f"- Excellent problem-solving and communication skills.\n\n"
        f"**Preferred Qualifications:**\n"
        f"- {random.choice(preferred_quals)}\n"
    )
    return jd

def generate_cv(role, tech_stack, level):
    """Tạo một trích đoạn CV (CV snippet) thực tế hơn."""
    name = fake.name()
    # SỬA LỖI: Đổi catch_phrase_noun() thành catch_phrase()
    summary = f"Results-driven {level} {role} with {random.randint(1, 8)} years of experience in {random.choice(tech_stack)} and {fake.catch_phrase().lower()}."
    
    projects = [
        f"Architected and implemented a new {random.choice(['microservice', 'frontend module', 'data pipeline'])} using {random.choice(tech_stack)}, improving system efficiency by {random.randint(10, 30)}%.",
        f"Led a team of {random.randint(2, 5)} developers in a project to refactor legacy code, reducing bugs by {random.randint(20, 50)}%.",
        f"Developed a {random.choice(['REST API', 'CI/CD pipeline', 'dashboard'])} that processed {random.randint(100, 1000)}s of requests per second.",
        f"Streamlined development workflow by integrating {random.choice(tech_stack if tech_stack else ['Docker', 'Jenkins'])}.",
        f"Collaborated with Product Managers to deliver a key feature on a tight deadline."
    ]
    
    # Chọn 2-3 thành tựu
    cv_projects = random.sample(projects, k=random.randint(2, 3))
    
    cv = (
        f"**Name:** {name}\n"
        f"**Summary:** {summary}\n\n"
        f"**Key Experience & Achievements:**\n"
    )
    for p in cv_projects:
        cv += f"- {p}\n"
    return cv

def generate_custom(role):
    """Tạo một yêu cầu tùy chỉnh."""
    topics = [
        "teamwork and communication",
        "problem solving under pressure",
        "leadership and decision making",
        "time management and prioritization",
        "career motivation and learning",
        "handling conflict with a coworker"
    ]
    return f"Ask me interview questions about {random.choice(topics)} for a {role}."

def generate_follow_up():
    """Tạo một cặp câu hỏi/trả lời trước đó (đã sửa để logic hơn)."""
    # Danh sách các cặp (câu hỏi, câu trả lời) logic
    qa_pairs = [
        # Version Control (VC)
        ("How did you handle version control in your last project?", 
         "I primarily used Git with a feature-branch workflow. We used pull requests for code reviews before merging to main."),
        ("What was your team's branching strategy?", 
         "We followed a GitFlow-like strategy, with main, develop, and feature branches. Hotfixes were applied directly to main and then merged back."),
        ("Can you describe a time you had a merge conflict?", 
         "Yes, another developer and I edited the same config file. I resolved it by communicating with them, then manually merging our changes in VS Code."),
        
        # Deadlines / Workflow
        ("Can you describe how you managed tight deadlines?", 
         "We prioritized tasks using daily standups and a Kanban board in Jira, focusing on the most critical features first."),
        ("How did you improve your team’s workflow?", 
         "I introduced CI/CD automation using Jenkins, which significantly reduced our manual deployment time from hours to minutes."),
        
        # Monitoring
        ("What tools did you use for monitoring system health?", 
         "We implemented Prometheus for metrics collection and Grafana for visualization. We tracked things like CPU, memory, and API response times."),
        ("How did you handle a production issue?",
         "We had an API outage. I first checked the logs in Datadog, identified a database connection pool exhaustion, and we scaled the DB connections.")
    ]
    
    # Chọn một cặp ngẫu nhiên
    prev_q, prev_a = random.choice(qa_pairs)
    return prev_q, prev_a

# ===== Hàm Main =====

def main():
    """Hàm chính để sinh dataset."""
    dataset = []
    print(f"Bắt đầu sinh {N_SAMPLES} mẫu... (Model: {MODEL})")
    
    # Xóa file cũ nếu tồn tại để chạy lại từ đầu
    if os.path.exists(OUT_FILE):
        os.remove(OUT_FILE)
        
    for _ in tqdm(range(N_SAMPLES)):
        role, domain, tech_stack = random.choice(ROLES)
        level = random.choice(LEVELS)
        category = random.choice(CATEGORIES)
        tone = random.choice(TONES)

        mode = random.choice(["JD", "CV", "CUSTOM", "FOLLOW-UP"])
        item = {}

        try:
            if mode == "JD":
                jd = generate_jd(role, domain, tech_stack, level)
                # Prompt được cải thiện: thêm category, tone, level
                prompt = (
                    f"Job Description:\n{jd}\n\n"
                    f"As a {tone} interviewer, generate one {category} question "
                    f"for this {level} {role} candidate."
                )
                question = call_openrouter(prompt)
                item = {
                    "input": {"type": "JD", "jd": jd},
                    "meta": {"role": role, "domain": domain, "level": level, "language": "English", "category": category, "tone": tone, "tech_stack": tech_stack},
                    "output": {"question": question}
                }

            elif mode == "CV":
                cv = generate_cv(role, tech_stack, level)
                # Prompt được cải thiện: thêm category, tone, level
                prompt = (
                    f"Candidate CV Snippet:\n{cv}\n\n"
                    f"As a {tone} interviewer, generate one {category} question "
                    f"to probe this candidate's experience for a {level} {role} position."
                )
                question = call_openrouter(prompt)
                item = {
                    "input": {"type": "CV", "cv": cv},
                    "meta": {"role": role, "domain": domain, "level": level, "language": "English", "category": category, "tone": tone, "tech_stack": tech_stack},
                    "output": {"question": question}
                }

            elif mode == "CUSTOM":
                custom = generate_custom(role)
                # Prompt được cải thiện: thêm category, tone, level
                prompt = (
                    f"Custom user request:\n{custom}\n\n"
                    f"As a {tone} interviewer, generate one {category} question "
                    f"for a {level} candidate that matches this request."
                )
                question = call_openrouter(prompt)
                item = {
                    "input": {"type": "CUSTOM", "custom": custom},
                    "meta": {"role": role, "domain": domain, "level": level, "language": "English", "category": category, "tone": tone},
                    "output": {"question": question}
                }

            else:  # FOLLOW-UP
                prev_q, prev_a = generate_follow_up()
                # Prompt được cải thiện: thêm category, tone
                prompt = (
                    f"Previous question:\n{prev_q}\n\n"
                    f"Candidate answer:\n{prev_a}\n\n"
                    f"As a {tone} interviewer, generate one {category} follow-up question."
                )
                question = call_openrouter(prompt)
                item = {
                    # Sửa lỗi: type: "FOLLOW-UP"
                    "input": {"type": "FOLLOW-UP", "previous_question": prev_q, "previous_answer": prev_a},
                    "meta": {"role": role, "domain": domain, "level": level, "language": "English", "category": category, "tone": tone, "tech_stack": tech_stack},
                    "output": {"question": question}
                }

            # Chỉ ghi file nếu item không rỗng và câu hỏi không chứa lỗi VÀ câu hỏi không rỗng
            if item and "Error" not in item["output"]["question"] and item["output"]["question"]:
                dataset.append(item)
                with open(OUT_FILE, "a", encoding="utf-8") as f:
                    f.write(json.dumps(item, ensure_ascii=False) + "\n")
            elif "Error" in item.get("output", {}).get("question", ""):
                 # Không cần làm gì, lỗi đã được log bởi call_openrouter
                 pass
            else:
                tqdm.write(f"\nBỏ qua mẫu do model trả về câu hỏi rỗng (sau khi thử lại).")


        except Exception as e:
            # Bắt các lỗi không mong muốn (ví dụ: lỗi Faker nếu vẫn còn)
            tqdm.write(f"\nLỗi trong vòng lặp chính (ngoài API): {e}")

    print(f"\n✅ Hoàn thành! Đã tạo {len(dataset)} mẫu hợp lệ → lưu vào {OUT_FILE}")

if __name__ == "__main__":
    main()
