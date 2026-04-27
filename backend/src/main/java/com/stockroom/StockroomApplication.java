package com.stockroom;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.stockroom.config.AppProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class StockroomApplication {
    public static void main(String[] args) {
        SpringApplication.run(StockroomApplication.class, args);
    }
}
