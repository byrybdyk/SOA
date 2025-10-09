package org.lovesoa.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@ComponentScan(basePackages = "org.lovesoa")
@EnableJpaRepositories(
        basePackages = "org.lovesoa.repository",
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = "org\\.springframework\\.data\\.querydsl.*"
        )
)
@PropertySource("classpath:application.properties")
public class AppConfig {
}