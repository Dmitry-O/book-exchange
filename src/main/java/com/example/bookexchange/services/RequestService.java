package com.example.bookexchange.services;

import com.example.bookexchange.dto.ExchangeDetailsDTO;
import com.example.bookexchange.dto.RequestCreateDTO;
import com.example.bookexchange.dto.ExchangeDTO;

import java.util.List;

public interface RequestService {

    ExchangeDTO createRequest(RequestCreateDTO dto);

    ExchangeDetailsDTO getSenderRequestDetails(Long senderUserId, Long exchangeId);

    List<ExchangeDTO> getSenderRequests(Long senderUserId);

    String declineUserRequest(Long senderUserId, Long exchangeId);
}
