package com.example.bookexchange.common.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserUpdateRepository extends JpaRepository<UserUpdate, Long> {

    @EntityGraph(attributePaths = "user")
    @Query("SELECT u FROM UserUpdate u WHERE u.user.id = :userId")
    Page<UserUpdate> findForUser(@Param("userId") Long userId, Pageable pageable);

    @EntityGraph(attributePaths = "user")
    @Query("SELECT u FROM UserUpdate u WHERE u.user.id = :userId AND u.isRead = :isRead")
    Page<UserUpdate> findForUserByReadState(
            @Param("userId") Long userId,
            @Param("isRead") boolean isRead,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "user")
    Optional<UserUpdate> findByIdAndUserId(Long id, Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE UserUpdate u
            SET u.isRead = true
            WHERE u.user.id = :userId
                AND u.isRead = false
            """)
    int markAllAsRead(@Param("userId") Long userId);
}
