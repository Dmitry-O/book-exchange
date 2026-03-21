package com.example.bookexchange.util;

import com.example.bookexchange.core.result.Result;
import com.example.bookexchange.core.result.ResultFactory;
import com.example.bookexchange.models.MessageKey;
import com.example.bookexchange.models.UserExchangeRole;
import com.example.bookexchange.repositories.ExchangeRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class ExchangeUtil {

    private final ExchangeRepository exchangeRepository;

    public Result<UserExchangeRole> identifyUserExchangeRole(Long userId, Long exchangeId) {
        if (exchangeRepository.findByIdAndSenderUserId(exchangeId, userId).isPresent()) {
            return ResultFactory.ok(UserExchangeRole.SENDER);
        } else if (exchangeRepository.findByIdAndReceiverUserId(exchangeId, userId).isPresent()) {
            return ResultFactory.ok(UserExchangeRole.RECEIVER);
        } else {
            return ResultFactory.notFound(MessageKey.EXCHANGE_NOT_FOUND);
        }
    }
}
