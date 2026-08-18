package com.aurix.platform.openfinance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EntityScan(basePackages = {
    "com.aurix.platform.openfinance.entity"
})
@EnableJpaRepositories(basePackages = {
    "com.aurix.platform.openfinance.repository"
})
@EnableScheduling
public class OpenFinanceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OpenFinanceApplication.class, args);
    }
}
