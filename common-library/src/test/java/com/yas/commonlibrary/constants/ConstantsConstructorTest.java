package com.yas.commonlibrary.constants;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Constructor;
import org.junit.jupiter.api.Test;

class ConstantsConstructorTest {

    @Test
    void privateConstructors_canBeInvokedByReflectionForCoverage() throws Exception {
        assertNotNull(newInstance(ApiConstant.class));
        assertNotNull(newInstance(MessageCode.class));
        assertNotNull(newInstance(PageableConstant.class));
    }

    @Test
    void constants_keepExpectedValues() {
        assertEquals("/backoffice/warehouses", ApiConstant.WAREHOUSE_URL);
        assertEquals("403", ApiConstant.CODE_403);
        assertEquals("ACCESS_DENIED", ApiConstant.ACCESS_DENIED);
        assertEquals("WAREHOUSE_NOT_FOUND", MessageCode.WAREHOUSE_NOT_FOUND);
        assertEquals("PRODUCT_NOT_FOUND", MessageCode.PRODUCT_NOT_FOUND);
        assertEquals("10", PageableConstant.DEFAULT_PAGE_SIZE);
        assertEquals("0", PageableConstant.DEFAULT_PAGE_NUMBER);
    }

    private Object newInstance(Class<?> type) throws Exception {
        Constructor<?> constructor = type.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }
}
