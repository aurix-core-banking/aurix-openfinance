package com.aurix.platform.openfinance;

import com.aurix.platform.shared.crypto.PiiEncryptor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EntityScan(basePackages = "com.aurix.platform.openfinance")
@EnableJpaRepositories(basePackages = "com.aurix.platform.openfinance")
@EnableScheduling
// aurix-shared não está no pacote base do @SpringBootApplication (com.aurix.platform.openfinance),
// então componentes usados daqui precisam ser trazidos explicitamente via @Import.
@Import(PiiEncryptor.class)
public class OpenFinanceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OpenFinanceApplication.class, args);
    }
}
