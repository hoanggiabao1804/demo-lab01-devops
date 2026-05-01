package com.yas.inventory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.DuplicatedException;
import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.inventory.model.Warehouse;
import com.yas.inventory.model.enumeration.FilterExistInWhSelection;
import com.yas.inventory.repository.StockRepository;
import com.yas.inventory.repository.WarehouseRepository;
import com.yas.inventory.viewmodel.address.AddressDetailVm;
import com.yas.inventory.viewmodel.address.AddressPostVm;
import com.yas.inventory.viewmodel.address.AddressVm;
import com.yas.inventory.viewmodel.product.ProductInfoVm;
import com.yas.inventory.viewmodel.warehouse.WarehouseDetailVm;
import com.yas.inventory.viewmodel.warehouse.WarehouseGetVm;
import com.yas.inventory.viewmodel.warehouse.WarehouseListGetVm;
import com.yas.inventory.viewmodel.warehouse.WarehousePostVm;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
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
class WarehouseServiceTest {

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private StockRepository stockRepository;

    @Mock
    private ProductService productService;

    @Mock
    private LocationService locationService;

    @InjectMocks
    private WarehouseService warehouseService;

    private Warehouse warehouse;

    @BeforeEach
    void setUp() {
        warehouse = new Warehouse();
        warehouse.setId(1L);
        warehouse.setName("Warehouse 1");
        warehouse.setAddressId(10L);
    }

