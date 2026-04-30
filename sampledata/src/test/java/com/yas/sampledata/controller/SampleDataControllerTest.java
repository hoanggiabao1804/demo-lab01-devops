package com.yas.sampledata.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yas.sampledata.service.SampleDataService;
import com.yas.sampledata.viewmodel.SampleDataVm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SampleDataControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SampleDataService sampleDataService;

    @Autowired
    private ObjectMapper objectMapper;

    private SampleDataVm sampleDataVm;

    @BeforeEach
    void setUp() {
        sampleDataVm = new SampleDataVm("Insert Sample Data successfully!");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreateSampleDataSuccess() throws Exception {
        // Arrange
        when(sampleDataService.createSampleData()).thenReturn(sampleDataVm);

        // Act & Assert
        mockMvc.perform(post("/storefront/sampledata")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleDataVm)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Insert Sample Data successfully!"));
    }

    @Test
    void testCreateSampleDataUnauthorized() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/storefront/sampledata")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleDataVm)))
                .andExpect(status().isUnauthorized());
    }
}
