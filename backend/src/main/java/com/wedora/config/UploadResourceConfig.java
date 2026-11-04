package com.wedora.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class UploadResourceConfig implements WebMvcConfigurer {
    private final String location;

    public UploadResourceConfig(@Value("${app.uploads-dir}") String uploadsDirectory) throws IOException {
        Path directory = Paths.get(uploadsDirectory).toAbsolutePath().normalize();
        Files.createDirectories(directory);
        this.location = directory.toUri().toString();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**").addResourceLocations(location);
    }
}
