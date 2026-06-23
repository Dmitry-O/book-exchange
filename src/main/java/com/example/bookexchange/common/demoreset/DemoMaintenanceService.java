package com.example.bookexchange.common.demoreset;

import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class DemoMaintenanceService {

    private final AtomicBoolean maintenanceMode = new AtomicBoolean(false);

    public boolean isMaintenanceMode() {
        return maintenanceMode.get();
    }

    public void enable() {
        maintenanceMode.set(true);
    }

    public void disable() {
        maintenanceMode.set(false);
    }
}
