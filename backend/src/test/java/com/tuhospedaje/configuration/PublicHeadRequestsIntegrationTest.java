package com.tuhospedaje.configuration;

import com.tuhospedaje.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PublicHeadRequestsIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void publicEndpointsSupportHeadRequestAnonymously() throws Exception {
        mockMvc.perform(head("/actuator/health/readiness"))
                .andExpect(status().isOk());

        mockMvc.perform(head("/api/categories"))
                .andExpect(status().isOk());

        mockMvc.perform(head("/api/features"))
                .andExpect(status().isOk());

        mockMvc.perform(head("/api/policies"))
                .andExpect(status().isOk());

        mockMvc.perform(head("/api/lodgings"))
                .andExpect(status().isOk());
    }
}
