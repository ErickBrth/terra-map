package com.terramap.config;

import com.terramap.domain.service.GeometryValidator;
import com.terramap.domain.service.JtsGeometryValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration that exposes domain services as beans,
 * preserving domain purity (no Spring annotations in domain package).
 */
@Configuration
public class DomainConfig {

    @Bean
    public GeometryValidator geometryValidator() {
        return new JtsGeometryValidator();
    }
}
