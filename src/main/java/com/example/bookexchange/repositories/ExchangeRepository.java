package com.example.bookexchange.repositories;

import com.example.bookexchange.models.Exchange;
import com.example.bookexchange.models.ExchangeStatus;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface ExchangeRepository extends CrudRepository<Exchange, Integer> {

    Exchange findByIdAndSenderUserId(Long exchangeId, Long senderUserId);

    List<Exchange> findBySenderUserIdAndStatus(Long senderUserId, ExchangeStatus status);

    List<Exchange> findByReceiverUserIdAndStatus(Long receiverUserId, ExchangeStatus status);

    Exchange findByIdAndReceiverUserId(Long exchangeId, Long receiverUserId);

    List<Exchange> findBySenderUserIdAndStatusNot(Long senderUserId, ExchangeStatus status);

    Optional<Exchange> findBySenderUserIdAndSenderBookIdAndReceiverUserIdAndReceiverBookIdAndStatusNot(Long senderUserId, Long senderBookId, Long receiverUserId, Long receiverBookId, ExchangeStatus status);

    Optional<Exchange> findBySenderUserIdAndReceiverUserIdAndReceiverBookIdAndStatusNot(Long senderUserId, Long receiverUserId, Long receiverBookId, ExchangeStatus status);

    List<Exchange> findByIdNotAndSenderBookIdAndStatus(Long exchangeId,  Long senderUserId, ExchangeStatus status);

    List<Exchange> findByIdNotAndReceiverBookIdAndStatus(Long exchangeId,  Long receiverUserId, ExchangeStatus status);
}
