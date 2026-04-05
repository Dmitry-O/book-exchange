package com.example.bookexchange.support.fixture;

import com.example.bookexchange.common.result.Failure;
import com.example.bookexchange.common.result.Result;
import com.example.bookexchange.common.result.Success;
import com.example.bookexchange.exchange.dto.ExchangeDetailsDTO;
import com.example.bookexchange.exchange.dto.RequestCreateDTO;
import com.example.bookexchange.exchange.service.RequestService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ExchangeFixtureSupport {

    private final RequestService requestService;

    public Long createExchange(Long senderUserId, Long receiverUserId, Long senderBookId, Long receiverBookId) {
        RequestCreateDTO requestCreateDTO = RequestCreateDTO.builder()
                .receiverUserId(receiverUserId)
                .senderBookId(senderBookId)
                .receiverBookId(receiverBookId)
                .build();

        ExchangeDetailsDTO exchangeDTO = unwrap(requestService.createRequest(senderUserId, requestCreateDTO));

        return exchangeDTO.getId();
    }

    private <T> T unwrap(Result<T> result) {
        if (result instanceof Success<T> success) {
            return success.body();
        }

        if (result instanceof Failure<T> failure) {
            throw new IllegalStateException(
                    "Expected success result, but got failure: "
                            + failure.messageKey()
                            + " (" + failure.status() + ")"
            );
        }

        throw new IllegalStateException("Unknown result type: " + result.getClass().getName());
    }
}
