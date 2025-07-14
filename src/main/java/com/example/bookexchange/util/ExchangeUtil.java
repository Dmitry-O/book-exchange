package com.example.bookexchange.util;

import com.example.bookexchange.models.Exchange;
import com.example.bookexchange.models.UserExchangeRole;
import com.example.bookexchange.repositories.ExchangeRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@AllArgsConstructor
public class ExchangeUtil {

    private final ExchangeRepository exchangeRepository;

    public UserExchangeRole identifyUserExchangeRole(Long userId, Long exchangeId) {
        Optional<Exchange> exchangeAsSender = exchangeRepository.findByIdAndSenderUserId(exchangeId, userId);

        if (exchangeAsSender.isPresent()) {
            return UserExchangeRole.SENDER;
        } else {
            Optional<Exchange> exchangeAsReceiver = exchangeRepository.findByIdAndReceiverUserId(exchangeId, userId);

            if (exchangeAsReceiver.isPresent()) {
                return UserExchangeRole.RECEIVER;
            } else {
                throw new EntityNotFoundException("Der Umtauschantrag wurde nicht gefunden");
            }
        }
    }
}
