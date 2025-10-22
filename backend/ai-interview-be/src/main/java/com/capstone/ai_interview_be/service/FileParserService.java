package com.capstone.ai_interview_be.service;

import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

@Service
@Slf4j
public class FileParserService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    // Phương thức chính để phân tích CV
    public String parseCV(MultipartFile file) throws IOException {
        validateFile(file);

        String filename = file.getOriginalFilename().toLowerCase();
        String contentType = file.getContentType();

        log.info("Parsing CV file: {} ({})", filename, contentType);

        if (isPDF(filename, contentType)) {
            return parsePDF(file);
        } else if (isDOCX(filename, contentType)) {
            return parseDOCX(file);
        }

        throw new IllegalArgumentException("Unsupported file format. Please upload PDF or DOCX files.");
    }

    // Kiểm tra định dạng PDF
    private boolean isPDF(String name, String type) {
        return (type != null && type.equals("application/pdf")) || name.endsWith(".pdf");
    }

    // Kiểm tra định dạng DOCX
    private boolean isDOCX(String name, String type) {
        return (type != null && type.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) || name.endsWith(".docx");
    }

    // Validate uploaded file
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("File cannot be null or empty");
        if (file.getOriginalFilename() == null) throw new IllegalArgumentException("File name cannot be null");
        if (file.getSize() > MAX_FILE_SIZE) throw new IllegalArgumentException("File size exceeds 10MB");
    }

    // Parse PDF files
    private String parsePDF(MultipartFile file) throws IOException {
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            String text = new PDFTextStripper().getText(document);
            if (text != null && text.trim().length() > 10) {
                log.debug("Extracted text directly from PDF ({} chars)", text.length());
                return text.trim();
            }
            log.info("PDF is image-based, using OCR...");
            return extractTextUsingOCR(document);
        } catch (Exception e) {
            throw new IOException("Failed to parse PDF: " + e.getMessage(), e);
        }
    }

    // trích xuất văn bản từ PDF hình ảnh sử dụng OCR
    private String extractTextUsingOCR(PDDocument document) throws IOException {
        Tesseract tesseract = setupTesseract();
        PDFRenderer renderer = new PDFRenderer(document);
        StringBuilder text = new StringBuilder();

        for (int i = 0; i < document.getNumberOfPages(); i++) {
            try {
                log.info("OCR page {}/{}", i + 1, document.getNumberOfPages());
                BufferedImage image = renderer.renderImageWithDPI(i, 200);
                text.append(tesseract.doOCR(image)).append("\n");
            } catch (TesseractException e) {
                log.warn("OCR failed on page {}: {}", i + 1, e.getMessage());
            }
        }

        String result = text.toString().trim();
        if (result.isEmpty()) {
            throw new IOException("No text extracted. Ensure Tesseract is installed and configured.");
        }
        return result;
    }


    // Cấu hình Tesseract với các đường dẫn tessdata phổ biến
    private Tesseract setupTesseract() {
        Tesseract tesseract = new Tesseract();
        tesseract.setLanguage("eng");

        String[] paths = {
                "C:\\Program Files\\Tesseract-OCR\\tessdata",
                System.getenv("TESSDATA_PREFIX"),
                "/usr/share/tesseract-ocr/5/tessdata",
                "/usr/share/tessdata",
                "/opt/homebrew/share/tessdata",
                "tessdata"
        };

        for (String path : paths) {
            if (path != null && new File(path).isDirectory()) {
                tesseract.setDatapath(path);
                log.info("Using tessdata path: {}", path);
                break;
            }
        }

        return tesseract;
    }


    // Parse DOCX files
    private String parseDOCX(MultipartFile file) throws IOException {
        try (XWPFDocument doc = new XWPFDocument(file.getInputStream());
             XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
            String text = extractor.getText().trim();
            log.debug("Extracted text from DOCX ({} chars)", text.length());
            return text;
        }
    }
}
