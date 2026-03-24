package com.example.bookexchange.core.audit;

import com.example.bookexchange.authentication.RequestContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AuditServiceImpl implements AuditService {

    @Override
    public void log(AuditEvent event) {
        if (event.getResult() == AuditResult.ERROR) {
            log.error(
                    "[AUDIT]: result={} action={} requestId={} reason={} details={}",
                    event.getResult(),
                    event.getAction(),
                    RequestContext.getRequestId(),
                    event.getReason(),
                    event.getDetails()
            );

            return;
        }

        if (event.getResult() == AuditResult.FAILURE) {
            log.warn(
                    "[AUDIT]: result={} action={} actorId={} actorEmail={} requestId={} reason={} details={}",
                    event.getResult(),
                    event.getAction(),
                    event.getActorId(),
                    event.getActorEmail(),
                    RequestContext.getRequestId(),
                    event.getReason(),
                    event.getDetails()
            );

            return;
        }

        log.info(
                "[AUDIT]: result={} action={} actorId={} actorEmail={} requestId={} details={}",
                event.getResult(),
                event.getAction(),
                event.getActorId(),
                event.getActorEmail(),
                RequestContext.getRequestId(),
                event.getDetails()
        );
    }
}
