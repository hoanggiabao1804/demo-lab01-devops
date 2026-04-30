package com.yas.sampledata.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.yas.sampledata.utils.SqlScriptExecutor;
import com.yas.sampledata.viewmodel.SampleDataVm;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SampleDataServiceTest {
    @Mock
    private DataSource productDataSource;

    @Mock
    private DataSource mediaDataSource;

    @InjectMocks
    private SampleDataService sampleDataService;

    @Test
    void testCreateSampleDataSuccess() {
        try (MockedStatic<SqlScriptExecutor> mockedStatic = Mockito.mockStatic(SqlScriptExecutor.class)) {
            // Arrange
            SqlScriptExecutor mockExecutor = mock(SqlScriptExecutor.class);
            mockedStatic.when(SqlScriptExecutor::new).thenReturn(mockExecutor);

            // Act
            SampleDataVm result = sampleDataService.createSampleData();

            // Assert
            assertNotNull(result);
            assertEquals("Insert Sample Data successfully!", result.message());
            verify(mockExecutor, times(1))
                .executeScriptsForSchema(any(DataSource.class), anyString(), anyString());
            verify(mockExecutor, times(2))
                .executeScriptsForSchema(any(DataSource.class), anyString(), anyString());
        }
    }
}
