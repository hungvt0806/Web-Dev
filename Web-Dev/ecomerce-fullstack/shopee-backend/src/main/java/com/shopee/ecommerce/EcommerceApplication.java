package com.shopee.ecommerce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Shopee Ecommerce - Spring Boot Application Entry Point
 *
 * <p>Architecture: Clean Layered Architecture
 * <ul>
 *   <li>Controller  → HTTP interface, request/response DTOs</li>
 *   <li>Service     → Business logic, orchestration</li>
 *   <li>Repository  → Data access via Spring Data JPA</li>
 *   <li>Entity      → JPA domain models</li>
 *   <li>DTO         → Data Transfer Objects (request/response)</li>
 *   <li>Mapper      → MapStruct entity ↔ DTO conversion</li>
 * </ul>
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableAsync
@ConfigurationPropertiesScan
public class EcommerceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EcommerceApplication.class, args);
    }
}
