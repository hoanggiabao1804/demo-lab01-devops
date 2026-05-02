package com.yas.tax.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.tax.constants.MessageCode;
import com.yas.tax.model.TaxClass;
import com.yas.tax.model.TaxRate;
import com.yas.tax.repository.TaxClassRepository;
import com.yas.tax.repository.TaxRateRepository;
import com.yas.tax.viewmodel.location.StateOrProvinceAndCountryGetNameVm;
import com.yas.tax.viewmodel.taxrate.TaxRateListGetVm;
import com.yas.tax.viewmodel.taxrate.TaxRatePostVm;
import com.yas.tax.viewmodel.taxrate.TaxRateVm;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class TaxRateServiceTest {

    @Mock
    private TaxRateRepository taxRateRepository;

    @Mock
    private TaxClassRepository taxClassRepository;

    @Mock
    private LocationService locationService;

    @InjectMocks
    private TaxRateService taxRateService;

    private TaxRate taxRate;
    private TaxClass taxClass;
    private TaxRatePostVm taxRatePostVm;

    @BeforeEach
    void setUp() {
        taxClass = new TaxClass();
        taxClass.setId(1L);
        taxClass.setName("Standard Tax");

        taxRate = TaxRate.builder()
                .id(1L)
                .rate(10.0)
                .zipCode("12345")
                .taxClass(taxClass)
                .stateOrProvinceId(2L)
                .countryId(3L)
                .build();

        taxRatePostVm = new TaxRatePostVm(10.0, "12345", 1L, 2L, 3L);
    }

    @Test
    void createTaxRate_WhenTaxClassExists_ShouldReturnSavedTaxRate() {
        when(taxClassRepository.existsById(1L)).thenReturn(true);
        when(taxClassRepository.getReferenceById(1L)).thenReturn(taxClass);
        when(taxRateRepository.save(any(TaxRate.class))).thenReturn(taxRate);

        TaxRate result = taxRateService.createTaxRate(taxRatePostVm);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(taxRateRepository).save(any(TaxRate.class));
    }

    @Test
    void createTaxRate_WhenTaxClassDoesNotExist_ShouldThrowNotFoundException() {
        when(taxClassRepository.existsById(1L)).thenReturn(false);

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> taxRateService.createTaxRate(taxRatePostVm));

        assertThat(exception.getMessage()).isEqualTo(MessageCode.TAX_CLASS_NOT_FOUND);
        verify(taxRateRepository, never()).save(any(TaxRate.class));
    }

    @Test
    void updateTaxRate_WhenTaxRateAndTaxClassExist_ShouldUpdateSuccessfully() {
        when(taxRateRepository.findById(1L)).thenReturn(Optional.of(taxRate));
        when(taxClassRepository.existsById(1L)).thenReturn(true);
        when(taxClassRepository.getReferenceById(1L)).thenReturn(taxClass);
        when(taxRateRepository.save(any(TaxRate.class))).thenReturn(taxRate);

        taxRateService.updateTaxRate(taxRatePostVm, 1L);

        ArgumentCaptor<TaxRate> captor = ArgumentCaptor.forClass(TaxRate.class);
        verify(taxRateRepository).save(captor.capture());
        TaxRate savedTaxRate = captor.getValue();

        assertThat(savedTaxRate.getRate()).isEqualTo(10.0);
        assertThat(savedTaxRate.getZipCode()).isEqualTo("12345");
        assertThat(savedTaxRate.getTaxClass()).isEqualTo(taxClass);
        assertThat(savedTaxRate.getStateOrProvinceId()).isEqualTo(2L);
        assertThat(savedTaxRate.getCountryId()).isEqualTo(3L);
    }

    @Test
    void updateTaxRate_WhenTaxRateDoesNotExist_ShouldThrowNotFoundException() {
        when(taxRateRepository.findById(1L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> taxRateService.updateTaxRate(taxRatePostVm, 1L));

        assertThat(exception.getMessage()).isEqualTo(MessageCode.TAX_RATE_NOT_FOUND);
        verify(taxRateRepository, never()).save(any(TaxRate.class));
    }

    @Test
    void updateTaxRate_WhenTaxClassDoesNotExist_ShouldThrowNotFoundException() {
        when(taxRateRepository.findById(1L)).thenReturn(Optional.of(taxRate));
        when(taxClassRepository.existsById(1L)).thenReturn(false);

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> taxRateService.updateTaxRate(taxRatePostVm, 1L));

        assertThat(exception.getMessage()).isEqualTo(MessageCode.TAX_CLASS_NOT_FOUND);
        verify(taxRateRepository, never()).save(any(TaxRate.class));
    }

    @Test
    void delete_WhenTaxRateExists_ShouldDeleteSuccessfully() {
        when(taxRateRepository.existsById(1L)).thenReturn(true);
        doNothing().when(taxRateRepository).deleteById(1L);

        taxRateService.delete(1L);

        verify(taxRateRepository).deleteById(1L);
    }

    @Test
    void delete_WhenTaxRateDoesNotExist_ShouldThrowNotFoundException() {
        when(taxRateRepository.existsById(1L)).thenReturn(false);

        NotFoundException exception = assertThrows(NotFoundException.class, () -> taxRateService.delete(1L));

        assertThat(exception.getMessage()).isEqualTo(MessageCode.TAX_RATE_NOT_FOUND);
        verify(taxRateRepository, never()).deleteById(any());
    }

    @Test
    void findById_WhenTaxRateExists_ShouldReturnTaxRateVm() {
        when(taxRateRepository.findById(1L)).thenReturn(Optional.of(taxRate));

        TaxRateVm result = taxRateService.findById(1L);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.rate()).isEqualTo(10.0);
    }

    @Test
    void findById_WhenTaxRateDoesNotExist_ShouldThrowNotFoundException() {
        when(taxRateRepository.findById(1L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class, () -> taxRateService.findById(1L));

        assertThat(exception.getMessage()).isEqualTo(MessageCode.TAX_RATE_NOT_FOUND);
    }

    @Test
    void findAll_ShouldReturnListOfTaxRateVm() {
        when(taxRateRepository.findAll()).thenReturn(List.of(taxRate));

        List<TaxRateVm> result = taxRateService.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(1L);
    }

    @Test
    void getPageableTaxRates_WithContent_ShouldReturnTaxRateListGetVm() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<TaxRate> taxRatePage = new PageImpl<>(List.of(taxRate), pageable, 1);
        when(taxRateRepository.findAll(pageable)).thenReturn(taxRatePage);

        StateOrProvinceAndCountryGetNameVm locationVm = new StateOrProvinceAndCountryGetNameVm(2L, "California", "US");
        when(locationService.getStateOrProvinceAndCountryNames(anyList())).thenReturn(List.of(locationVm));

        TaxRateListGetVm result = taxRateService.getPageableTaxRates(0, 10);

        assertThat(result).isNotNull();
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.taxRateGetDetailContent()).hasSize(1);
        assertThat(result.taxRateGetDetailContent().get(0).stateOrProvinceName()).isEqualTo("California");
        assertThat(result.taxRateGetDetailContent().get(0).countryName()).isEqualTo("US");
    }

    @Test
    void getPageableTaxRates_WithEmptyContent_ShouldReturnEmptyListGetVm() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<TaxRate> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
        when(taxRateRepository.findAll(pageable)).thenReturn(emptyPage);

        TaxRateListGetVm result = taxRateService.getPageableTaxRates(0, 10);

        assertThat(result).isNotNull();
        assertThat(result.totalElements()).isEqualTo(0);
        assertThat(result.taxRateGetDetailContent()).isEmpty();
        verify(locationService, never()).getStateOrProvinceAndCountryNames(anyList());
    }

    @Test
    void getTaxPercent_WhenFound_ShouldReturnPercentValue() {
        when(taxRateRepository.getTaxPercent(3L, 2L, "12345", 1L)).thenReturn(8.5);

        double result = taxRateService.getTaxPercent(1L, 3L, 2L, "12345");

        assertThat(result).isEqualTo(8.5);
    }

    @Test
    void getTaxPercent_WhenNotFound_ShouldReturnZero() {
        when(taxRateRepository.getTaxPercent(3L, 2L, "12345", 1L)).thenReturn(null);

        double result = taxRateService.getTaxPercent(1L, 3L, 2L, "12345");

        assertThat(result).isEqualTo(0.0);
    }

    @Test
    void getBulkTaxRate_ShouldReturnListOfTaxRateVm() {
        when(taxRateRepository.getBatchTaxRates(eq(3L), eq(2L), eq("12345"), any(Set.class)))
                .thenReturn(List.of(taxRate));

        List<TaxRateVm> result = taxRateService.getBulkTaxRate(List.of(1L), 3L, 2L, "12345");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(1L);
    }
}