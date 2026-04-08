package com.example.bookexchange.common.audit.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class SoftDeleteFilterHelper {

    private final EntityManager entityManager;

    public <T> T runWithoutDeletedFilter(Supplier<T> action) {
        Session session = entityManager.unwrap(Session.class);
        boolean filterWasEnabled = session.getEnabledFilter("deletedFilter") != null;

        if (filterWasEnabled) {
            session.disableFilter("deletedFilter");
        }

        try {
            return action.get();
        } finally {
            if (filterWasEnabled) {
                session.enableFilter("deletedFilter");
            }
        }
    }
}
