package com.example.bookexchange.services;

import com.example.bookexchange.exception.NotFoundException;
import com.example.bookexchange.models.MessageKey;
import org.springframework.data.jpa.repository.JpaRepository;

public abstract class BaseServiceImpl<T, ID> implements BaseService<T, ID> {

    @Override
    public T findOrThrow(JpaRepository<T, ID> repo, ID id, MessageKey messageKey) {
        return repo.findById(id)
                .orElseThrow(() -> new NotFoundException(messageKey, id));
    }
}
