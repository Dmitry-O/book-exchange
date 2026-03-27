package com.example.bookexchange.exchange.repository;

import com.example.bookexchange.exchange.model.Exchange;
import com.example.bookexchange.exchange.model.ExchangeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
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
            """)
    Page<Exchange> findUserExchangeHistory(
            @Param("userId") Long userId,
            @Param("status") ExchangeStatus status,
            Pageable pageable
    );

    Optional<Exchange> findBySenderUserIdAndReceiverUserIdAndReceiverBookIdAndStatusNot(Long senderUserId, Long receiverUserId, Long receiverBookId, ExchangeStatus status);

    List<Exchange> findByIdNotAndSenderBookIdAndStatus(Long exchangeId,  Long senderUserId, ExchangeStatus status);

    List<Exchange> findByIdNotAndReceiverBookIdAndStatus(Long exchangeId,  Long receiverUserId, ExchangeStatus status);

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
    Page<Exchange> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {
            "senderUser",
            "receiverUser",
            "senderBook",
            "receiverBook",
            "declinerUser"
    })
    Optional<Exchange> findById(Long id);
}
