package com.example.bookexchange.services;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BaseService<T, ID> {

    T findOrThrow(JpaRepository<T, ID> repo, ID id, String errorMessage);
}

