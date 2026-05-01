package com.yas.inventory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.BadRequestException;
import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.commonlibrary.exception.StockExistingException;
import com.yas.inventory.model.Stock;
import com.yas.inventory.model.Warehouse;
import com.yas.inventory.model.enumeration.FilterExistInWhSelection;
import com.yas.inventory.repository.StockRepository;
import com.yas.inventory.repository.WarehouseRepository;
import com.yas.inventory.viewmodel.product.ProductInfoVm;
import com.yas.inventory.viewmodel.product.ProductQuantityPostVm;
import com.yas.inventory.viewmodel.stock.StockPostVm;
import com.yas.inventory.viewmodel.stock.StockQuantityUpdateVm;
import com.yas.inventory.viewmodel.stock.StockQuantityVm;
import com.yas.inventory.viewmodel.stock.StockVm;
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

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private StockRepository stockRepository;

    @Mock
    private ProductService productService;

    @Mock
    private WarehouseService warehouseService;

    @Mock
    private StockHistoryService stockHistoryService;

    @InjectMocks
    private StockService stockService;

    private Warehouse warehouse;

    @BeforeEach
    void setUp() {
        warehouse = new Warehouse();
        warehouse.setId(1L);
        warehouse.setName("Main Warehouse");
    }

    @Nested
    class AddProductIntoWarehouse {

        @Test
        void shouldThrowStockExistingException_WhenStockAlreadyExists() {
            StockPostVm postVm = new StockPostVm(100L, 1L);
            when(stockRepository.existsByWarehouseIdAndProductId(1L, 100L)).thenReturn(true);

            assertThatThrownBy(() -> stockService.addProductIntoWarehouse(List.of(postVm)))
                    .isInstanceOf(StockExistingException.class)
                    .hasMessageContaining("already existing");
        }

        @Test
        void shouldThrowNotFoundException_WhenProductDoesNotExist() {
            StockPostVm postVm = new StockPostVm(100L, 1L);
            when(stockRepository.existsByWarehouseIdAndProductId(1L, 100L)).thenReturn(false);
            when(productService.getProduct(100L)).thenReturn(null);

            assertThatThrownBy(() -> stockService.addProductIntoWarehouse(List.of(postVm)))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("is not found");
        }

        @Test
        void shouldThrowNotFoundException_WhenWarehouseDoesNotExist() {
            StockPostVm postVm = new StockPostVm(100L, 1L);
            when(stockRepository.existsByWarehouseIdAndProductId(1L, 100L)).thenReturn(false);
            when(productService.getProduct(100L)).thenReturn(new ProductInfoVm(100L, "Product A", "SKU-A", true));
            when(warehouseRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> stockService.addProductIntoWarehouse(List.of(postVm)))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("is not found");
        }

        @Test
        void shouldSaveStock_WhenDataIsValid() {
            StockPostVm postVm = new StockPostVm(100L, 1L);

            when(stockRepository.existsByWarehouseIdAndProductId(1L, 100L)).thenReturn(false);
            when(productService.getProduct(100L)).thenReturn(new ProductInfoVm(100L, "Product A", "SKU-A", true));
            when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));

            stockService.addProductIntoWarehouse(List.of(postVm));

            ArgumentCaptor<List<Stock>> captor = ArgumentCaptor.forClass(List.class);
            verify(stockRepository).saveAll(captor.capture());

            List<Stock> savedStocks = captor.getValue();
            assertThat(savedStocks).hasSize(1);
            assertThat(savedStocks.get(0).getProductId()).isEqualTo(100L);
            assertThat(savedStocks.get(0).getWarehouse().getId()).isEqualTo(1L);
            assertThat(savedStocks.get(0).getQuantity()).isEqualTo(0L);
            assertThat(savedStocks.get(0).getReservedQuantity()).isEqualTo(0L);
        }
    }

    @Nested
    class GetStocksByWarehouseIdAndProductNameAndSku {

        @Test
        void shouldReturnStockVms() {
            Long warehouseId = 1L;
            ProductInfoVm productVm = new ProductInfoVm(100L, "Product A", "SKU-A", true);

            when(warehouseService.getProductWarehouse(warehouseId, "Product A", "SKU-A", FilterExistInWhSelection.YES))
                    .thenReturn(List.of(productVm));

            Stock stock = Stock.builder()
                    .id(10L)
                    .warehouse(warehouse)
                    .productId(100L)
                    .quantity(50L)
                    .reservedQuantity(5L)
                    .build();

            when(stockRepository.findByWarehouseIdAndProductIdIn(eq(warehouseId), eq(List.of(100L))))
                    .thenReturn(List.of(stock));

            List<StockVm> result = stockService.getStocksByWarehouseIdAndProductNameAndSku(warehouseId, "Product A",
                    "SKU-A");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).id()).isEqualTo(10L);
            assertThat(result.get(0).productId()).isEqualTo(100L);
            assertThat(result.get(0).productName()).isEqualTo("Product A");
            assertThat(result.get(0).productSku()).isEqualTo("SKU-A");
            assertThat(result.get(0).quantity()).isEqualTo(50L);
        }

        @Test
        void shouldReturnEmptyList_WhenNoProductsFound() {
            Long warehouseId = 1L;

            when(warehouseService.getProductWarehouse(warehouseId, "Product A", "SKU-A", FilterExistInWhSelection.YES))
                    .thenReturn(List.of());

            when(stockRepository.findByWarehouseIdAndProductIdIn(eq(warehouseId), eq(List.of())))
                    .thenReturn(List.of());

            List<StockVm> result = stockService.getStocksByWarehouseIdAndProductNameAndSku(warehouseId, "Product A",
                    "SKU-A");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    class UpdateProductQuantityInStock {

        @Test
        void shouldContinue_WhenStockQuantityVmIsNull() {
            StockQuantityUpdateVm request = new StockQuantityUpdateVm(
                    List.of(new StockQuantityVm(99L, 10L, "test note")));

            Stock existingStock = Stock.builder().id(10L).quantity(50L).build();
            when(stockRepository.findAllById(List.of(99L))).thenReturn(List.of(existingStock));

            stockService.updateProductQuantityInStock(request);

            assertThat(existingStock.getQuantity()).isEqualTo(50L);
            verify(stockRepository).saveAll(anyList());
        }

        @Test
        void shouldThrowBadRequestException_WhenAdjustedQuantityIsInvalid() {
            StockQuantityUpdateVm request = new StockQuantityUpdateVm(
                    List.of(new StockQuantityVm(10L, -10L, "invalid quantity")));

            Stock existingStock = Stock.builder().id(10L).quantity(-50L).build();
            when(stockRepository.findAllById(List.of(10L))).thenReturn(List.of(existingStock));

            assertThatThrownBy(() -> stockService.updateProductQuantityInStock(request))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        void shouldUpdateQuantityAndCallServices_WhenValid() {
            StockQuantityVm sqVm1 = new StockQuantityVm(10L, null, "null quantity"); // sẽ được gán bằng 0
            StockQuantityVm sqVm2 = new StockQuantityVm(11L, -10L, "deduct quantity");
            StockQuantityUpdateVm request = new StockQuantityUpdateVm(List.of(sqVm1, sqVm2));

            Stock stock1 = Stock.builder().id(10L).productId(100L).quantity(50L).build();
            Stock stock2 = Stock.builder().id(11L).productId(200L).quantity(50L).build();

            when(stockRepository.findAllById(List.of(10L, 11L))).thenReturn(List.of(stock1, stock2));

            stockService.updateProductQuantityInStock(request);

            assertThat(stock1.getQuantity()).isEqualTo(50L);
            assertThat(stock2.getQuantity()).isEqualTo(40L);

            ArgumentCaptor<List<Stock>> stockCaptor = ArgumentCaptor.forClass(List.class);
            verify(stockRepository).saveAll(stockCaptor.capture());

            verify(stockHistoryService).createStockHistories(eq(List.of(stock1, stock2)), eq(List.of(sqVm1, sqVm2)));

            ArgumentCaptor<List<ProductQuantityPostVm>> productQuantityCaptor = ArgumentCaptor.forClass(List.class);
            verify(productService).updateProductQuantity(productQuantityCaptor.capture());

            List<ProductQuantityPostVm> productQuantityList = productQuantityCaptor.getValue();
            assertThat(productQuantityList).hasSize(2);
            assertThat(productQuantityList.get(0).productId()).isEqualTo(100L);
            assertThat(productQuantityList.get(0).stockQuantity()).isEqualTo(50L); // Sau khi update
        }

        @Test
        void shouldNotCallProductService_WhenStockListIsEmpty() {
            StockQuantityUpdateVm request = new StockQuantityUpdateVm(List.of());

            when(stockRepository.findAllById(List.of())).thenReturn(List.of());

            stockService.updateProductQuantityInStock(request);

            verify(stockRepository).saveAll(List.of());
            verify(stockHistoryService).createStockHistories(List.of(), List.of());

            verify(productService, org.mockito.Mockito.never()).updateProductQuantity(anyList());
        }
    }
}