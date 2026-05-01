package com.yas.media;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.yas.media.controller.MediaController;
import com.yas.media.model.Media;
import com.yas.media.model.dto.MediaDto;
import com.yas.media.service.MediaService;
import com.yas.media.viewmodel.MediaPostVm;
import com.yas.media.viewmodel.MediaVm;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

// IMPORT ĐÚNG CHUẨN SPRING BOOT 4 CỦA BẠN:
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = MediaController.class, excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        OAuth2ResourceServerAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
class MediaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MediaService mediaService;

    private Media mockMedia;
    private MediaVm mockMediaVm;

    @BeforeEach
    void setUp() {
        mockMedia = new Media();
        mockMedia.setId(1L);
        mockMedia.setCaption("Test Caption");
        mockMedia.setFileName("test-file.jpg");
        mockMedia.setMediaType("image/jpeg");

        mockMediaVm = new MediaVm(1L, "Test Caption", "test-file.jpg", "image/jpeg", "http://url.com");
    }

    @Nested
    class CreateMedia {
        @Test
        void shouldReturnOkAndNoFileMediaVm_WhenValidRequest() throws Exception {
            // BYPASS: Chuỗi Base64 của một file ảnh GIF 1x1 pixel thật 100% (chỉ 43 bytes).
            // Mọi thư viện validator khó tính nhất cũng phải công nhận đây là ảnh GIF hợp
            // lệ.
            String base64Gif = "R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7";
            byte[] realImageBytes = java.util.Base64.getDecoder().decode(base64Gif);

            MockMultipartFile file = new MockMultipartFile(
                    "multipartFile",
                    "test-file.gif", // Đổi tên đuôi thành gif
                    MediaType.IMAGE_GIF_VALUE, // Đổi content type thành gif
                    realImageBytes);

            when(mediaService.saveMedia(any(MediaPostVm.class))).thenReturn(mockMedia);

            mockMvc.perform(multipart("/medias")
                    .file(file)
                    .param("caption", "Test Caption")
                    .contentType(MediaType.MULTIPART_FORM_DATA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.caption").value("Test Caption"))
                    .andExpect(jsonPath("$.fileName").value("test-file.jpg"))
                    .andExpect(jsonPath("$.mediaType").value("image/jpeg"));
        }
    }

    @Nested
    class DeleteMedia {
        @Test
        void shouldReturnNoContent_WhenDeletedSuccessfully() throws Exception {
            doNothing().when(mediaService).removeMedia(1L);

            mockMvc.perform(delete("/medias/{id}", 1L))
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    class GetMediaById {
        @Test
        void shouldReturnOkAndMediaVm_WhenFound() throws Exception {
            when(mediaService.getMediaById(1L)).thenReturn(mockMediaVm);

            mockMvc.perform(get("/medias/{id}", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.caption").value("Test Caption"))
                    .andExpect(jsonPath("$.url").value("http://url.com"));
        }

        @Test
        void shouldReturnNotFound_WhenMediaDoesNotExist() throws Exception {
            when(mediaService.getMediaById(99L)).thenReturn(null);

            mockMvc.perform(get("/medias/{id}", 99L))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class GetMediasByIds {
        @Test
        void shouldReturnOkAndListMediaVm_WhenFound() throws Exception {
            when(mediaService.getMediaByIds(anyList())).thenReturn(List.of(mockMediaVm));

            mockMvc.perform(get("/medias")
                    .param("ids", "1", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(1L))
                    .andExpect(jsonPath("$[0].caption").value("Test Caption"));
        }

        @Test
        void shouldReturnNotFound_WhenListIsEmpty() throws Exception {
            when(mediaService.getMediaByIds(anyList())).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/medias")
                    .param("ids", "99"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void shouldReturnBadRequest_WhenIdsParamIsMissingOrEmpty() throws Exception {
            mockMvc.perform(get("/medias")
                    .param("ids", ""))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class GetFile {
        @Test
        void shouldReturnOkAndFileStream_WhenFound() throws Exception {
            String fileName = "test-file.jpg";
            InputStream inputStream = new ByteArrayInputStream("dummy content".getBytes());

            MediaDto mediaDto = MediaDto.builder()
                    .mediaType(MediaType.IMAGE_JPEG)
                    .content(inputStream)
                    .build();

            when(mediaService.getFile(eq(1L), eq(fileName))).thenReturn(mediaDto);

            mockMvc.perform(get("/medias/{id}/file/{fileName}", 1L, fileName))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + fileName + "\""))
                    .andExpect(content().contentType(MediaType.IMAGE_JPEG))
                    .andExpect(content().bytes("dummy content".getBytes()));
        }
    }
}