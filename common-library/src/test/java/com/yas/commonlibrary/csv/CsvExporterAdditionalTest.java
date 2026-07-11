package com.yas.commonlibrary.csv;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.yas.commonlibrary.csv.anotation.CsvColumn;
import com.yas.commonlibrary.csv.anotation.CsvName;
import java.io.IOException;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.junit.jupiter.api.Test;

class CsvExporterAdditionalTest {

    @SuperBuilder
    @CsvName(fileName = "NullableFile")
    @Getter
    @Setter
    static class NullableData extends BaseCsv {
        @CsvColumn(columnName = "Name")
        private String name;

        @CsvColumn(columnName = "Tags")
        private List<String> tags;

        private String ignoredField;
    }

    @SuperBuilder
    @CsvName(fileName = "NoGetterFile")
    static class DataWithoutGetter extends BaseCsv {
        @CsvColumn(columnName = "Hidden")
        private String hidden;
    }

    @Test
    void exportToCsv_whenValuesAreNull_writesEmptyCsvColumns() throws IOException {
        List<BaseCsv> values = List.of(NullableData.builder()
            .id(3L)
            .name(null)
            .tags(null)
            .ignoredField("ignored")
            .build());

        String csvContent = new String(CsvExporter.exportToCsv(values, NullableData.class));

        assertEquals("Id,Name,Tags\n3,,\n", csvContent);
    }

    @Test
    void exportToCsv_whenAnnotatedFieldHasNoGetter_writesEmptyColumn() throws IOException {
        List<BaseCsv> values = List.of(DataWithoutGetter.builder()
            .id(4L)
            .hidden("secret")
            .build());

        String csvContent = new String(CsvExporter.exportToCsv(values, DataWithoutGetter.class));

        assertEquals("Id,Hidden\n4,\n", csvContent);
    }
}
