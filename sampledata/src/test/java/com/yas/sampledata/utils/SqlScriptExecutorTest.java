package com.yas.sampledata.utils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.init.ScriptUtils;

@ExtendWith(MockitoExtension.class)
class SqlScriptExecutorTest {

    @InjectMocks
    private SqlScriptExecutor sqlScriptExecutor;

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private Resource resource;

    private MockedStatic<ScriptUtils> mockedScriptUtils;

    @BeforeEach
    void setUp() {
        // Giả lập class tĩnh ScriptUtils để tránh chạy script SQL thật xuống DB
        mockedScriptUtils = mockStatic(ScriptUtils.class);
    }

    @AfterEach
    void tearDown() {
        // Đóng mockStatic sau mỗi test để tránh ảnh hưởng memory/test khác
        mockedScriptUtils.close();
    }

    @Test
    void executeScriptsForSchema_Success() throws Exception {
        // GIVEN
        when(dataSource.getConnection()).thenReturn(connection);
        when(resource.getFilename()).thenReturn("test-script.sql");

        // Giả lập việc khởi tạo "new PathMatchingResourcePatternResolver()"
        try (MockedConstruction<PathMatchingResourcePatternResolver> mockedResolver = mockConstruction(
                PathMatchingResourcePatternResolver.class,
                (mock, context) -> {
                    when(mock.getResources(anyString())).thenReturn(new Resource[] { resource });
                })) {

            // WHEN
            sqlScriptExecutor.executeScriptsForSchema(dataSource, "public", "classpath*:*.sql");

            // THEN
            verify(connection).setSchema("public");
            mockedScriptUtils.verify(() -> ScriptUtils.executeSqlScript(connection, resource));
        }
    }

    @Test
    void executeScriptsForSchema_ThrowsException_WhenResolverFails() {
        // GIVEN
        // Giả lập việc getResources ném ra Exception (VD: sai cú pháp pattern, lỗi đọc
        // file...)
        try (MockedConstruction<PathMatchingResourcePatternResolver> mockedResolver = mockConstruction(
                PathMatchingResourcePatternResolver.class,
                (mock, context) -> {
                    when(mock.getResources(anyString())).thenThrow(new RuntimeException("Simulated Resolver Error"));
                })) {

            // WHEN
            sqlScriptExecutor.executeScriptsForSchema(dataSource, "public", "classpath*:*.sql");

            // THEN
            // Log.error đã catch lỗi nên code không throw ra ngoài.
            // Xác nhận rằng hàm private executeSqlScript không được gọi.
            verifyNoInteractions(dataSource);
            mockedScriptUtils.verifyNoInteractions();
        }
    }

    @Test
    void executeScriptsForSchema_ThrowsSQLException_WhenConnectionFails() throws Exception {
        // GIVEN
        // Giả lập lỗi khi lấy Connection từ DataSource
        when(dataSource.getConnection()).thenThrow(new SQLException("Simulated Database Connection Error"));

        try (MockedConstruction<PathMatchingResourcePatternResolver> mockedResolver = mockConstruction(
                PathMatchingResourcePatternResolver.class,
                (mock, context) -> {
                    when(mock.getResources(anyString())).thenReturn(new Resource[] { resource });
                })) {

            // WHEN
            sqlScriptExecutor.executeScriptsForSchema(dataSource, "public", "classpath*:*.sql");

            // THEN
            // Hàm catch (SQLException e) trong method executeSqlScript sẽ bắt lỗi này
            verify(dataSource).getConnection();
            mockedScriptUtils.verifyNoInteractions(); // ScriptUtils không được phép chạy
        }
    }
}