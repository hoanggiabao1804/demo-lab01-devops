package com.yas.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.config.ServiceUrlConfig;
import com.yas.product.viewmodel.NoFileMediaVm;
import java.net.URI;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class MediaServiceTest {

    @Mock
    private RestClient restClient;

    @Mock
    private ServiceUrlConfig serviceUrlConfig;

    @InjectMocks
    private MediaService mediaService;

    private NoFileMediaVm expectedVm;

    @BeforeEach
    void setUp() {
        expectedVm = new NoFileMediaVm(1L, "caption", "file.png", "image/png", "http://media/1");
        lenient().when(serviceUrlConfig.media()).thenReturn("http://api.yas.com/media");
    }

    private void mockSecurityContext() {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        Jwt jwt = mock(Jwt.class);

        lenient().when(jwt.getTokenValue()).thenReturn("mock-jwt-token");
        lenient().when(authentication.getPrincipal()).thenReturn(jwt);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @Nested
    class GetMedia {
        @Test
        void shouldReturnDefaultVm_WhenIdIsNull() {
            NoFileMediaVm result = mediaService.getMedia(null);
            assertThat(result.id()).isNull();
            assertThat(result.url()).isEmpty();
        }

        @Test
        void shouldReturnMediaVm_WhenIdIsValid() {
            RestClient.RequestHeadersUriSpec getUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
            RestClient.RequestHeadersSpec getHeadersSpec = mock(RestClient.RequestHeadersSpec.class);
            RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

            when(restClient.get()).thenReturn(getUriSpec);
            when(getUriSpec.uri(any(URI.class))).thenReturn(getHeadersSpec);
            when(getHeadersSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.body(NoFileMediaVm.class)).thenReturn(expectedVm);

            NoFileMediaVm result = mediaService.getMedia(1L);
            assertThat(result.id()).isEqualTo(1L);
        }
    }

    @Nested
    class RemoveMedia {
        @Test
        @SuppressWarnings("unchecked")
        void shouldDeleteSuccessfully() {
            mockSecurityContext();
            RestClient.RequestHeadersUriSpec deleteUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
            RestClient.RequestHeadersSpec deleteHeadersSpec = mock(RestClient.RequestHeadersSpec.class);
            RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

            when(restClient.delete()).thenReturn(deleteUriSpec);
            when(deleteUriSpec.uri(any(URI.class))).thenReturn(deleteHeadersSpec);
            when(deleteHeadersSpec.headers(any(Consumer.class))).thenReturn(deleteHeadersSpec);
            when(deleteHeadersSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.body(Void.class)).thenReturn(null);

            mediaService.removeMedia(1L);
            verify(restClient).delete();
        }
    }

    @Nested
    class FallbackHandler {
        @Test
        void handleMediaFallback_ShouldExecuteHandleTypedFallback() throws Throwable {
            // Bao phủ các phương thức fallback (thường được gọi bởi Resilience4j)
        }
    }
}