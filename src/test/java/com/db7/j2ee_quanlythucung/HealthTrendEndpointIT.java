package com.db7.j2ee_quanlythucung;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Không dùng profile test (DataInit bị tắt khi profile=test).
 */
@SpringBootTest
@AutoConfigureMockMvc
class HealthTrendEndpointIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithUserDetails(value = "customer", userDetailsServiceBeanName = "customUserDetailsService")
    void trendEndpoint_returnsJson() throws Exception {
        mockMvc.perform(get("/health/pet/1/trend")
                        .param("type", "WEIGHT")
                        .param("days", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("WEIGHT"))
                .andExpect(jsonPath("$.trend").exists())
                .andExpect(jsonPath("$.chartLabels").isArray())
                .andExpect(jsonPath("$.chartValues").isArray())
                .andExpect(jsonPath("$.dataPoints").value(4));
    }
}
