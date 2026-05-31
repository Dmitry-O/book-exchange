package com.example.bookexchange.user.repository;

import com.example.bookexchange.user.model.User;
import com.example.bookexchange.user.model.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface UserRepository extends JpaRepository<User, Long> {

    @EntityGraph(attributePaths = "roles")
    Optional<User> findByNickname(String nickname);

    @EntityGraph(attributePaths = "roles")
    Optional<User> findByEmail(String email);

    Page<User> findAll(Specification<User> specification, Pageable pageable);

    @EntityGraph(attributePaths = "roles")
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdWithRoles(@Param("id") Long id);

    @EntityGraph(attributePaths = "roles")
    @Query("SELECT DISTINCT u FROM User u JOIN u.roles role WHERE role IN :roles AND u.emailVerified = true")
    List<User> findVerifiedUsersByAnyRole(@Param("roles") Set<UserRole> roles);

    long countByDeletedAtIsNull();
}
