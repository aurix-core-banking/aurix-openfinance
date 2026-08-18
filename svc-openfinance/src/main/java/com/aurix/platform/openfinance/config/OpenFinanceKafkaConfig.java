package com.aurix.platform.openfinance.config;

import com.aurix.platform.shared.config.KafkaConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(KafkaConfig.class)
public class OpenFinanceKafkaConfig {
}
