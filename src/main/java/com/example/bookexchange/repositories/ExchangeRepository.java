package com.example.bookexchange.repositories;

import com.example.bookexchange.models.Exchange;
import com.example.bookexchange.models.ExchangeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface ExchangeRepository extends JpaRepository<Exchange, Long>, JpaSpecificationExecutor<Exchange> {

    Optional<Exchange> findByIdAndSenderUserId(Long exchangeId, Long senderUserId);

    Page<Exchange> findBySenderUserIdAndStatus(Long senderUserId, ExchangeStatus status, Pageable pageable);

    Page<Exchange> findByReceiverUserIdAndStatus(Long receiverUserId, ExchangeStatus status, Pageable pageable);

    Optional<Exchange> findByIdAndReceiverUserId(Long exchangeId, Long receiverUserId);

    List<Exchange> findBySenderUserIdAndStatusNot(Long senderUserId, ExchangeStatus status);

    List<Exchange> findByReceiverUserIdAndStatusNot(Long senderUserId, ExchangeStatus status);

    Optional<Exchange> findBySenderUserIdAndSenderBookIdAndReceiverUserIdAndReceiverBookIdAndStatusNot(Long senderUserId, Long senderBookId, Long receiverUserId, Long receiverBookId, ExchangeStatus status);

    Optional<Exchange> findBySenderUserIdAndReceiverUserIdAndReceiverBookIdAndStatusNot(Long senderUserId, Long receiverUserId, Long receiverBookId, ExchangeStatus status);

    List<Exchange> findByIdNotAndSenderBookIdAndStatus(Long exchangeId,  Long senderUserId, ExchangeStatus status);

    List<Exchange> findByIdNotAndReceiverBookIdAndStatus(Long exchangeId,  Long receiverUserId, ExchangeStatus status);

    Optional<Exchange> findById(Long exchangeId);
}
