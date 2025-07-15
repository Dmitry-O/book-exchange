package com.example.bookexchange.services;

import com.example.bookexchange.dto.ExchangeDetailsDTO;
import com.example.bookexchange.dto.RequestCreateDTO;
import com.example.bookexchange.dto.ExchangeDTO;
import org.springframework.data.domain.Page;

public interface RequestService {

    ExchangeDTO createRequest(RequestCreateDTO dto);

    ExchangeDetailsDTO getSenderRequestDetails(Long senderUserId, Long exchangeId);

    Page<ExchangeDTO> getSenderRequests(Long senderUserId, Integer pageIndex, Integer pageSize);

    String declineUserRequest(Long senderUserId, Long exchangeId);
}
