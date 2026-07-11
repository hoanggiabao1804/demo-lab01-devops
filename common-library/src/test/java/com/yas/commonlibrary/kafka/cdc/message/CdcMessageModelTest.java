package com.yas.commonlibrary.kafka.cdc.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CdcMessageModelTest {

    @Test
    void operation_getName_returnsDebeziumOperationCode() {
        assertEquals("r", Operation.READ.getName());
        assertEquals("c", Operation.CREATE.getName());
        assertEquals("u", Operation.UPDATE.getName());
        assertEquals("d", Operation.DELETE.getName());
    }

    @Test
    void product_supportsBuilderNoArgsAndSetters() {
        Product product = Product.builder()
            .id(10L)
            .isPublished(true)
            .build();

        assertEquals(10L, product.getId());
        assertTrue(product.isPublished());

        Product emptyProduct = new Product();
        emptyProduct.setId(11L);
        emptyProduct.setPublished(false);

        assertEquals(11L, emptyProduct.getId());
        assertFalse(emptyProduct.isPublished());
    }

    @Test
    void productCdcMessage_supportsBuilderNoArgsAndSetters() {
        Product before = Product.builder().id(1L).isPublished(false).build();
        Product after = Product.builder().id(1L).isPublished(true).build();

        ProductCdcMessage message = ProductCdcMessage.builder()
            .before(before)
            .after(after)
            .op(Operation.UPDATE)
            .build();

        assertSame(before, message.getBefore());
        assertSame(after, message.getAfter());
        assertEquals(Operation.UPDATE, message.getOp());

        ProductCdcMessage emptyMessage = new ProductCdcMessage();
        emptyMessage.setBefore(before);
        emptyMessage.setAfter(after);
        emptyMessage.setOp(Operation.CREATE);

        assertSame(before, emptyMessage.getBefore());
        assertSame(after, emptyMessage.getAfter());
        assertEquals(Operation.CREATE, emptyMessage.getOp());
    }

    @Test
    void productMsgKey_supportsBuilderNoArgsAndSetters() {
        ProductMsgKey key = ProductMsgKey.builder().id(5L).build();
        assertEquals(5L, key.getId());

        ProductMsgKey emptyKey = new ProductMsgKey();
        emptyKey.setId(6L);
        assertEquals(6L, emptyKey.getId());
    }
}
