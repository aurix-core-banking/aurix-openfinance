package com.aurix.platform.openfinance.config;

import com.aurix.platform.openfinance.trust.filter.DpopFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class OpenFinanceSecurityConfig {

    private final DpopFilter dpopFilter;

    public OpenFinanceSecurityConfig(DpopFilter dpopFilter) {
        this.dpopFilter = dpopFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/health", "/info").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**", "/swagger-resources/**").permitAll()
                .requestMatchers("/open-finance/**").permitAll()
                .anyRequest().permitAll()
            )
            .addFilterBefore(dpopFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
