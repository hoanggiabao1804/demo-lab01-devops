package com.yas.tax.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.tax.model.TaxClass;
import com.yas.tax.model.TaxRate;
import com.yas.tax.service.TaxRateService;
import com.yas.tax.viewmodel.taxrate.TaxRateListGetVm;
import com.yas.tax.viewmodel.taxrate.TaxRatePostVm;
import com.yas.tax.viewmodel.taxrate.TaxRateVm;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

@ExtendWith(MockitoExtension.class)
class TaxRateControllerTest {

    @Mock
    private TaxRateService taxRateService;

    @InjectMocks
    private TaxRateController taxRateController;

    private TaxRatePostVm taxRatePostVm;
    private TaxRateVm taxRateVm;
    private TaxRate taxRate;

    @BeforeEach
    void setUp() {
        taxRatePostVm = new TaxRatePostVm(10.0, "12345", 1L, 2L, 3L);
        taxRateVm = new TaxRateVm(1L, 10.0, "12345", 1L, 2L, 3L);

        TaxClass taxClass = new TaxClass();
        taxClass.setId(1L);

        taxRate = TaxRate.builder()
                .id(1L)
                .rate(10.0)
                .zipCode("12345")
                .taxClass(taxClass)
                .stateOrProvinceId(2L)
                .countryId(3L)
                .build();
    }

    @Test
    void getPageableTaxRates_ShouldReturnOkAndTaxRateListGetVm() {
        // GIVEN
        TaxRateListGetVm expectedList = new TaxRateListGetVm(List.of(), 0, 10, 0, 0, true);
        when(taxRateService.getPageableTaxRates(0, 10)).thenReturn(expectedList);

        // WHEN
        ResponseEntity<TaxRateListGetVm> response = taxRateController.getPageableTaxRates(0, 10);

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expectedList);
    }

    @Test
    void getTaxRate_ShouldReturnOkAndTaxRateVm() {
        // GIVEN
        when(taxRateService.findById(1L)).thenReturn(taxRateVm);

        // WHEN
        ResponseEntity<TaxRateVm> response = taxRateController.getTaxRate(1L);

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(taxRateVm);
    }

    @Test
    void createTaxRate_ShouldReturnCreatedAndTaxRateVm() {
        // GIVEN
        when(taxRateService.createTaxRate(taxRatePostVm)).thenReturn(taxRate);
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.newInstance();

        // WHEN
        ResponseEntity<TaxRateVm> response = taxRateController.createTaxRate(taxRatePostVm, uriBuilder);

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Xác minh URI Location header được trả về chính xác
        URI location = response.getHeaders().getLocation();
        assertThat(location).isNotNull();
        assertThat(location.getPath()).isEqualTo("/tax-rates/1");

        // Xác minh body được map từ model
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(1L);
        assertThat(response.getBody().rate()).isEqualTo(10.0);
    }

    @Test
    void updateTaxRate_ShouldReturnNoContent() {
        // GIVEN
        doNothing().when(taxRateService).updateTaxRate(taxRatePostVm, 1L);

        // WHEN
        ResponseEntity<Void> response = taxRateController.updateTaxRate(1L, taxRatePostVm);

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(taxRateService).updateTaxRate(taxRatePostVm, 1L);
    }

    @Test
    void deleteTaxRate_ShouldReturnNoContent() {
        // GIVEN
        doNothing().when(taxRateService).delete(1L);

        // WHEN
        ResponseEntity<Void> response = taxRateController.deleteTaxRate(1L);

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(taxRateService).delete(1L);
    }

    @Test
    void getTaxPercentByAddress_ShouldReturnOkAndPercentValue() {
        // GIVEN
        Double expectedPercent = 8.5;
        when(taxRateService.getTaxPercent(1L, 3L, 2L, "12345")).thenReturn(expectedPercent);

        // WHEN
        ResponseEntity<Double> response = taxRateController.getTaxPercentByAddress(1L, 3L, 2L, "12345");

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expectedPercent);
    }

    @Test
    void getBatchTaxPercentsByAddress_ShouldReturnOkAndListOfTaxRateVm() {
        // GIVEN
        List<Long> taxClassIds = List.of(1L, 2L);
        List<TaxRateVm> expectedList = List.of(taxRateVm);
        when(taxRateService.getBulkTaxRate(taxClassIds, 3L, 2L, "12345")).thenReturn(expectedList);

        // WHEN
        ResponseEntity<List<TaxRateVm>> response = taxRateController.getBatchTaxPercentsByAddress(taxClassIds, 3L, 2L,
                "12345");

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expectedList);
    }
}