package com.example.bookexchange.exchange.util;

import com.example.bookexchange.common.result.Result;
import com.example.bookexchange.common.result.ResultFactory;
import com.example.bookexchange.common.i18n.MessageKey;
import com.example.bookexchange.exchange.model.UserExchangeRole;
import com.example.bookexchange.exchange.repository.ExchangeRepository;
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
