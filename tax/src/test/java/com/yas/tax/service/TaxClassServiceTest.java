package com.yas.tax.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.DuplicatedException;
import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.tax.constants.MessageCode;
import com.yas.tax.model.TaxClass;
import com.yas.tax.repository.TaxClassRepository;
import com.yas.tax.viewmodel.taxclass.TaxClassListGetVm;
import com.yas.tax.viewmodel.taxclass.TaxClassPostVm;
import com.yas.tax.viewmodel.taxclass.TaxClassVm;
import java.util.List;
import java.util.Optional;
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
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class TaxClassServiceTest {

    @Mock
    private TaxClassRepository taxClassRepository;

    @InjectMocks
    private TaxClassService taxClassService;

    private TaxClass taxClass;
    private TaxClassPostVm taxClassPostVm;

    @BeforeEach
    void setUp() {
        taxClass = TaxClass.builder()
                .id(1L)
                .name("Standard Tax")
                .build();

        // Trong record TaxClassPostVm, trường thứ nhất là id (String), trường thứ hai
        // là name
        taxClassPostVm = new TaxClassPostVm("1", "Standard Tax");
    }

    @Test
    void findAllTaxClasses_ShouldReturnListOfTaxClassVm_SortedByName() {
        when(taxClassRepository.findAll(any(Sort.class))).thenReturn(List.of(taxClass));

        List<TaxClassVm> result = taxClassService.findAllTaxClasses();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(1L);
        assertThat(result.get(0).name()).isEqualTo("Standard Tax");

        // Xác minh rằng có gọi findAll với tham số Sort.by("name")
        ArgumentCaptor<Sort> sortCaptor = ArgumentCaptor.forClass(Sort.class);
        verify(taxClassRepository).findAll(sortCaptor.capture());
        Sort sort = sortCaptor.getValue();
        assertThat(sort.getOrderFor("name")).isNotNull();
        assertThat(sort.getOrderFor("name").getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void findById_WhenTaxClassExists_ShouldReturnTaxClassVm() {
        when(taxClassRepository.findById(1L)).thenReturn(Optional.of(taxClass));

        TaxClassVm result = taxClassService.findById(1L);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("Standard Tax");
    }

    @Test
    void findById_WhenTaxClassDoesNotExist_ShouldThrowNotFoundException() {
        when(taxClassRepository.findById(1L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class, () -> taxClassService.findById(1L));

        assertThat(exception.getMessage()).isEqualTo(MessageCode.TAX_CLASS_NOT_FOUND);
    }

    @Test
    void create_WhenNameIsUnique_ShouldReturnSavedTaxClass() {
        when(taxClassRepository.existsByName("Standard Tax")).thenReturn(false);
        when(taxClassRepository.save(any(TaxClass.class))).thenReturn(taxClass);

        TaxClass result = taxClassService.create(taxClassPostVm);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Standard Tax");
        verify(taxClassRepository).save(any(TaxClass.class));
    }

    @Test
    void create_WhenNameExists_ShouldThrowDuplicatedException() {
        when(taxClassRepository.existsByName("Standard Tax")).thenReturn(true);

        DuplicatedException exception = assertThrows(DuplicatedException.class,
                () -> taxClassService.create(taxClassPostVm));

        assertThat(exception.getMessage()).isEqualTo(MessageCode.NAME_ALREADY_EXITED);
        verify(taxClassRepository, never()).save(any(TaxClass.class));
    }

    @Test
    void update_WhenValidInput_ShouldUpdateSuccessfully() {
        when(taxClassRepository.findById(1L)).thenReturn(Optional.of(taxClass));
        when(taxClassRepository.existsByNameNotUpdatingTaxClass("Standard Tax", 1L)).thenReturn(false);

        // Thực hiện lệnh update
        taxClassService.update(taxClassPostVm, 1L);

        // Xác minh TaxClass đã được lưu với thông tin mới
        ArgumentCaptor<TaxClass> captor = ArgumentCaptor.forClass(TaxClass.class);
        verify(taxClassRepository).save(captor.capture());

        TaxClass savedTaxClass = captor.getValue();
        assertThat(savedTaxClass.getName()).isEqualTo("Standard Tax");
    }

    @Test
    void update_WhenTaxClassDoesNotExist_ShouldThrowNotFoundException() {
        when(taxClassRepository.findById(1L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> taxClassService.update(taxClassPostVm, 1L));

        assertThat(exception.getMessage()).isEqualTo(MessageCode.TAX_CLASS_NOT_FOUND);
        verify(taxClassRepository, never()).save(any(TaxClass.class));
    }

    @Test
    void update_WhenNameExistsForOtherId_ShouldThrowDuplicatedException() {
        when(taxClassRepository.findById(1L)).thenReturn(Optional.of(taxClass));
        when(taxClassRepository.existsByNameNotUpdatingTaxClass("Standard Tax", 1L)).thenReturn(true);

        DuplicatedException exception = assertThrows(DuplicatedException.class,
                () -> taxClassService.update(taxClassPostVm, 1L));

        assertThat(exception.getMessage()).isEqualTo(MessageCode.NAME_ALREADY_EXITED);
        verify(taxClassRepository, never()).save(any(TaxClass.class));
    }

    @Test
    void delete_WhenTaxClassExists_ShouldDeleteSuccessfully() {
        when(taxClassRepository.existsById(1L)).thenReturn(true);
        doNothing().when(taxClassRepository).deleteById(1L);

        taxClassService.delete(1L);

        verify(taxClassRepository).deleteById(1L);
    }

    @Test
    void delete_WhenTaxClassDoesNotExist_ShouldThrowNotFoundException() {
        when(taxClassRepository.existsById(1L)).thenReturn(false);

        NotFoundException exception = assertThrows(NotFoundException.class, () -> taxClassService.delete(1L));

        assertThat(exception.getMessage()).isEqualTo(MessageCode.TAX_CLASS_NOT_FOUND);
        verify(taxClassRepository, never()).deleteById(any());
    }

    @Test
    void getPageableTaxClasses_ShouldReturnTaxClassListGetVm() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<TaxClass> taxClassPage = new PageImpl<>(List.of(taxClass), pageable, 1);
        when(taxClassRepository.findAll(pageable)).thenReturn(taxClassPage);

        TaxClassListGetVm result = taxClassService.getPageableTaxClasses(0, 10);

        assertThat(result).isNotNull();
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.totalPages()).isEqualTo(1);
        assertThat(result.pageNo()).isEqualTo(0);
        assertThat(result.pageSize()).isEqualTo(10);
        assertThat(result.isLast()).isTrue();
        assertThat(result.taxClassContent()).hasSize(1);
        assertThat(result.taxClassContent().get(0).name()).isEqualTo("Standard Tax");
    }
}