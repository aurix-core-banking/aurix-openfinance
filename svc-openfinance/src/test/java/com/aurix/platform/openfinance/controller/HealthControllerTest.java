package com.aurix.platform.openfinance.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void deveRetornarHealthUp() throws Exception {
        mockMvc.perform(get("/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.service").value("svc-openfinance"))
            .andExpect(jsonPath("$.port").value(8096));
    }

    @Test
    void deveRetornarInfo() throws Exception {
        mockMvc.perform(get("/info"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.service").value("svc-openfinance"))
            .andExpect(jsonPath("$.version").value("1.0.0"));
    }
}
