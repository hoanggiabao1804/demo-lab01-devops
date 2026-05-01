package com.yas.recommendation.store;

import com.yas.recommendation.vector.common.store.SimpleVectorRepository;
import org.springframework.ai.vectorstore.VectorStore;

public class TestVectorRepository extends SimpleVectorRepository<TestDocument, String> {
    public TestVectorRepository(VectorStore vectorStore) {
        super(TestDocument.class, vectorStore);
    }

    @Override
    public String getEntity(Long id) {
        return "EntityContent";
    }
}