package com.yas.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.BadRequestException;
import com.yas.commonlibrary.exception.DuplicatedException;
import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.product.model.Brand;
import com.yas.product.model.Category;
import com.yas.product.model.Product;
import com.yas.product.model.ProductCategory;
import com.yas.product.model.ProductImage;
import com.yas.product.repository.BrandRepository;
import com.yas.product.repository.CategoryRepository;
import com.yas.product.repository.ProductCategoryRepository;
import com.yas.product.repository.ProductImageRepository;
import com.yas.product.repository.ProductOptionCombinationRepository;
import com.yas.product.repository.ProductOptionRepository;
import com.yas.product.repository.ProductOptionValueRepository;
import com.yas.product.repository.ProductRelatedRepository;
import com.yas.product.repository.ProductRepository;
import com.yas.product.viewmodel.NoFileMediaVm;
import com.yas.product.viewmodel.product.ProductDetailVm;
import com.yas.product.viewmodel.product.ProductGetDetailVm;
import com.yas.product.viewmodel.product.ProductPostVm;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private MediaService mediaService;
    @Mock
    private BrandRepository brandRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private ProductCategoryRepository productCategoryRepository;
    @Mock
    private ProductImageRepository productImageRepository;
    @Mock
    private ProductOptionRepository productOptionRepository;
    @Mock
    private ProductOptionValueRepository productOptionValueRepository;
    @Mock
    private ProductOptionCombinationRepository productOptionCombinationRepository;
    @Mock
    private ProductRelatedRepository productRelatedRepository;

    @InjectMocks
    private ProductService productService;

    private Product mainProduct;

    @BeforeEach
    void setUp() {
        mainProduct = Product.builder()
                .id(1L)
                .name("Main Product")
                .slug("main-product")
                .sku("SKU-123")
                .gtin("GTIN-123")
                .price(100.0)
                .build();
    }

    @Nested
    class CreateProduct {

        @Test
        void shouldThrowBadRequest_WhenLengthLessThanWidth() {
            ProductPostVm postVm = mock(ProductPostVm.class);
            when(postVm.length()).thenReturn(5.0);
            when(postVm.width()).thenReturn(10.0);

            // Đổi message text cho đúng format lỗi sinh ra
            assertThatThrownBy(() -> productService.createProduct(postVm))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("length greater than width");
        }

        @Test
        void shouldThrowDuplicatedException_WhenSlugExists() {
            ProductPostVm postVm = mock(ProductPostVm.class);
            when(postVm.length()).thenReturn(10.0);
            when(postVm.width()).thenReturn(5.0);
            when(postVm.slug()).thenReturn("existing-slug");

            Product existingProduct = Product.builder().id(99L).build();
            when(productRepository.findBySlugAndIsPublishedTrue("existing-slug"))
                    .thenReturn(Optional.of(existingProduct));

            // Đổi message text cho đúng format lỗi sinh ra
            assertThatThrownBy(() -> productService.createProduct(postVm))
                    .isInstanceOf(DuplicatedException.class)
                    .hasMessageContaining("is already existed or is duplicated");
        }

        @Test
        void shouldCreateMainProductSuccessfully_WhenNoVariations() {
            ProductPostVm postVm = mock(ProductPostVm.class);
            when(postVm.length()).thenReturn(10.0);
            when(postVm.width()).thenReturn(5.0);
            when(postVm.slug()).thenReturn("new-product");
            when(postVm.gtin()).thenReturn("GTIN-NEW");
            when(postVm.sku()).thenReturn("SKU-NEW");
            when(postVm.brandId()).thenReturn(1L);
            when(postVm.categoryIds()).thenReturn(List.of(1L, 2L));
            when(postVm.variations()).thenReturn(Collections.emptyList());

            // Mock danh sách relatedProducts để trigger lời gọi
            // productRelatedRepository.saveAll
            when(postVm.relatedProductIds()).thenReturn(List.of(3L));

            Brand brand = new Brand();
            brand.setId(1L);
            when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));

            when(productRepository.findBySlugAndIsPublishedTrue(anyString())).thenReturn(Optional.empty());
            when(productRepository.findByGtinAndIsPublishedTrue(anyString())).thenReturn(Optional.empty());
            when(productRepository.findBySkuAndIsPublishedTrue(anyString())).thenReturn(Optional.empty());
            when(productRepository.save(any(Product.class))).thenReturn(mainProduct);

            Category category1 = new Category();
            category1.setId(1L);
            Category category2 = new Category();
            category2.setId(2L);
            when(categoryRepository.findAllById(anyList())).thenReturn(List.of(category1, category2));

            // Mock trả về dữ liệu cho related products
            Product relatedProduct = new Product();
            relatedProduct.setId(3L);
            when(productRepository.findAllById(anyList())).thenReturn(List.of(relatedProduct));

            ProductGetDetailVm result = productService.createProduct(postVm);

            assertThat(result).isNotNull();
            verify(productRepository).save(any(Product.class));
            verify(productCategoryRepository).saveAll(anyList());
            verify(productImageRepository).saveAll(anyList());
            verify(productRelatedRepository).saveAll(anyList());
            verify(productOptionValueRepository, never()).saveAll(anyList());
        }
    }

    @Nested
    class GetProductById {
        @Test
        void shouldThrowNotFound_WhenProductDoesNotExist() {
            when(productRepository.findById(99L)).thenReturn(Optional.empty());

            // Đổi message text cho đúng format lỗi sinh ra
            assertThatThrownBy(() -> productService.getProductById(99L))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("is not found");
        }

        @Test
        void shouldReturnProductDetailVm_WhenProductExists() {
            Brand brand = new Brand();
            brand.setId(1L);
            brand.setName("BrandName");

            Category category = new Category();
            category.setId(1L);
            category.setName("CategoryName");

            ProductCategory productCategory = ProductCategory.builder().product(mainProduct).category(category).build();
            ProductImage productImage = ProductImage.builder().imageId(100L).product(mainProduct).build();

            mainProduct.setBrand(brand);
            mainProduct.setProductCategories(List.of(productCategory));
            mainProduct.setProductImages(List.of(productImage));
            mainProduct.setThumbnailMediaId(200L);

            when(productRepository.findById(1L)).thenReturn(Optional.of(mainProduct));

            NoFileMediaVm mediaVm1 = mock(NoFileMediaVm.class);
            NoFileMediaVm mediaVm2 = mock(NoFileMediaVm.class);
            when(mediaVm1.url()).thenReturn("url-image-100");
            when(mediaVm2.url()).thenReturn("url-thumbnail-200");

            when(mediaService.getMedia(100L)).thenReturn(mediaVm1);
            when(mediaService.getMedia(200L)).thenReturn(mediaVm2);

            ProductDetailVm result = productService.getProductById(1L);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.brandId()).isEqualTo(1L);
            assertThat(result.productImageMedias()).hasSize(1);
        }
    }

    @Nested
    class DeleteProduct {
        @Test
        void shouldSoftDeleteProductSuccessfully() {
            when(productRepository.findById(1L)).thenReturn(Optional.of(mainProduct));

            productService.deleteProduct(1L);

            assertThat(mainProduct.isPublished()).isFalse();
            verify(productRepository).save(mainProduct);
        }
    }

    @Nested
    class GetProductsWithFilter {
        @Test
        void shouldReturnProductListGetVm_WhenProductsExist() {
            // Chuẩn bị dữ liệu trả về từ Repository
            org.springframework.data.domain.Page<Product> page = new org.springframework.data.domain.PageImpl<>(
                    List.of(mainProduct));

            // Giả lập Repository trả về một trang dữ liệu
            when(productRepository.getProductsWithFilter(anyString(), anyString(),
                    any(org.springframework.data.domain.Pageable.class)))
                    .thenReturn(page);

            // Gọi hàm cần test (truyền vào pageNo=0, pageSize=10, productName="Main",
            // brandName="")
            com.yas.product.viewmodel.product.ProductListGetVm result = productService.getProductsWithFilter(0, 10,
                    "Main", "");

            // Xác nhận kết quả map đúng
            assertThat(result).isNotNull();
            assertThat(result.totalElements()).isEqualTo(1);
            assertThat(result.productContent()).hasSize(1);
            assertThat(result.productContent().get(0).name()).isEqualTo("Main Product");
        }
    }

    @Nested
    class StockQuantityManagement {
        @Test
        void shouldUpdateStockQuantitySuccessfully() {
            // Giả lập dữ liệu đầu vào: Danh sách chứa 1 yêu cầu cập nhật số lượng
            com.yas.product.viewmodel.product.ProductQuantityPostVm quantityVm = new com.yas.product.viewmodel.product.ProductQuantityPostVm(
                    1L, 50L);
            List<com.yas.product.viewmodel.product.ProductQuantityPostVm> reqList = List.of(quantityVm);

            // Gán số lượng tồn kho ban đầu là 10
            mainProduct.setStockQuantity(10L);
            when(productRepository.findAllByIdIn(anyList())).thenReturn(List.of(mainProduct));

            // Gọi hàm cần test
            productService.updateProductQuantity(reqList);

            // Xác nhận tồn kho đã được ghi đè bằng giá trị mới (50)
            assertThat(mainProduct.getStockQuantity()).isEqualTo(50L);
            verify(productRepository).saveAll(anyList());
        }

        @Test
        void shouldSubtractStockQuantitySuccessfully() {
            // Giả lập dữ liệu đầu vào: Yêu cầu trừ đi 5 sản phẩm
            com.yas.product.viewmodel.product.ProductQuantityPutVm quantityPutVm = new com.yas.product.viewmodel.product.ProductQuantityPutVm(
                    1L, 5L);
            List<com.yas.product.viewmodel.product.ProductQuantityPutVm> reqList = List.of(quantityPutVm);

            // Cấu hình Product cho phép theo dõi tồn kho và set giá trị ban đầu là 10
            mainProduct.setStockTrackingEnabled(true);
            mainProduct.setStockQuantity(10L);
            when(productRepository.findAllByIdIn(anyList())).thenReturn(List.of(mainProduct));

            // Gọi hàm cần test
            productService.subtractStockQuantity(reqList);

            // 10 - 5 = 5. Xác nhận tồn kho bị trừ đúng.
            assertThat(mainProduct.getStockQuantity()).isEqualTo(5L);
            verify(productRepository).saveAll(anyList());
        }

        @Test
        void shouldNotSubtractStockQuantityBelowZero() {
            // Yêu cầu trừ đi 20 sản phẩm, nhưng trong kho chỉ có 10
            com.yas.product.viewmodel.product.ProductQuantityPutVm quantityPutVm = new com.yas.product.viewmodel.product.ProductQuantityPutVm(
                    1L, 20L);
            List<com.yas.product.viewmodel.product.ProductQuantityPutVm> reqList = List.of(quantityPutVm);

            mainProduct.setStockTrackingEnabled(true);
            mainProduct.setStockQuantity(10L);
            when(productRepository.findAllByIdIn(anyList())).thenReturn(List.of(mainProduct));

            // Gọi hàm cần test
            productService.subtractStockQuantity(reqList);

            // Hàm tính toán subtractStockQuantity phải tự động chặn không cho giá trị âm,
            // trả về 0.
            assertThat(mainProduct.getStockQuantity()).isZero();
            verify(productRepository).saveAll(anyList());
        }

        @Test
        void shouldRestoreStockQuantitySuccessfully() {
            // Giả lập yêu cầu hoàn trả lại 5 sản phẩm vào kho
            com.yas.product.viewmodel.product.ProductQuantityPutVm quantityPutVm = new com.yas.product.viewmodel.product.ProductQuantityPutVm(
                    1L, 5L);
            List<com.yas.product.viewmodel.product.ProductQuantityPutVm> reqList = List.of(quantityPutVm);

            mainProduct.setStockTrackingEnabled(true);
            mainProduct.setStockQuantity(10L);
            when(productRepository.findAllByIdIn(anyList())).thenReturn(List.of(mainProduct));

            // Gọi hàm cần test
            productService.restoreStockQuantity(reqList);

            // 10 + 5 = 15. Xác nhận tồn kho cộng thêm.
            assertThat(mainProduct.getStockQuantity()).isEqualTo(15L);
            verify(productRepository).saveAll(anyList());
        }
    }

    @Nested
    class ProductVariationsAndRelations {
        @Test
        void getProductVariationsByParentId_ShouldReturnList_WhenHasOptions() {
            // Giả lập sản phẩm cha có biến thể
            mainProduct.setHasOptions(true);
            Product variation = Product.builder().id(2L).name("Var 1").isPublished(true).build();
            mainProduct.setProducts(List.of(variation));

            when(productRepository.findById(1L)).thenReturn(Optional.of(mainProduct));
            // Mock kết hợp option (Combination)
            com.yas.product.model.ProductOptionCombination combination = mock(
                    com.yas.product.model.ProductOptionCombination.class);
            com.yas.product.model.ProductOption option = mock(com.yas.product.model.ProductOption.class);
            when(option.getId()).thenReturn(10L);
            when(combination.getProductOption()).thenReturn(option);
            when(combination.getValue()).thenReturn("Red");
            when(productOptionCombinationRepository.findAllByProduct(variation)).thenReturn(List.of(combination));

            List<com.yas.product.viewmodel.product.ProductVariationGetVm> result = productService
                    .getProductVariationsByParentId(1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).name()).isEqualTo("Var 1");
            assertThat(result.get(0).options().get(10L)).isEqualTo("Red");
        }

        @Test
        void getProductSlug_ShouldReturnParentSlug_WhenIsVariation() {
            Product childProduct = Product.builder().id(2L).parent(mainProduct).build();
            when(productRepository.findById(2L)).thenReturn(Optional.of(childProduct));

            com.yas.product.viewmodel.product.ProductSlugGetVm result = productService.getProductSlug(2L);

            // Phải trả về slug của sản phẩm cha
            assertThat(result.slug()).isEqualTo(mainProduct.getSlug());
        }
    }

    @Nested
    class SearchAndExport {
        @Test
        void getProductsByBrand_ShouldReturnThumbnails() {
            Brand brand = new Brand();
            brand.setSlug("apple");
            when(brandRepository.findBySlug("apple")).thenReturn(Optional.of(brand));
            when(productRepository.findAllByBrandAndIsPublishedTrueOrderByIdAsc(brand))
                    .thenReturn(List.of(mainProduct));

            NoFileMediaVm media = mock(NoFileMediaVm.class);
            when(media.url()).thenReturn("thumb-url");
            when(mediaService.getMedia(any())).thenReturn(media);

            List<com.yas.product.viewmodel.product.ProductThumbnailVm> result = productService
                    .getProductsByBrand("apple");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).slug()).isEqualTo("main-product");
        }

        @Test
        void exportProducts_ShouldReturnExportList() {
            // Dùng setter vì Brand không có builder
            Brand brand = new Brand();
            brand.setId(1L);
            brand.setName("Apple");
            mainProduct.setBrand(brand);

            when(productRepository.getExportingProducts(anyString(), anyString())).thenReturn(List.of(mainProduct));

            List<com.yas.product.viewmodel.product.ProductExportingDetailVm> result = productService
                    .exportProducts("Main", "Apple");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).brandName()).isEqualTo("Apple");
        }
    }

    @Nested
    class SpecializedViews {
        @Test
        void getProductEsDetailById_ShouldReturnElasticsearchVm() {
            // Mock dependencies cho Elasticsearch detail
            when(productRepository.findById(1L)).thenReturn(Optional.of(mainProduct));

            com.yas.product.viewmodel.product.ProductEsDetailVm result = productService.getProductEsDetailById(1L);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.slug()).isEqualTo("main-product");
        }

        @Test
        void getProductsForWarehouse_ShouldReturnInfoList() {
            // Test truy vấn dành cho kho hàng
            when(productRepository.findProductForWarehouse(any(), any(), any(), any()))
                    .thenReturn(List.of(mainProduct));

            List<com.yas.product.viewmodel.product.ProductInfoVm> result = productService.getProductsForWarehouse(
                    "name", "sku", List.of(1L),
                    com.yas.product.model.enumeration.FilterExistInWhSelection.ALL);

            assertThat(result).hasSize(1);
        }
    }
}