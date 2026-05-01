package com.yas.recommendation.store;

import com.yas.recommendation.vector.common.document.BaseDocument;
import com.yas.recommendation.vector.common.document.DocumentMetadata;
import com.yas.recommendation.vector.common.formatter.DefaultDocumentFormatter;

@DocumentMetadata(docIdPrefix = "test_", contentFormat = "json", documentFormatter = DefaultDocumentFormatter.class)
public class TestDocument extends BaseDocument {
    public TestDocument() {
    }
}