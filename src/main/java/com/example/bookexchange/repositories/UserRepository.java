package com.example.bookexchange.repositories;

import com.example.bookexchange.models.User;
import org.springframework.data.repository.CrudRepository;

public interface UserRepository extends CrudRepository<User, Integer> {
}
