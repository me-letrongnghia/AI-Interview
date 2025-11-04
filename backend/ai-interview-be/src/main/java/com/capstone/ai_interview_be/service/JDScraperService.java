package com.capstone.ai_interview_be.service;

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

    private static final int TIMEOUT_MS = 15000; // 15 seconds
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    /**
     * Scrape JD content from URL
     * @param url URL of the job posting
     * @return Extracted JD text content
     * @throws IOException if connection fails
     */
    public String scrapeJDFromUrl(String url) throws IOException {
        // Validate URL
        validateUrl(url);
        
        log.info("Scraping JD from URL: {}", url);
        
        try {
            // Fetch HTML document
            Document doc = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT_MS)
                    .followRedirects(true)
                    .get();
            
            // Detect domain and apply specific extraction strategy
            String domain = extractDomain(url);
            String jdText = extractJDContent(doc, domain);
            
            if (jdText == null || jdText.trim().isEmpty()) {
                throw new IOException("No job description content found on the page");
            }
            
            log.info("Successfully scraped JD from {}, length: {} characters", domain, jdText.length());
            return cleanText(jdText);
            
        } catch (IOException e) {
            log.error("Failed to scrape JD from URL: {}", url, e);
            throw new IOException("Failed to fetch job description from URL: " + e.getMessage(), e);
        }
    }
    
    /**
     * Extract JD content based on domain-specific selectors
     */
    private String extractJDContent(Document doc, String domain) {
        StringBuilder content = new StringBuilder();
        
        // Try domain-specific selectors first
        if (domain.contains("linkedin")) {
            content.append(extractLinkedIn(doc));
        } else if (domain.contains("indeed")) {
            content.append(extractIndeed(doc));
        } else if (domain.contains("topcv")) {
            content.append(extractTopCV(doc));
        } else if (domain.contains("vietnamworks")) {
            content.append(extractVietnamWorks(doc));
        } else if (domain.contains("careerbuilder")) {
            content.append(extractCareerBuilder(doc));
        } else if (domain.contains("itviec")) {
            content.append(extractITviec(doc));
        }
        
        // If domain-specific extraction fails, try generic extraction
        if (content.length() < 100) {
            content.append(extractGeneric(doc));
        }
        
        return content.toString();
    }
    
    /**
     * LinkedIn specific extraction
     */
    private String extractLinkedIn(Document doc) {
        StringBuilder sb = new StringBuilder();
        
        // Job title
        Elements title = doc.select("h1.top-card-layout__title, h1.topcard__title");
        if (!title.isEmpty()) {
            sb.append("Position: ").append(title.text()).append("\n\n");
        }
        
        // Company
        Elements company = doc.select("a.topcard__org-name-link, span.topcard__flavor");
        if (!company.isEmpty()) {
            sb.append("Company: ").append(company.text()).append("\n\n");
        }
        
        // Job description
        Elements description = doc.select("div.show-more-less-html__markup, div.description__text");
        if (!description.isEmpty()) {
            sb.append(description.text());
        }
        
        return sb.toString();
    }
    
    /**
     * Indeed specific extraction
     */
    private String extractIndeed(Document doc) {
        StringBuilder sb = new StringBuilder();
        
        // Job title
        Elements title = doc.select("h1.jobsearch-JobInfoHeader-title");
        if (!title.isEmpty()) {
            sb.append("Position: ").append(title.text()).append("\n\n");
        }
        
        // Job description
        Elements description = doc.select("div#jobDescriptionText, div.jobsearch-jobDescriptionText");
        if (!description.isEmpty()) {
            sb.append(description.text());
        }
        
        return sb.toString();
    }
    
    /**
     * TopCV specific extraction
     */
    private String extractTopCV(Document doc) {
        StringBuilder sb = new StringBuilder();
        
        // Job title
        Elements title = doc.select("h1.job-detail__info--title");
        if (!title.isEmpty()) {
            sb.append("Position: ").append(title.text()).append("\n\n");
        }
        
        // Job description
        Elements description = doc.select("div.job-description, div.job-detail__information-detail");
        if (!description.isEmpty()) {
            sb.append(description.text());
        }
        
        return sb.toString();
    }
    
    /**
     * VietnamWorks specific extraction
     */
    private String extractVietnamWorks(Document doc) {
        StringBuilder sb = new StringBuilder();
        
        // Job title
        Elements title = doc.select("h1.job-title");
        if (!title.isEmpty()) {
            sb.append("Position: ").append(title.text()).append("\n\n");
        }
        
        // Job description
        Elements description = doc.select("div.job-description, div#job-description");
        if (!description.isEmpty()) {
            sb.append(description.text());
        }
        
        return sb.toString();
    }
    
    /**
     * CareerBuilder specific extraction
     */
    private String extractCareerBuilder(Document doc) {
        StringBuilder sb = new StringBuilder();
        
        // Job title
        Elements title = doc.select("h1.job-title");
        if (!title.isEmpty()) {
            sb.append("Position: ").append(title.text()).append("\n\n");
        }
        
        // Job description
        Elements description = doc.select("div.job-description-wrapper");
        if (!description.isEmpty()) {
            sb.append(description.text());
        }
        
        return sb.toString();
    }
    
    /**
     * ITviec specific extraction
     */
    private String extractITviec(Document doc) {
        StringBuilder sb = new StringBuilder();
        
        // Job title
        Elements title = doc.select("h1.job-title");
        if (!title.isEmpty()) {
            sb.append("Position: ").append(title.text()).append("\n\n");
        }
        
        // Job description
        Elements description = doc.select("div.job-description, div.job-description__content");
        if (!description.isEmpty()) {
            sb.append(description.text());
        }
        
        return sb.toString();
    }
    
    /**
     * Generic extraction for unknown domains
     */
    private String extractGeneric(Document doc) {
        StringBuilder sb = new StringBuilder();
        
        // Try to find job title
        Elements h1 = doc.select("h1");
        if (!h1.isEmpty()) {
            sb.append("Position: ").append(h1.first().text()).append("\n\n");
        }
        
        // Try common job description selectors
        String[] selectors = {
            "div.job-description",
            "div.description",
            "div[class*='job-desc']",
            "div[class*='description']",
            "section.description",
            "article.job-detail",
            "div#job-description",
            "div#description"
        };
        
        for (String selector : selectors) {
            Elements elements = doc.select(selector);
            if (!elements.isEmpty()) {
                sb.append(elements.text());
                break;
            }
        }
        
        // If still empty, try to get main content
        if (sb.length() < 100) {
            Elements main = doc.select("main, article, div.content, div.main-content");
            if (!main.isEmpty()) {
                sb.append(main.first().text());
            }
        }
        
        // Last resort: get body text (but filter out navigation, footer, etc.)
        if (sb.length() < 100) {
            // Remove unwanted elements
            doc.select("nav, header, footer, script, style").remove();
            sb.append(doc.body().text());
        }
        
        return sb.toString();
    }
    
    /**
     * Clean and format extracted text for better readability
     */
    private String cleanText(String text) {
        if (text == null) return "";
        
        // Step 1: Basic cleanup
        String cleaned = text
                // Normalize whitespace within lines
                .replaceAll("[ \\t]+", " ")
                // Remove excessive newlines (keep max 2)
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
        
        // Step 2: Remove duplicate sections (common in scraped content)
        String[] lines = cleaned.split("\\n");
        StringBuilder result = new StringBuilder();
        String previousLine = "";
        int duplicateCount = 0;
        
        for (String line : lines) {
            String trimmedLine = line.trim();
            
            // Skip if same as previous line (duplicate)
            if (trimmedLine.equals(previousLine)) {
                duplicateCount++;
                if (duplicateCount > 1) {
                    continue; // Skip excessive duplicates
                }
            } else {
                duplicateCount = 0;
            }
            
            previousLine = trimmedLine;
            
            // Skip very short lines that are likely noise
            if (trimmedLine.length() > 0) {
                result.append(trimmedLine).append("\n");
            }
        }
        
        // Step 3: Add section breaks for common JD sections
        String formatted = result.toString()
                // Add line break before common section headers
                .replaceAll("(?i)(Mô tả công việc|Job Description|Yêu cầu|Requirements?|Quyền lợi|Benefits?|Địa điểm|Location|Thời gian|Working Time)", "\n\n$1")
                // Add line break after bullet points
                .replaceAll("•", "\n• ")
                .replaceAll("-\\s+([A-Z])", "\n- $1")
                // Clean up extra spaces
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
        
        return formatted;
    }
    
    /**
     * Validate URL format
     */
    private void validateUrl(String urlString) throws MalformedURLException {
        if (urlString == null || urlString.trim().isEmpty()) {
            throw new MalformedURLException("URL cannot be empty");
        }
        
        // Ensure URL has protocol
        if (!urlString.startsWith("http://") && !urlString.startsWith("https://")) {
            throw new MalformedURLException("URL must start with http:// or https://");
        }
        
        // Validate URL format
        try {
            new URL(urlString);
        } catch (MalformedURLException e) {
            throw new MalformedURLException("Invalid URL format: " + e.getMessage());
        }
    }
    
    /**
     * Extract domain from URL
     */
    private String extractDomain(String urlString) {
        try {
            URL url = new URL(urlString);
            return url.getHost().toLowerCase();
        } catch (MalformedURLException e) {
            return "";
        }
    }
}

