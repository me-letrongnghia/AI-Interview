package com.capstone.ai_interview_be.config;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.io.File;

@Configuration
@Order(-1000) 
@Slf4j
public class DotenvConfig {

    static {
        loadEnvironmentVariables();
    }
    private static void loadEnvironmentVariables() {
        try {
            File envFile = findEnvFile();
            
            if (envFile != null) {
                Dotenv dotenv = Dotenv.configure()
                        .directory(envFile.getParent())
                        .filename("local.env")
                        .load();
                dotenv.entries().forEach(entry -> {
                    System.setProperty(entry.getKey(), entry.getValue());
                });
                
                log.info("Environment variables loaded from: {}", envFile.getAbsolutePath());
            } else {
                log.warn("local.env file not found");
            }
        } catch (Exception e) {
            log.error("Failed to load environment variables", e);
        }
    }
    private static File findEnvFile() {
        File currentDir = new File("").getAbsoluteFile();
        while (currentDir != null) {
            File envFile = new File(currentDir, "local.env");
            if (envFile.exists() && envFile.isFile()) {
                return envFile;
            }
            currentDir = currentDir.getParentFile();
        }
        return null;
    }
}