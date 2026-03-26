package com.example.bookexchange.exchange.service;

import com.example.bookexchange.common.audit.model.AuditEvent;
import com.example.bookexchange.common.audit.model.AuditResult;
import com.example.bookexchange.common.audit.service.AuditService;
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

    public Result<Exchange> requirePendingVersion(
            Exchange exchange,
            Long incomingVersion,
            String action,
            Long actorId,
            String actorEmail,
            MessageKey invalidStatusMessageKey,
            String invalidStatusReason
    ) {
        return validateVersion(exchange, incomingVersion, action, actorId, actorEmail)
                .flatMap(validExchange ->
                        validatePendingStatus(validExchange, action, actorId, actorEmail, invalidStatusMessageKey, invalidStatusReason)
                );
    }

    private Result<Exchange> validateVersion(
            Exchange exchange,
            Long incomingVersion,
            String action,
            Long actorId,
            String actorEmail
    ) {
        if (!exchange.getVersion().equals(incomingVersion)) {
            auditService.log(AuditEvent.builder()
                    .action(action)
                    .result(AuditResult.FAILURE)
                    .actorId(actorId)
                    .actorEmail(actorEmail)
                    .reason("SYSTEM_OPTIMISTIC_LOCK")
                    .detail("currentVersion", exchange.getVersion())
                    .detail("incomingVersion", incomingVersion)
                    .detail("currentExchangeState", exchange.getStatus())
                    .build()
            );

            return ResultFactory.error(MessageKey.SYSTEM_OPTIMISTIC_LOCK, HttpStatus.CONFLICT);
        }

        return ResultFactory.ok(exchange);
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
