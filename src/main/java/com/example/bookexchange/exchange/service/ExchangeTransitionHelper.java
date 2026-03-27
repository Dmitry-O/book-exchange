package com.example.bookexchange.exchange.service;

import com.example.bookexchange.common.audit.model.AuditEvent;
import com.example.bookexchange.common.audit.model.AuditResult;
import com.example.bookexchange.common.audit.service.AuditService;
import com.example.bookexchange.common.audit.service.VersionedEntityTransitionHelper;
import com.example.bookexchange.common.i18n.MessageKey;
import com.example.bookexchange.common.result.Result;
import com.example.bookexchange.common.result.ResultFactory;
import com.example.bookexchange.exchange.model.Exchange;
import com.example.bookexchange.exchange.model.ExchangeStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ExchangeTransitionHelper {

    private final AuditService auditService;
    private final VersionedEntityTransitionHelper versionedEntityTransitionHelper;

    public Result<Exchange> requirePendingVersion(
            Exchange exchange,
            Long incomingVersion,
            String action,
            Long actorId,
            String actorEmail,
            MessageKey invalidStatusMessageKey,
            String invalidStatusReason
    ) {
        return versionedEntityTransitionHelper.requireVersion(
                        exchange,
                        incomingVersion,
                        action,
                        builder -> builder
                                .actorId(actorId)
                                .actorEmail(actorEmail)
                                .detail("currentExchangeState", exchange.getStatus())
                )
                .flatMap(validExchange ->
                        validatePendingStatus(validExchange, action, actorId, actorEmail, invalidStatusMessageKey, invalidStatusReason)
                );
    }

    private Result<Exchange> validatePendingStatus(
            Exchange exchange,
            String action,
            Long actorId,
            String actorEmail,
            MessageKey invalidStatusMessageKey,
            String invalidStatusReason
    ) {
        if (!exchange.getStatus().equals(ExchangeStatus.PENDING)) {
            auditService.log(AuditEvent.builder()
                    .action(action)
                    .result(AuditResult.FAILURE)
                    .actorId(actorId)
                    .actorEmail(actorEmail)
                    .reason(invalidStatusReason)
                    .detail("currentExchangeState", exchange.getStatus())
                    .build()
            );

            return ResultFactory.error(invalidStatusMessageKey, HttpStatus.BAD_REQUEST);
        }

        return ResultFactory.ok(exchange);
    }
}
