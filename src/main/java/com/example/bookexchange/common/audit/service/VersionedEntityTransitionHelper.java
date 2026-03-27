package com.example.bookexchange.common.audit.service;

import com.example.bookexchange.common.audit.model.AuditEvent;
import com.example.bookexchange.common.audit.model.AuditResult;
import com.example.bookexchange.common.audit.model.VersionedEntity;
import com.example.bookexchange.common.i18n.MessageKey;
import com.example.bookexchange.common.result.Result;
import com.example.bookexchange.common.result.ResultFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class VersionedEntityTransitionHelper {

    private final AuditService auditService;

    public <T extends VersionedEntity> Result<T> requireVersion(
            T entity,
            Long incomingVersion,
            String action,
            Consumer<AuditEvent.AuditEventBuilder> auditCustomizer
    ) {
        if (!Objects.equals(entity.getVersion(), incomingVersion)) {
            AuditEvent.AuditEventBuilder auditBuilder = AuditEvent.builder()
                    .action(action)
                    .result(AuditResult.FAILURE)
                    .reason("SYSTEM_OPTIMISTIC_LOCK")
                    .detail("currentVersion", entity.getVersion())
                    .detail("incomingVersion", incomingVersion);

            auditCustomizer.accept(auditBuilder);
            auditService.log(auditBuilder.build());

            return ResultFactory.error(MessageKey.SYSTEM_OPTIMISTIC_LOCK, HttpStatus.CONFLICT);
        }

        return ResultFactory.ok(entity);
    }
}
