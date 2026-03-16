package com.example.bookexchange.services;

import com.example.bookexchange.exception.NotFoundException;
import org.springframework.data.jpa.repository.JpaRepository;

public abstract class BaseServiceImpl<T, ID> implements BaseService<T, ID> {

    @Override
    public T findOrThrow(JpaRepository<T, ID> repo, ID id, String errorMessage) {
        return repo.findById(id)
                .orElseThrow(() -> new NotFoundException(errorMessage));
    }
}
