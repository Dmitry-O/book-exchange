package com.example.bookexchange.common.demoreset;

import com.example.bookexchange.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.demo-reset.enabled", havingValue = "true")
public class DemoResetJob {

    private final DemoResetService demoResetService;

    @Scheduled(
            cron = "${app.demo-reset.cron:0 0 0 * * *}",
            zone = "${app.demo-reset.zone:UTC}"
    )
    public void resetDemoEnvironment() {
        Result<Void> result = demoResetService.resetDemoEnvironment();

        if (result.isFailure()) {
            log.warn("Scheduled demo reset finished with failure result");
        }
    }
}
