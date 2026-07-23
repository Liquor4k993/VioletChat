package com.violetchat.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

/**
 * Конфигурация CORS (Cross-Origin Resource Sharing).
 * Разрешает кросс-доменные запросы для фронтенда.
 */
@Configuration
public class CorsConfig {

    /**
     * Создаёт фильтр CORS с настройками для всех источников.
     *
     * @return настроенный CorsFilter
     */
    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        // Разрешаем все источники (для разработки)
        config.setAllowCredentials(true);
        config.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://localhost:8080",
                "http://127.0.0.1:5500",
                "http://127.0.0.1:63342"
        ));

        // Разрешаем все методы
        config.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));

        // Разрешаем все заголовки
        config.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "Accept",
                "Origin",
                "X-Requested-With",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers"
        ));

        // Разрешаем все заголовки для ответа
        config.setExposedHeaders(List.of(
                "Authorization",
                "Content-Type"
        ));

        // Кэшируем preflight запросы на 1 час
        config.setMaxAge(3600L);

        // Применяем настройки ко всем маршрутам
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}