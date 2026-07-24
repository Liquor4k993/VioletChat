package com.violetchat.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Для статики
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/");

        // Для загруженных файлов
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");

        // Для favicon
        registry.addResourceHandler("/favicon.ico")
                .addResourceLocations("classpath:/static/favicon.ico");
    }
}