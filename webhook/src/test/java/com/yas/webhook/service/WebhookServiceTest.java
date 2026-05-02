package com.yas.webhook.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// SỬA IMPORT TẠI ĐÂY: Sử dụng tools.jackson thay vì com.fasterxml.jackson
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.webhook.config.constants.MessageCode;
import com.yas.webhook.integration.api.WebhookApi;
import com.yas.webhook.model.Event;
import com.yas.webhook.model.Webhook;
import com.yas.webhook.model.WebhookEvent;
import com.yas.webhook.model.WebhookEventNotification;
import com.yas.webhook.model.dto.WebhookEventNotificationDto;
import com.yas.webhook.model.enums.NotificationStatus;
import com.yas.webhook.model.mapper.WebhookMapper;
import com.yas.webhook.model.viewmodel.webhook.EventVm;
import com.yas.webhook.model.viewmodel.webhook.WebhookDetailVm;
import com.yas.webhook.model.viewmodel.webhook.WebhookListGetVm;
import com.yas.webhook.model.viewmodel.webhook.WebhookPostVm;
import com.yas.webhook.model.viewmodel.webhook.WebhookVm;
import com.yas.webhook.repository.EventRepository;
import com.yas.webhook.repository.WebhookEventNotificationRepository;
import com.yas.webhook.repository.WebhookEventRepository;
import com.yas.webhook.repository.WebhookRepository;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class WebhookServiceTest {

    @Mock
    private WebhookRepository webhookRepository;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private WebhookEventRepository webhookEventRepository;
    @Mock
    private WebhookEventNotificationRepository webhookEventNotificationRepository;
    @Mock
    private WebhookMapper webhookMapper;
    @Mock
    private WebhookApi webHookApi;

    @InjectMocks
    private WebhookService webhookService;

    private Webhook webhook;
    private WebhookPostVm webhookPostVm;
    private WebhookDetailVm webhookDetailVm;

    @BeforeEach
    void setUp() {
        webhook = new Webhook();
        webhook.setId(1L);
        webhook.setWebhookEvents(List.of(new WebhookEvent()));

        webhookPostVm = new WebhookPostVm();
        webhookPostVm.setPayloadUrl("http://localhost/webhook");

        webhookDetailVm = new WebhookDetailVm();
        webhookDetailVm.setId(1L);
    }

    @Nested
    class GetWebhooks {
        @Test
        void getPageableWebhooks_ShouldReturnWebhookListGetVm() {
            Page<Webhook> webhookPage = new PageImpl<>(List.of(webhook));
            when(webhookRepository.findAll(any(PageRequest.class))).thenReturn(webhookPage);

            WebhookListGetVm expectedVm = WebhookListGetVm.builder().build();
            when(webhookMapper.toWebhookListGetVm(webhookPage, 0, 10)).thenReturn(expectedVm);

            WebhookListGetVm result = webhookService.getPageableWebhooks(0, 10);

            assertThat(result).isEqualTo(expectedVm);
            verify(webhookRepository).findAll(any(PageRequest.class));
        }

        @Test
        void findAllWebhooks_ShouldReturnListWebhookVm() {
            when(webhookRepository.findAll(any(Sort.class))).thenReturn(List.of(webhook));
            WebhookVm webhookVm = new WebhookVm();
            when(webhookMapper.toWebhookVm(webhook)).thenReturn(webhookVm);

            List<WebhookVm> result = webhookService.findAllWebhooks();

            assertThat(result).hasSize(1);
            assertThat(result.get(0)).isEqualTo(webhookVm);
        }

        @Test
        void findById_WhenExists_ShouldReturnWebhookDetailVm() {
            when(webhookRepository.findById(1L)).thenReturn(Optional.of(webhook));
            when(webhookMapper.toWebhookDetailVm(webhook)).thenReturn(webhookDetailVm);

            WebhookDetailVm result = webhookService.findById(1L);

            assertThat(result).isEqualTo(webhookDetailVm);
        }

        @Test
        void findById_WhenNotExists_ShouldThrowNotFoundException() {
            when(webhookRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> webhookService.findById(1L))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining(MessageCode.WEBHOOK_NOT_FOUND);
        }
    }

    @Nested
    class CreateWebhook {
        @Test
        void create_WithoutEvents_ShouldSaveAndReturnWebhookDetailVm() {
            webhookPostVm.setEvents(Collections.emptyList());
            when(webhookMapper.toCreatedWebhook(webhookPostVm)).thenReturn(webhook);
            when(webhookRepository.save(webhook)).thenReturn(webhook);
            when(webhookMapper.toWebhookDetailVm(webhook)).thenReturn(webhookDetailVm);

            WebhookDetailVm result = webhookService.create(webhookPostVm);

            assertThat(result).isEqualTo(webhookDetailVm);
            verify(webhookEventRepository, never()).saveAll(any());
        }

        @Test
        void create_WithEvents_ShouldSaveWebhookAndEvents() {
            EventVm eventVm = new EventVm();
            eventVm.setId(1L);
            webhookPostVm.setEvents(List.of(eventVm));

            when(webhookMapper.toCreatedWebhook(webhookPostVm)).thenReturn(webhook);
            when(webhookRepository.save(webhook)).thenReturn(webhook);

            Event mockEvent = mock(Event.class);
            when(eventRepository.findById(1L)).thenReturn(Optional.of(mockEvent));

            WebhookEvent webhookEvent = new WebhookEvent();
            when(webhookEventRepository.saveAll(anyList())).thenReturn(List.of(webhookEvent));

            when(webhookMapper.toWebhookDetailVm(webhook)).thenReturn(webhookDetailVm);

            WebhookDetailVm result = webhookService.create(webhookPostVm);

            assertThat(result).isEqualTo(webhookDetailVm);
            assertThat(webhook.getWebhookEvents()).hasSize(1);
            verify(webhookEventRepository).saveAll(anyList());
        }

        @Test
        void create_WhenEventNotFound_ShouldThrowNotFoundException() {
            EventVm eventVm = new EventVm();
            eventVm.setId(1L);
            webhookPostVm.setEvents(List.of(eventVm));

            when(webhookMapper.toCreatedWebhook(webhookPostVm)).thenReturn(webhook);
            when(webhookRepository.save(webhook)).thenReturn(webhook);

            when(eventRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> webhookService.create(webhookPostVm))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining(MessageCode.EVENT_NOT_FOUND);
        }
    }

    @Nested
    class UpdateWebhook {
        @Test
        void update_WithoutEvents_ShouldUpdateWebhookAndClearEvents() {
            webhookPostVm.setEvents(null);
            when(webhookRepository.findById(1L)).thenReturn(Optional.of(webhook));
            when(webhookMapper.toUpdatedWebhook(webhook, webhookPostVm)).thenReturn(webhook);

            webhookService.update(webhookPostVm, 1L);

            verify(webhookRepository).save(webhook);
            verify(webhookEventRepository).deleteAll(anyList());
            verify(webhookEventRepository, never()).saveAll(anyList());
        }

        @Test
        void update_WithEvents_ShouldUpdateWebhookAndEvents() {
            EventVm eventVm = new EventVm();
            eventVm.setId(2L);
            webhookPostVm.setEvents(List.of(eventVm));

            when(webhookRepository.findById(1L)).thenReturn(Optional.of(webhook));
            when(webhookMapper.toUpdatedWebhook(webhook, webhookPostVm)).thenReturn(webhook);

            when(eventRepository.findById(2L)).thenReturn(Optional.of(mock(Event.class)));

            webhookService.update(webhookPostVm, 1L);

            verify(webhookRepository).save(webhook);
            verify(webhookEventRepository).deleteAll(anyList());
            verify(webhookEventRepository).saveAll(anyList());
        }

        @Test
        void update_WhenWebhookNotFound_ShouldThrowNotFoundException() {
            when(webhookRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> webhookService.update(webhookPostVm, 1L))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining(MessageCode.WEBHOOK_NOT_FOUND);
        }
    }

    @Nested
    class DeleteWebhook {
        @Test
        void delete_WhenExists_ShouldDeleteWebhookAndEvents() {
            when(webhookRepository.existsById(1L)).thenReturn(true);

            webhookService.delete(1L);

            verify(webhookEventRepository).deleteByWebhookId(1L);
            verify(webhookRepository).deleteById(1L);
        }

        @Test
        void delete_WhenNotExists_ShouldThrowNotFoundException() {
            when(webhookRepository.existsById(1L)).thenReturn(false);

            assertThatThrownBy(() -> webhookService.delete(1L))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining(MessageCode.WEBHOOK_NOT_FOUND);
        }
    }

    @Nested
    class NotifyWebhook {

        private ObjectMapper objectMapper = new ObjectMapper();

        @Test
        void notifyToWebhook_WhenFound_ShouldNotifyAndSetStatus() throws Exception {
            JsonNode payloadNode = objectMapper.readTree("{\"key\":\"value\"}");

            WebhookEventNotificationDto dto = WebhookEventNotificationDto.builder()
                    .notificationId(1L)
                    .url("http://callback.com")
                    .secret("secret_key")
                    .payload(payloadNode)
                    .build();

            WebhookEventNotification notification = new WebhookEventNotification();
            when(webhookEventNotificationRepository.findById(1L)).thenReturn(Optional.of(notification));

            webhookService.notifyToWebhook(dto);

            verify(webHookApi).notify(dto.getUrl(), dto.getSecret(), dto.getPayload());
            assertThat(notification.getNotificationStatus()).isEqualTo(NotificationStatus.NOTIFIED);
            verify(webhookEventNotificationRepository).save(notification);
        }

        @Test
        void notifyToWebhook_WhenNotFound_ShouldThrowException() throws Exception {
            JsonNode payloadNode = objectMapper.readTree("{\"key\":\"value\"}");

            WebhookEventNotificationDto dto = WebhookEventNotificationDto.builder()
                    .notificationId(1L)
                    .url("http://callback.com")
                    .secret("secret_key")
                    .payload(payloadNode)
                    .build();

            when(webhookEventNotificationRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> webhookService.notifyToWebhook(dto))
                    .isInstanceOf(NoSuchElementException.class);

            verify(webHookApi).notify(dto.getUrl(), dto.getSecret(), dto.getPayload());
        }
    }
}