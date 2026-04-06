package com.example.bookexchange.common.audit.service;

import com.example.bookexchange.common.audit.model.AuditEvent;
import com.example.bookexchange.common.i18n.MessageKey;
import com.example.bookexchange.common.result.Result;
import com.example.bookexchange.support.unit.UnitTestDataFactory;
import com.example.bookexchange.user.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static com.example.bookexchange.support.unit.ResultAssertions.assertFailure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class VersionedEntityTransitionHelperTest {

    @Mock
    private AuditService auditService;

    @InjectMocks
    private VersionedEntityTransitionHelper helper;

    @Test
    void shouldReturnEntity_whenRequiredVersionMatches() {
        User user = UnitTestDataFactory.user(1L, "reader@example.com", "reader_one");
        user.setVersion(3L);

        Result<User> result = helper.requireVersion(
                user,
                3L,
                "USER_UPDATE",
                builder -> builder.actorId(user.getId()).actorEmail(user.getEmail())
        );

        assertThat(result.isSuccess()).isTrue();
        verify(auditService, never()).log(any());
    }

    @Test
    void shouldReturnConflict_whenRequiredVersionDiffers() {
        User user = UnitTestDataFactory.user(1L, "reader@example.com", "reader_one");
        user.setVersion(3L);

        Result<User> result = helper.requireVersion(
                user,
                2L,
                "USER_UPDATE",
                builder -> builder.actorId(user.getId()).actorEmail(user.getEmail())
        );

        assertFailure(result, MessageKey.SYSTEM_OPTIMISTIC_LOCK, HttpStatus.CONFLICT);

        ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditService).log(auditCaptor.capture());
        assertThat(auditCaptor.getValue().getReason()).isEqualTo("SYSTEM_OPTIMISTIC_LOCK");
    }
}
