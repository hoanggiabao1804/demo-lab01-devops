package com.yas.recommendation.store;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import tools.jackson.databind.ObjectMapper;
import com.yas.recommendation.configuration.EmbeddingSearchConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap; // Thêm import HashMap
import java.util.List;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class SimpleVectorRepositoryTest {

    @Mock
    private VectorStore vectorStore;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private EmbeddingSearchConfiguration searchConfig;

    private TestVectorRepository repository;

    @BeforeEach
    void setUp() {
        repository = new TestVectorRepository(vectorStore);
        ReflectionTestUtils.setField(repository, "objectMapper", objectMapper);
        ReflectionTestUtils.setField(repository, "embeddingSearchConfiguration", searchConfig);
    }

    @Test
    void add_ShouldSuccess() {
        // GIVEN: Sử dụng HashMap để Map có thể thực hiện lệnh .put() trong code nghiệp
        // vụ
        Map<String, Object> contentMap = new HashMap<>();
        contentMap.put("id", 1L);
        contentMap.put("name", "test");

        when(objectMapper.convertValue(any(), eq(Map.class))).thenReturn(contentMap);

        // WHEN
        repository.add(1L);

        // THEN
        verify(vectorStore, times(1)).add(anyList());
    }

    @Test
    void delete_ShouldInvokeVectorStoreDelete() {
        // WHEN
        repository.delete(1L);

        // THEN
        verify(vectorStore, times(1)).delete(anyList());
    }

    @Test
    void update_ShouldInvokeDeleteAndAdd() {
        // GIVEN: Tương tự, sử dụng HashMap cho luồng update (gọi hàm add)[cite: 30, 36]
        Map<String, Object> contentMap = new HashMap<>();
        contentMap.put("id", 1L);

        when(objectMapper.convertValue(any(), eq(Map.class))).thenReturn(contentMap);

        // WHEN
        repository.update(1L);

        // THEN
        verify(vectorStore).delete(anyList());
        verify(vectorStore).add(anyList());
    }

    @Test
    void search_ShouldReturnMatchedDocuments() {
        // GIVEN: Trong hàm search không có lệnh .put() vào Map nên Map.of() vẫn có thể
        // dùng,
        // nhưng để đồng nhất bạn có thể đổi sang HashMap[cite: 30, 36]
        Map<String, Object> contentMap = new HashMap<>();
        contentMap.put("id", 1L);

        when(objectMapper.convertValue(any(), eq(Map.class))).thenReturn(contentMap);
        when(searchConfig.topK()).thenReturn(5);
        when(searchConfig.similarityThreshold()).thenReturn(0.7);

        Document mockDoc = new Document("Result content", Map.of("id", 2L));
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(mockDoc));

        // WHEN
        List<TestDocument> results = repository.search(1L);

        // THEN
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getContent()).isEqualTo("Result content");
        verify(vectorStore).similaritySearch(any(SearchRequest.class));
    }

    @Test
    void toBaseDocument_ShouldMapCorrectly() {
        // GIVEN
        Document doc = new Document("content", Map.of("key", "value"));

        // WHEN
        TestDocument result = ReflectionTestUtils.invokeMethod(repository, "toBaseDocument", doc);

        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEqualTo("content");
        assertThat(result.getMetadata()).containsEntry("key", "value");
    }
}