package com.yas.webhook.model.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.yas.webhook.model.Webhook;
import com.yas.webhook.model.WebhookEvent;
import com.yas.webhook.model.viewmodel.webhook.EventVm;
import com.yas.webhook.model.viewmodel.webhook.WebhookDetailVm;
import com.yas.webhook.model.viewmodel.webhook.WebhookListGetVm;
import com.yas.webhook.model.viewmodel.webhook.WebhookPostVm;
import com.yas.webhook.model.viewmodel.webhook.WebhookVm;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class WebhookMapperTest {

    // Lấy instance do MapStruct sinh ra
    private final WebhookMapper mapper = Mappers.getMapper(WebhookMapper.class);

    private Webhook webhook;
    private WebhookPostVm webhookPostVm;

    @BeforeEach
    void setUp() {
        webhook = new Webhook();
        webhook.setId(1L);
        webhook.setPayloadUrl("http://old.url");
        webhook.setSecret("oldSecret");
        webhook.setIsActive(false);

        WebhookEvent event = new WebhookEvent();
        event.setEventId(100L);
        webhook.setWebhookEvents(List.of(event));

        webhookPostVm = new WebhookPostVm();
        webhookPostVm.setPayloadUrl("http://new.url");
        webhookPostVm.setSecret("newSecret");
        webhookPostVm.setIsActive(true);
    }

    @Test
    void toWebhookVm_ShouldMapCorrectly() {
        WebhookVm result = mapper.toWebhookVm(webhook);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getPayloadUrl()).isEqualTo("http://old.url");
        assertThat(result.getIsActive()).isFalse();
    }

    @Test
    void toWebhookEventVms_WithNullOrEmptyList_ShouldReturnEmptyList() {
        assertThat(mapper.toWebhookEventVms(null)).isEmpty();
        assertThat(mapper.toWebhookEventVms(Collections.emptyList())).isEmpty();
    }

    @Test
    void toWebhookEventVms_WithValidList_ShouldMapCorrectly() {
        WebhookEvent event1 = new WebhookEvent();
        event1.setEventId(10L);
        WebhookEvent event2 = new WebhookEvent();
        event2.setEventId(20L);

        List<EventVm> result = mapper.toWebhookEventVms(List.of(event1, event2));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(10L);
        assertThat(result.get(1).getId()).isEqualTo(20L);
    }

    @Test
    void toWebhookListGetVm_ShouldExtractPageInfoAndMapList() {
        Page<Webhook> page = new PageImpl<>(List.of(webhook), PageRequest.of(1, 10), 21);

        WebhookListGetVm result = mapper.toWebhookListGetVm(page, 1, 10);

        assertThat(result).isNotNull();
        assertThat(result.getPageNo()).isEqualTo(1);
        assertThat(result.getPageSize()).isEqualTo(10);
        assertThat(result.getTotalElements()).isEqualTo(21);
        assertThat(result.getTotalPages()).isEqualTo(3);
        assertThat(result.isLast()).isFalse();
        assertThat(result.getWebhooks()).hasSize(1);
        assertThat(result.getWebhooks().get(0).getId()).isEqualTo(1L);
    }

    @Test
    void toUpdatedWebhook_ShouldUpdateTargetAndIgnoreSpecificFields() {
        Webhook result = mapper.toUpdatedWebhook(webhook, webhookPostVm);

        assertThat(result).isNotNull();
        // ID phải được giữ nguyên (ignore = true)
        assertThat(result.getId()).isEqualTo(1L);
        // PayloadUrl, Secret, isActive được cập nhật từ PostVm
        assertThat(result.getPayloadUrl()).isEqualTo("http://new.url");
        assertThat(result.getSecret()).isEqualTo("newSecret");
        assertThat(result.getIsActive()).isTrue();
    }

    @Test
    void toCreatedWebhook_ShouldMapFieldsCorrectly() {
        Webhook result = mapper.toCreatedWebhook(webhookPostVm);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isNull(); // ID bị ignore
        assertThat(result.getPayloadUrl()).isEqualTo("http://new.url");
        assertThat(result.getSecret()).isEqualTo("newSecret");
        assertThat(result.getIsActive()).isTrue();
    }

    @Test
    void toWebhookDetailVm_ShouldMapEventsWithQualifiedNameAndIgnoreSecret() {
        WebhookDetailVm result = mapper.toWebhookDetailVm(webhook);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getPayloadUrl()).isEqualTo("http://old.url");

        // Secret bị đánh dấu ignore = true trong Mapping
        assertThat(result.getSecret()).isNull();

        // Kiểm tra custom mapping cho events
        assertThat(result.getEvents()).hasSize(1);
        assertThat(result.getEvents().get(0).getId()).isEqualTo(100L);
    }
}