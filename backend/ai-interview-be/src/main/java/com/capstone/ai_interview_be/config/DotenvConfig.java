package com.capstone.ai_interview_be.config;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.io.File;

@Configuration
@Order(-1000) // Load sớm nhất
@Slf4j
public class DotenvConfig {
    
    static {
        // Load trong static block để chạy trước khi Spring khởi tạo
        loadEnvironmentVariables();
    }
    
    private static void loadEnvironmentVariables() {
        try {
            // Tìm file local.env ở root project (2 levels up)
            File currentDir = new File("").getAbsoluteFile();
            File projectRoot = currentDir.getParentFile().getParentFile();
            
            Dotenv dotenv = Dotenv.configure()
                    .directory(projectRoot.getAbsolutePath())
                    .filename("local.env")
                    .load();
            
            dotenv.entries().forEach(entry -> {
                System.setProperty(entry.getKey(), entry.getValue());
            });
            
        } catch (Exception e) {
            // Fallback: thử với absolute path
            try {
                Dotenv dotenv = Dotenv.configure()
                        .directory("D:/Projects/NCKH")
                        .filename("local.env")
                        .load();
                
                dotenv.entries().forEach(entry -> {
                    System.setProperty(entry.getKey(), entry.getValue());
                });
                
            } catch (Exception ex) {
                // Silent fail
            }
        }
    }
}