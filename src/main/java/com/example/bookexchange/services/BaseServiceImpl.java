package com.example.bookexchange.services;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.repository.CrudRepository;

public abstract class BaseServiceImpl<T, ID> implements BaseService<T, ID> {

    @Override
    public T findOrThrow(CrudRepository<T, ID> repo, ID id, String errorMessage) {
        return repo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(errorMessage));
    }
}
