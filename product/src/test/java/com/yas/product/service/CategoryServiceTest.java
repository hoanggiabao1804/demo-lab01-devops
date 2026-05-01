package com.yas.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.BadRequestException;
import com.yas.commonlibrary.exception.DuplicatedException;
import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.product.model.Category;
import com.yas.product.repository.CategoryRepository;
import com.yas.product.viewmodel.NoFileMediaVm;
import com.yas.product.viewmodel.category.CategoryGetDetailVm;
import com.yas.product.viewmodel.category.CategoryGetVm;
import com.yas.product.viewmodel.category.CategoryListGetVm;
import com.yas.product.viewmodel.category.CategoryPostVm;
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
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private MediaService mediaService;

    @InjectMocks
    private CategoryService categoryService;

    private Category category;
    private Category parentCategory;
    private NoFileMediaVm noFileMediaVm;
    private CategoryPostVm categoryPostVm;

    @BeforeEach
    void setUp() {
        parentCategory = new Category();
        parentCategory.setId(2L);
        parentCategory.setName("Parent Category");

        category = new Category();
        category.setId(1L);
        category.setName("name");
        category.setSlug("slug");
        category.setDescription("description");
        category.setMetaKeyword("metaKeyword");
        category.setMetaDescription("metaDescription");
        category.setDisplayOrder((short) 1);
        category.setIsPublished(true);
        category.setImageId(1L);

        noFileMediaVm = new NoFileMediaVm(1L, "caption", "fileName", "mediaType", "url");

        // Dùng lenient() để tránh lỗi UnnecessaryStubbing khi các test khác (như
        // GetCategory) không sử dụng biến này
        categoryPostVm = mock(CategoryPostVm.class);
        lenient().when(categoryPostVm.name()).thenReturn("New Category");
        lenient().when(categoryPostVm.slug()).thenReturn("new-category");
    }

    @Nested
    class GetPageableCategories {
        @Test
        void shouldReturnCategoryList() {
            Page<Category> page = new PageImpl<>(List.of(category));
            when(categoryRepository.findAll(any(Pageable.class))).thenReturn(page);

            CategoryListGetVm result = categoryService.getPageableCategories(0, 10);

            assertThat(result.totalElements()).isEqualTo(1);
            assertThat(result.categoryContent().get(0).name()).isEqualTo("name");
        }
    }

    @Nested
    class CreateCategory {
        @Test
        void shouldThrowDuplicatedException_WhenNameExists() {
            when(categoryRepository.findExistedName(anyString(), eq(null))).thenReturn(category);

            // Cập nhật đúng message text (is already existed)
            assertThatThrownBy(() -> categoryService.create(categoryPostVm))
                    .isInstanceOf(DuplicatedException.class)
                    .hasMessageContaining("is already existed");
        }

        @Test
        void shouldThrowBadRequest_WhenParentCategoryNotFound() {
            when(categoryRepository.findExistedName(anyString(), eq(null))).thenReturn(null);
            when(categoryPostVm.parentId()).thenReturn(99L);
            when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.create(categoryPostVm))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("is not found");
        }

        @Test
        void shouldCreateSuccessfully_WithNoParent() {
            when(categoryRepository.findExistedName(anyString(), eq(null))).thenReturn(null);
            when(categoryPostVm.parentId()).thenReturn(null);
            when(categoryRepository.save(any(Category.class))).thenReturn(category);

            Category result = categoryService.create(categoryPostVm);

            assertThat(result).isNotNull();
            verify(categoryRepository).save(any(Category.class));
        }

        @Test
        void shouldCreateSuccessfully_WithValidParent() {
            when(categoryRepository.findExistedName(anyString(), eq(null))).thenReturn(null);
            when(categoryPostVm.parentId()).thenReturn(2L);
            when(categoryRepository.findById(2L)).thenReturn(Optional.of(parentCategory));

            // Bắt lại category trước khi save để check xem đã set Parent đúng chưa
            ArgumentCaptor<Category> categoryCaptor = ArgumentCaptor.forClass(Category.class);
            when(categoryRepository.save(categoryCaptor.capture())).thenReturn(category);

            categoryService.create(categoryPostVm);

            Category savedCategory = categoryCaptor.getValue();
            assertThat(savedCategory.getParent()).isNotNull();
            assertThat(savedCategory.getParent().getId()).isEqualTo(2L);
        }
    }

    @Nested
    class UpdateCategory {
        @Test
        void shouldThrowNotFound_WhenCategoryToUpdateDoesNotExist() {
            when(categoryRepository.findExistedName(anyString(), anyLong())).thenReturn(null);
            when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.update(categoryPostVm, 1L))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("is not found");
        }

        @Test
        void shouldUpdateSuccessfully_WithNullParent() {
            when(categoryRepository.findExistedName(anyString(), anyLong())).thenReturn(null);
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
            when(categoryPostVm.parentId()).thenReturn(null);

            categoryService.update(categoryPostVm, 1L);

            assertThat(category.getName()).isEqualTo("New Category");
            assertThat(category.getParent()).isNull();
        }

        @Test
        void shouldThrowBadRequest_WhenSettingParentToItself() {
            when(categoryRepository.findExistedName(anyString(), anyLong())).thenReturn(null);
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

            // Cố tình truyền Parent ID bằng đúng ID của category đang sửa (1L)
            when(categoryPostVm.parentId()).thenReturn(1L);
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

            // Cập nhật message text chính xác ("Parent category cannot be itself children")
            assertThatThrownBy(() -> categoryService.update(categoryPostVm, 1L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Parent category cannot be itself children");
        }

        @Test
        void shouldUpdateSuccessfully_WithValidParent() {
            when(categoryRepository.findExistedName(anyString(), anyLong())).thenReturn(null);
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
            when(categoryPostVm.parentId()).thenReturn(2L);
            when(categoryRepository.findById(2L)).thenReturn(Optional.of(parentCategory));

            categoryService.update(categoryPostVm, 1L);

            assertThat(category.getParent()).isEqualTo(parentCategory);
        }
    }

    @Nested
    class CheckParentRecursive {
        @Test
        void shouldThrowBadRequest_WhenCyclicParentDetected() {
            // Setup vòng lặp: Grandparent (3L) -> Parent (2L) -> Category đang sửa (1L)
            Category grandParent = new Category();
            grandParent.setId(3L);

            // Ép Parent (2L) nhận con của nó (1L) làm cha -> Tạo thành vòng lặp vô tận
            parentCategory.setParent(category);

            when(categoryRepository.findExistedName(anyString(), anyLong())).thenReturn(null);
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
            when(categoryPostVm.parentId()).thenReturn(2L);
            when(categoryRepository.findById(2L)).thenReturn(Optional.of(parentCategory));

            // Cập nhật message text chính xác ("Parent category cannot be itself children")
            assertThatThrownBy(() -> categoryService.update(categoryPostVm, 1L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Parent category cannot be itself children");
        }
    }

    @Nested
    class GetCategoryById {
        @Test
        void shouldReturnDetail_WhenImageIsNull() {
            category.setImageId(null);
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

            CategoryGetDetailVm result = categoryService.getCategoryById(1L);

            assertThat(result.categoryImage()).isNull();
            assertThat(result.parentId()).isEqualTo(0L); // Không có cha thì parentId = 0
            verify(mediaService, never()).getMedia(anyLong());
        }

        @Test
        void shouldReturnDetail_WithImageAndParent() {
            category.setParent(parentCategory);
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
            when(mediaService.getMedia(1L)).thenReturn(noFileMediaVm);

            CategoryGetDetailVm result = categoryService.getCategoryById(1L);

            assertThat(result.categoryImage().url()).isEqualTo("url");
            assertThat(result.parentId()).isEqualTo(2L);
        }
    }

    @Nested
    class GetCategories {
        @Test
        void shouldReturnList_WithNoImageAndNoParent() {
            category.setImageId(null);
            when(categoryRepository.findByNameContainingIgnoreCase(anyString())).thenReturn(List.of(category));

            List<CategoryGetVm> result = categoryService.getCategories("name");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).categoryImage()).isNull();
            assertThat(result.get(0).parentId()).isEqualTo(-1L); // Theo logic code, không có cha trả về -1
        }

        @Test
        void shouldReturnList_WithImageAndParent() {
            category.setParent(parentCategory);
            when(categoryRepository.findByNameContainingIgnoreCase(anyString())).thenReturn(List.of(category));
            when(mediaService.getMedia(1L)).thenReturn(noFileMediaVm);

            List<CategoryGetVm> result = categoryService.getCategories("name");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).categoryImage().url()).isEqualTo("url");
            assertThat(result.get(0).parentId()).isEqualTo(2L);
        }
    }

    @Nested
    class MiscMethods {
        @Test
        void getCategoryByIds_ShouldReturnList() {
            when(categoryRepository.findAllById(anyList())).thenReturn(List.of(category));

            List<CategoryGetVm> result = categoryService.getCategoryByIds(List.of(1L));

            assertThat(result).hasSize(1);
            assertThat(result.get(0).id()).isEqualTo(1L);
        }

        @Test
        void getTopNthCategories_ShouldReturnList() {
            when(categoryRepository.findCategoriesOrderedByProductCount(any(Pageable.class)))
                    .thenReturn(List.of("Cat1", "Cat2"));

            List<String> result = categoryService.getTopNthCategories(2);

            assertThat(result).hasSize(2);
            assertThat(result.get(0)).isEqualTo("Cat1");
        }
    }
}