    @Nested
    class FindAllWarehouses {
        @Test
        void shouldReturnListOfWarehouseGetVm() {
            when(warehouseRepository.findAll()).thenReturn(List.of(warehouse));

            List<WarehouseGetVm> result = warehouseService.findAllWarehouses();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).id()).isEqualTo(1L);
            assertThat(result.get(0).name()).isEqualTo("Warehouse 1");
        }
    }

    @Nested
    class GetProductWarehouse {
        @Test
        void shouldReturnMappedProductInfoVm_WhenProductIdsIsNotEmpty() {
            Long warehouseId = 1L;
            List<Long> productIdsInWh = List.of(100L);
            ProductInfoVm productVm = new ProductInfoVm(100L, "Product A", "SKU-A", false);

            when(stockRepository.getProductIdsInWarehouse(warehouseId)).thenReturn(productIdsInWh);
            when(productService.filterProducts(null, null, productIdsInWh, FilterExistInWhSelection.ALL))
                    .thenReturn(List.of(productVm));

            List<ProductInfoVm> result = warehouseService.getProductWarehouse(warehouseId, null, null,
                    FilterExistInWhSelection.ALL);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).id()).isEqualTo(100L);
            assertThat(result.get(0).existInWh()).isTrue();
        }

        @Test
        void shouldReturnOriginalProductInfoVm_WhenProductIdsIsEmpty() {
            Long warehouseId = 1L;
            List<Long> productIdsInWh = Collections.emptyList();
            ProductInfoVm productVm = new ProductInfoVm(100L, "Product A", "SKU-A", false);

            when(stockRepository.getProductIdsInWarehouse(warehouseId)).thenReturn(productIdsInWh);
            when(productService.filterProducts(null, null, productIdsInWh, FilterExistInWhSelection.ALL))
                    .thenReturn(List.of(productVm));

            List<ProductInfoVm> result = warehouseService.getProductWarehouse(warehouseId, null, null,
                    FilterExistInWhSelection.ALL);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).existInWh()).isFalse();
        }
    }

    @Nested
    class FindById {
        @Test
        void shouldThrowNotFoundException_WhenWarehouseDoesNotExist() {
            when(warehouseRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> warehouseService.findById(1L))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("is not found");
        }

        @Test
        void shouldReturnWarehouseDetailVm_WhenWarehouseExists() {
            when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
            AddressDetailVm addressDetailVm = new AddressDetailVm(10L, "Contact Name", "123456", "Line 1", "Line 2",
                    "City", "Zip", 1L, "District", 1L, "State", 1L, "Country");
            when(locationService.getAddressById(10L)).thenReturn(addressDetailVm);

            WarehouseDetailVm result = warehouseService.findById(1L);

            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.name()).isEqualTo("Warehouse 1");
            assertThat(result.contactName()).isEqualTo("Contact Name");
        }
    }

    @Nested
    class Create {
        private WarehousePostVm postVm;

        @BeforeEach
        void setUp() {
            postVm = WarehousePostVm.builder()
                    .id(null)
                    .name("Warehouse New")
                    .contactName("Contact")
                    .phone("Phone")
                    .addressLine1("Line 1")
                    .addressLine2("Line 2")
                    .city("City")
                    .zipCode("Zip")
                    .districtId(1L)
                    .stateOrProvinceId(1L)
                    .countryId(1L)
                    .build();
        }

        @Test
        void shouldThrowDuplicatedException_WhenNameAlreadyExists() {
            when(warehouseRepository.existsByName(postVm.name())).thenReturn(true);

            assertThatThrownBy(() -> warehouseService.create(postVm))
                    .isInstanceOf(DuplicatedException.class)
                    .hasMessageContaining("is already existed");
        }

        @Test
        void shouldCreateAndReturnWarehouse() {
            when(warehouseRepository.existsByName(postVm.name())).thenReturn(false);

            AddressVm addressVm = new AddressVm(99L, "Contact", "Phone", "Line 1", "City", "Zip", 1L, 1L, 1L);
            when(locationService.createAddress(any(AddressPostVm.class))).thenReturn(addressVm);

            when(warehouseRepository.save(any(Warehouse.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Warehouse result = warehouseService.create(postVm);

            assertThat(result.getName()).isEqualTo(postVm.name());
            assertThat(result.getAddressId()).isEqualTo(99L);

            ArgumentCaptor<AddressPostVm> addressCaptor = ArgumentCaptor.forClass(AddressPostVm.class);
            verify(locationService).createAddress(addressCaptor.capture());
            assertThat(addressCaptor.getValue().contactName()).isEqualTo("Contact");
        }
    }

    @Nested
    class Update {
        private WarehousePostVm postVm;

        @BeforeEach
        void setUp() {
            postVm = WarehousePostVm.builder()
                    .id("some-id")
                    .name("Warehouse Updated")
                    .contactName("Contact")
                    .phone("Phone")
                    .addressLine1("Line 1")
                    .addressLine2("Line 2")
                    .city("City")
                    .zipCode("Zip")
                    .districtId(1L)
                    .stateOrProvinceId(1L)
                    .countryId(1L)
                    .build();
        }

        @Test
        void shouldThrowNotFoundException_WhenWarehouseDoesNotExist() {
            when(warehouseRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> warehouseService.update(postVm, 1L))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("is not found");
        }

        @Test
        void shouldThrowDuplicatedException_WhenNameExistsWithDifferentId() {
            when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
            when(warehouseRepository.existsByNameWithDifferentId(postVm.name(), 1L)).thenReturn(true);

            assertThatThrownBy(() -> warehouseService.update(postVm, 1L))
                    .isInstanceOf(DuplicatedException.class)
                    .hasMessageContaining("is already existed");
        }

        @Test
        void shouldUpdateWarehouseAndAddress_WhenValid() {
            when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
            when(warehouseRepository.existsByNameWithDifferentId(postVm.name(), 1L)).thenReturn(false);

            warehouseService.update(postVm, 1L);

            assertThat(warehouse.getName()).isEqualTo("Warehouse Updated");

            ArgumentCaptor<AddressPostVm> addressCaptor = ArgumentCaptor.forClass(AddressPostVm.class);
            verify(locationService).updateAddress(eq(10L), addressCaptor.capture());
            assertThat(addressCaptor.getValue().contactName()).isEqualTo("Contact");

            verify(warehouseRepository).save(warehouse);
        }
    }

    @Nested
    class Delete {
        @Test
        void shouldThrowNotFoundException_WhenWarehouseDoesNotExist() {
            when(warehouseRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> warehouseService.delete(1L))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        void shouldDeleteWarehouseAndAddress_WhenExists() {
            when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));

            warehouseService.delete(1L);

            verify(warehouseRepository).deleteById(1L);
            verify(locationService).deleteAddress(10L);
        }
    }

    @Nested
    class GetPageableWarehouses {
        @Test
        void shouldReturnWarehouseListGetVm() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Warehouse> page = new PageImpl<>(List.of(warehouse), pageable, 1);
            when(warehouseRepository.findAll(pageable)).thenReturn(page);

            WarehouseListGetVm result = warehouseService.getPageableWarehouses(0, 10);

            assertThat(result.warehouseContent()).hasSize(1);
            assertThat(result.warehouseContent().get(0).id()).isEqualTo(1L);
            assertThat(result.pageNo()).isEqualTo(0);
            assertThat(result.pageSize()).isEqualTo(10);
            assertThat(result.totalElements()).isEqualTo(1);
            assertThat(result.totalPages()).isEqualTo(1);
            assertThat(result.isLast()).isTrue();
        }
    }
}