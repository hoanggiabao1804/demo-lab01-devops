package com.yas.commonlibrary.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;

class AbstractAuditEntityTest {

    @Test
    void auditEntity_gettersAndSettersWork() {
        AbstractAuditEntity entity = new AbstractAuditEntity();
        ZonedDateTime createdOn = ZonedDateTime.parse("2026-04-22T10:15:30+07:00");
        ZonedDateTime lastModifiedOn = ZonedDateTime.parse("2026-04-23T10:15:30+07:00");

        entity.setCreatedOn(createdOn);
        entity.setCreatedBy("creator");
        entity.setLastModifiedOn(lastModifiedOn);
        entity.setLastModifiedBy("modifier");

        assertEquals(createdOn, entity.getCreatedOn());
        assertEquals("creator", entity.getCreatedBy());
        assertEquals(lastModifiedOn, entity.getLastModifiedOn());
        assertEquals("modifier", entity.getLastModifiedBy());
    }
}
