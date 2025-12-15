package com.capstone.ai_interview_be.service.OptionsService;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

@Service
@Slf4j
public class JDScraperService {
    // Thời gian chờ kết nối (milliseconds)
    private static final int TIMEOUT_MS = 15000; 
    // User-Agent để giả lập trình duyệt
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";


    // Phương thức chính để trích xuất JD từ URL
    public String scrapeJDFromUrl(String url) throws IOException {
        // Kiểm tra tính hợp lệ của URL
        validateUrl(url);
        log.info("Scraping JD from URL: {}", url);
        
        try {
            // Kết nối và lấy nội dung trang web từ URL
            Document doc = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT_MS)
                    .followRedirects(true)
                    .get();
            // Trích xuất tên miền từ URL
            String domain = extractDomain(url);
            // Trích xuất nội dung JD dựa trên tên miền
            String jdText = extractJDContent(doc, domain);
            
            if (jdText == null || jdText.trim().isEmpty()) {
                throw new IOException("No job description content found on the page");
            }
            log.info("Successfully scraped JD ");
            return cleanText(jdText);
            
        } catch (IOException e) {
            log.error("Failed to scrape JD from URL");
            throw new IOException("Failed to fetch job description from URL: " + e.getMessage(), e);
        }
    }
    
    // Hàm trích xuất nội dung JD dựa trên tên miền
    private String extractJDContent(Document doc, String domain) {
        // Tìm nội dung JD dựa trên các selector phổ biến
        String content = extractWithCommonSelectors(doc);
        
        // Nếu không đủ nội dung, dùng phương pháp chung
        if (content.length() < 100) {
            content = extractGeneric(doc);
        }
        
        return content;
    }
    
    //Hàm trích xuất nội dung JD từ các selector phổ biến - Version nâng cao
    private String extractWithCommonSelectors(Document doc) {
        // Sử dụng StringBuilder để xây dựng nội dung JD
        StringBuilder sb = new StringBuilder();
        
        // 1. Tìm tiêu đề công việc
        String[] titleSelectors = {
            "h1.job-title", "h1[class*='job']", "h1[class*='title']",
            "h1.topcard__title", "h1.top-card-layout__title",
            ".job-detail__info--title", "h1.jobsearch-JobInfoHeader-title",
            "[data-automation='job-detail-title']", ".job-title__text",
            "h1", "h2.job-title"
        };
        String title = extractFirstMatch(doc, titleSelectors);
        if (!title.isEmpty()) {
            sb.append("- POSITION: \n").append(title).append("\n\n");
        }
        
        // 2. Tìm tên công ty
        String[] companySelectors = {
            ".company-name", ".employer-name", "[class*='company']",
            ".topcard__org-name-link", ".topcard__flavor--black",
            "[data-automation='job-detail-company']", "a.jobsearch-InlineCompanyRating",
            ".job-company-name", "span[itemprop='name']"
        };
        
        String company = extractFirstMatch(doc, companySelectors);
        if (!company.isEmpty()) {
            sb.append("- COMPANY: \n").append(company).append("\n\n");
        }
        
        // 3. Tìm mức lương
        String[] salarySelectors = {
            ".salary", "[class*='salary']", "[data-automation='job-detail-salary']",
            ".salary-snippet", ".job-salary", "span[itemprop='baseSalary']",
            "[class*='compensation']"
        };
        
        String salary = extractFirstMatch(doc, salarySelectors);
        if (!salary.isEmpty()) {
            sb.append("- SALARY: \n").append(salary).append("\n\n");
        }
        
        // 4. Tìm loại hình công việc
        String[] jobTypeSelectors = {
            ".job-type", "[class*='employment']", "[data-automation='job-detail-work-type']",
            ".jobsearch-JobMetadataHeader-item", "[class*='work-type']"
        };
        
        String jobType = extractFirstMatch(doc, jobTypeSelectors);
        if (!jobType.isEmpty()) {
            sb.append("- JOB TYPE: \n").append(jobType).append("\n\n");
        }
        
        // 5. Tìm kinh nghiệm yêu cầu
        String[] experienceSelectors = {
            ".experience", "[class*='experience']", "[class*='seniority']",
            "[data-automation='job-detail-experience']", ".job-experience-level"
        };
        
        String experience = extractFirstMatch(doc, experienceSelectors);
        if (!experience.isEmpty()) {
            sb.append("- EXPERIENCE: \n").append(experience).append("\n\n");
        }
        
        // 6. Tìm mô tả công việc
        String[] descriptionSelectors = {
            // Generic selectors
            "div.job-description", "div#job-description", 
            "section.job-description", "article.job-description",
            
            // Class patterns
            "div[class*='job-desc']", "div[class*='description']",
            "div[class*='job_desc']", "section[class*='description']",
            
            // ID patterns
            "div#jobDescriptionText", "div#description",
            
            // Popular job sites
            ".show-more-less-html__markup", ".description__text",
            ".job-detail__information-detail", ".job-description__content",
            "[data-automation='jobDescription']", ".jobsearch-jobDescriptionText",
            
            // Fallback to main content
            "main", "article", ".content", ".main-content"
        };
        
        String description = extractFirstMatch(doc, descriptionSelectors);
        if (!description.isEmpty()) {
            sb.append("- JOB DESCRIPTION: \n").append(description).append("\n\n");
        }
        
        // 7. Tìm yêu cầu công việc
        String[] requirementsSelectors = {
            "div[class*='requirement']", "section[class*='requirement']",
            "div[class*='qualification']", "[class*='skills-section']",
            "div:contains(Yêu cầu)", "div:contains(Requirements)",
            "div:contains(Qualifications)"
        };
        
        String requirements = extractFirstMatch(doc, requirementsSelectors);
        if (!requirements.isEmpty() && !description.contains(requirements)) {
            sb.append("- REQUIREMENTS: \n").append(requirements).append("\n\n");
        }
        
        // 8. Tìm quyền lợi
        String[] benefitsSelectors = {
            "div[class*='benefit']", "section[class*='benefit']",
            "div[class*='perk']", "[class*='welfare']",
            "div:contains(Quyền lợi)", "div:contains(Benefits)",
            "div:contains(What we offer)"
        };
        
        String benefits = extractFirstMatch(doc, benefitsSelectors);
        if (!benefits.isEmpty() && !description.contains(benefits)) {
            sb.append("- BENEFITS: \n").append(benefits).append("\n\n");
        }
        
        // 9. Tìm thông tin liên hệ
        String[] contactSelectors = {
            ".contact-info", "[class*='contact']", "[class*='apply']",
            ".application-details", "[data-automation='job-detail-apply']"
        };
        
        String contact = extractFirstMatch(doc, contactSelectors);
        if (!contact.isEmpty()) {
            sb.append("- CONTACT INFO: \n").append(contact).append("\n\n");
        }
        
        return sb.toString();
    }
    
    // Hàm trích xuất phần tử đầu tiên khớp với các selector đã cho
    private String extractFirstMatch(Document doc, String[] selectors) {
        for (String selector : selectors) {
            Elements elements = doc.select(selector);
            if (!elements.isEmpty()) {
                String text = elements.first().text().trim();
                if (text.length() > 20) { 
                    return text;
                }
            }
        }
        return "";
    }
    
    // Phương pháp chung để trích xuất nội dung JD
    private String extractGeneric(Document doc) {
        // Xóa các phần không cần thiết
        doc.select("nav, header, footer, script, style, .nav, .header, .footer, .menu, .sidebar").remove();
        
        // Lấy nội dung chính
        String bodyText = doc.body().text();
        
        // Lọc và làm sạch
        return bodyText.length() > 100 ? bodyText : "";
    }
    
    // Hàm làm sạch và định dạng văn bản JD
    private String cleanText(String text) {
        if (text == null) return "";

        String cleaned = text
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
        
        String[] lines = cleaned.split("\\n");
        StringBuilder result = new StringBuilder();
        String previousLine = "";
        int duplicateCount = 0;
        
        // Vòng lặp qua từng dòng để loại bỏ dòng trống và dòng lặp lại
        for (String line : lines) {
            String trimmedLine = line.trim();
            
            // Nếu dòng giống dòng trước đó, tăng bộ đếm lặp lại
            if (trimmedLine.equals(previousLine)) {
                duplicateCount++;
                // Nếu lặp lại quá nhiều lần, bỏ qua dòng này
                if (duplicateCount > 1) {
                    continue;
                }
            } else {
                duplicateCount = 0;
            }
            
            // Cập nhật dòng trước đó
            previousLine = trimmedLine;

            // Bỏ qua các dòng rất ngắn có khả năng là tiếng ồn
            if (trimmedLine.length() > 0) {
                result.append(trimmedLine).append("\n");
            }
        }
        
        // Định dạng cuối cùng với các tiêu đề và dấu đầu dòng
        String formatted = result.toString()
                // Thêm ngắt dòng trước các tiêu đề phổ biến
                .replaceAll("(?i)(Mô tả công việc|Job Description|Yêu cầu|Requirements?|Quyền lợi|Benefits?|Địa điểm|Location|Thời gian|Working Time)", "\n\n$1")
                // Thêm ngắt dòng sau các dấu đầu dòng
                .replaceAll("•", "\n• ")
                // Thêm ngắt dòng sau các dấu gạch ngang
                .replaceAll("-\\s+([A-Z])", "\n- $1")
                // Làm sạch khoảng trắng thừa
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
        
        return formatted;
    }
    
    // Hàm kiểm tra tính hợp lệ của URL
    private void validateUrl(String urlString) throws MalformedURLException {

        if (urlString == null || urlString.trim().isEmpty()) {
            throw new MalformedURLException("URL cannot be empty");
        }

        if (!urlString.startsWith("http://") && !urlString.startsWith("https://")) {
            throw new MalformedURLException("URL must start with http:// or https://");
        }

        try {
            new URL(urlString);
        } catch (MalformedURLException e) {
            throw new MalformedURLException("Invalid URL format: " + e.getMessage());
        }
    }
    
    // Hàm trích xuất tên miền từ URL
    private String extractDomain(String urlString) {
        try {
            URL url = new URL(urlString);
            return url.getHost().toLowerCase();
        } catch (MalformedURLException e) {
            return "";
        }
    }
}

