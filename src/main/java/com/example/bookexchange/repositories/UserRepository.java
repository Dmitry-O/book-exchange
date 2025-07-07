package com.example.bookexchange.repositories;

import com.example.bookexchange.models.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends CrudRepository<User, Long> {
    @Query(value = "select * from app_user where nickname = :nickname", nativeQuery = true)
    Optional<User> findByNickname(@Param(value = "nickname") String nickname);
}
