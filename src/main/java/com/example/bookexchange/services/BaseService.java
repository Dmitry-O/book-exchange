package com.example.bookexchange.services;

import org.springframework.data.repository.CrudRepository;

public interface BaseService<T, ID> {

    T findOrThrow(CrudRepository<T, ID> repo, ID id, String errorMessage);
}

