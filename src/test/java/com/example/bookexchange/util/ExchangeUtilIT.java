package com.example.bookexchange.util;

import com.example.bookexchange.exchange.dto.ExchangeDTO;
import com.example.bookexchange.exchange.dto.RequestCreateDTO;
import com.example.bookexchange.exchange.service.RequestService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ExchangeUtilIT {

    private final RequestService requestService;

    public Long createExchange(Long senderUserId, Long receiverUserId, Long senderBookId, Long receiverBookId) {
        RequestCreateDTO requestCreateDTO = RequestCreateDTO.builder()
                .receiverUserId(receiverUserId)
                .senderBookId(senderBookId)
                .receiverBookId(receiverBookId)
                .build();

        ExchangeDTO exchangeDTO = requestService.createRequest(senderUserId, requestCreateDTO);

        return exchangeDTO.getId();
    }
}
