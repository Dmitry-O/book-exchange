package com.example.bookexchange.services;

import com.example.bookexchange.models.MessageKey;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BaseService<T, ID> {

    T findOrThrow(JpaRepository<T, ID> repo, ID id, MessageKey messageKey);
}

