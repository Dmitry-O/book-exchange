package com.example.bookexchange.util;

import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class Helper {

    public void checkEntityVersion(Long entityVersion, Long incomingVersion) {
        if (!entityVersion.equals(incomingVersion)) {
            throw new OptimisticLockException();
        }
    }
}
