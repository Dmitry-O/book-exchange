package com.example.bookexchange.authentication;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class HibernateFilterAspect {

    private final EntityManager entityManager;

    @Before("@within(org.springframework.transaction.annotation.Transactional) || @annotation(org.springframework.transaction.annotation.Transactional)")
    public void enableSoftDeleteFilter() {
        Session session = entityManager.unwrap(Session.class);

        boolean includeDeleted = RequestContextHolder.isIncludeDeleted();

        if (!includeDeleted) {
            session.enableFilter("deletedFilter");
        }
    }
}
