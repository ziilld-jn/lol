package com.example.lucky4.config;

import com.example.lucky4.service.PhotoStorage;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Makes the files in data/uploads/ available in the browser at /uploads/... */
@Configuration
public class UploadsConfig implements WebMvcConfigurer {

    private final PhotoStorage photoStorage;

    public UploadsConfig(PhotoStorage photoStorage) {
        this.photoStorage = photoStorage;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = photoStorage.getDirectory().toUri().toString();
        if (!location.endsWith("/")) {
            location += "/";
        }
        registry.addResourceHandler("/uploads/**").addResourceLocations(location);
    }
}