package com.example.bookexchange.repositories;

import com.example.bookexchange.models.User;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface UserRepository extends CrudRepository<User, Long> {

    Optional<User> findByNickname(String nickname);
}
