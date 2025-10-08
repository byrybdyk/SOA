package org.lovesoa.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@ComponentScan(basePackages = "org.lovesoa")
@PropertySource("classpath:application.properties")
public class AppConfig {
    // Базовая конфигурация приложения
}