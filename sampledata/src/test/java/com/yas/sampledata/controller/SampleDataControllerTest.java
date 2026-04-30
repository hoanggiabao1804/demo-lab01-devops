package com.yas.sampledata.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yas.sampledata.service.SampleDataService;
import com.yas.sampledata.viewmodel.SampleDataVm;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SampleDataController.class)
class SampleDataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SampleDataService sampleDataService;

    @Test
    void shouldCreateSampleData() throws Exception {
        SampleDataVm response = new SampleDataVm("created");

        when(sampleDataService.createSampleData())
                .thenReturn(response);

        mockMvc.perform(post("/storefront/sampledata")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new SampleDataVm("input"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("created"));

        verify(sampleDataService).createSampleData();
    }
}