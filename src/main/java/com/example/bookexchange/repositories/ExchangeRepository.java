package com.example.bookexchange.repositories;

import com.example.bookexchange.models.Exchange;
import com.example.bookexchange.models.ExchangeStatus;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ExchangeRepository extends CrudRepository<Exchange, Integer> {

    Exchange findByIdAndSenderUserId(Long exchangeId, Long senderUserId);

    List<Exchange> findBySenderUserIdAndStatus(Long senderUserId, ExchangeStatus status);

    List<Exchange> findByReceiverUserIdAndStatus(Long receiverUserId, ExchangeStatus status);

    Exchange findByIdAndReceiverUserId(Long exchangeId, Long receiverUserId);

    List<Exchange> findBySenderUserIdAndStatusNot(Long senderUserId, ExchangeStatus status);
}
