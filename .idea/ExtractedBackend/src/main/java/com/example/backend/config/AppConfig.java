package com.example.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableMongoAuditing
public class AppConfig {

    /**
     * CORS configuration is handled centrally in SecurityConfiguration.
     * This ensures consistent CORS settings across all endpoints.
     */

    @Bean
    public MongoCustomConversions mongoCustomConversions() {
        List<Converter<?, ?>> converters = new ArrayList<>();
        return new MongoCustomConversions(converters);
    }
}
