package com.example.bookexchange.exchange.repository;

import com.example.bookexchange.exchange.model.Exchange;
import com.example.bookexchange.exchange.model.ExchangeStatus;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ExchangeRepository extends JpaRepository<Exchange, Long>, JpaSpecificationExecutor<Exchange> {

    @EntityGraph(attributePaths = {
            "senderUser",
            "receiverUser",
            "senderBook",
            "receiverBook",
            "declinerUser"
    })
    Optional<Exchange> findByIdAndSenderUserId(Long exchangeId, Long senderUserId);

    @EntityGraph(attributePaths = {
            "senderUser",
            "receiverUser",
            "senderBook",
            "receiverBook",
            "declinerUser"
    })
    Page<Exchange> findBySenderUserIdAndStatus(Long senderUserId, ExchangeStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {
            "senderUser",
            "receiverUser",
            "senderBook",
            "receiverBook",
            "declinerUser"
    })
    Page<Exchange> findByReceiverUserIdAndStatus(Long receiverUserId, ExchangeStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {
            "senderUser",
            "receiverUser",
            "senderBook",
            "receiverBook",
            "declinerUser"
    })
    Optional<Exchange> findByIdAndReceiverUserId(Long exchangeId, Long receiverUserId);

    @EntityGraph(attributePaths = {
            "senderUser",
            "receiverUser",
            "senderBook",
            "receiverBook",
            "declinerUser"
    })
    @Query("""
            SELECT e
            FROM Exchange e
            WHERE (e.senderUser.id = :userId OR e.receiverUser.id = :userId)
                AND e.status <> :status
            """
    )
    Page<Exchange> findUserExchangeHistory(
            @Param("userId") Long userId,
            @Param("status") ExchangeStatus status,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {
            "senderUser",
            "receiverUser",
            "senderBook",
            "receiverBook",
            "declinerUser"
    })
    @Query("""
            SELECT e
            FROM Exchange e
            WHERE e.senderUser.id = :userId
                OR e.receiverUser.id = :userId
            """
    )
    Page<Exchange> findUpdatesForUser(
            @Param("userId") Long userId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {
            "senderUser",
            "receiverUser",
            "senderBook",
            "receiverBook",
            "declinerUser"
    })
    @Query("""
            SELECT e
            FROM Exchange e
            WHERE (e.senderUser.id = :userId AND e.isReadBySender = :isRead)
                OR (e.receiverUser.id = :userId AND e.isReadByReceiver = :isRead)
            """
    )
    Page<Exchange> findUpdatesForUserByReadState(
            @Param("userId") Long userId,
            @Param("isRead") boolean isRead,
            Pageable pageable
    );

    Optional<Exchange> findBySenderUserIdAndReceiverUserIdAndReceiverBookIdAndStatusNot(Long senderUserId, Long receiverUserId, Long receiverBookId, ExchangeStatus status);

    Optional<Exchange> findBySenderUserIdAndReceiverUserIdAndSenderBookIdAndReceiverBookIdAndStatusNot(
            Long senderUserId,
            Long receiverUserId,
            Long senderBookId,
            Long receiverBookId,
            ExchangeStatus status
    );

    List<Exchange> findByIdNotAndSenderBookIdAndStatus(Long exchangeId,  Long senderUserId, ExchangeStatus status);

    List<Exchange> findByIdNotAndReceiverBookIdAndStatus(Long exchangeId,  Long receiverUserId, ExchangeStatus status);

    @Query("""
            SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END
            FROM Exchange e
            WHERE e.status IN :statuses
                AND (
                    (e.senderBook IS NOT NULL AND e.senderBook.id = :bookId)
                    OR e.receiverBook.id = :bookId
                )
            """)
    boolean existsByStatusInAndBookId(
            @Param("statuses") Set<ExchangeStatus> statuses,
            @Param("bookId") Long bookId
    );

    @Query("""
            SELECT DISTINCT e.senderBook.id
            FROM Exchange e
            WHERE e.status IN :statuses
                AND e.senderBook IS NOT NULL
                AND e.senderBook.id IN :bookIds
            """)
    List<Long> findLockedSenderBookIdsByStatusInAndBookIds(
            @Param("statuses") Set<ExchangeStatus> statuses,
            @Param("bookIds") Set<Long> bookIds
    );

    @Query("""
            SELECT DISTINCT e.receiverBook.id
            FROM Exchange e
            WHERE e.status IN :statuses
                AND e.receiverBook.id IN :bookIds
            """)
    List<Long> findLockedReceiverBookIdsByStatusInAndBookIds(
            @Param("statuses") Set<ExchangeStatus> statuses,
            @Param("bookIds") Set<Long> bookIds
    );

    @EntityGraph(attributePaths = {
            "senderUser",
            "receiverUser",
            "senderBook",
            "receiverBook",
            "declinerUser"
    })
    @Query("""
            SELECT e
            FROM Exchange e
            WHERE e.status = :status
                AND (
                    e.senderBook.id = :bookId
                    OR e.receiverBook.id = :bookId
                )
            """)
    List<Exchange> findByStatusAndBookId(
            @Param("status") ExchangeStatus status,
            @Param("bookId") Long bookId
    );

    @EntityGraph(attributePaths = {
            "senderUser",
            "receiverUser",
            "senderBook",
            "receiverBook",
            "declinerUser"
    })
    @Query("""
            SELECT e
            FROM Exchange e
            WHERE e.status = :status
                AND (e.senderUser.id = :userId OR e.receiverUser.id = :userId)
            """)
    List<Exchange> findByStatusAndParticipantUserId(
            @Param("status") ExchangeStatus status,
            @Param("userId") Long userId
    );

    @EntityGraph(attributePaths = {
            "senderUser",
            "receiverUser",
            "senderBook",
            "receiverBook",
            "declinerUser"
    })
    Page<Exchange> findByStatusIn(Set<ExchangeStatus> exchangeStatuses, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {
            "senderUser",
            "receiverUser",
            "senderBook",
            "receiverBook",
            "declinerUser"
    })
    Page<Exchange> findAll(@NonNull Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {
            "senderUser",
            "receiverUser",
            "senderBook",
            "receiverBook",
            "declinerUser"
    })
    Optional<Exchange> findById(Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Exchange e
            SET e.isReadBySender = true
            WHERE e.senderUser.id = :userId
                AND e.isReadBySender = false
            """)
    int markAllSenderUpdatesAsRead(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Exchange e
            SET e.isReadByReceiver = true
            WHERE e.receiverUser.id = :userId
                AND e.isReadByReceiver = false
            """)
    int markAllReceiverUpdatesAsRead(@Param("userId") Long userId);
}
