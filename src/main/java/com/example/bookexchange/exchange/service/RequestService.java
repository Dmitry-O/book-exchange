package com.example.bookexchange.exchange.service;

import com.example.bookexchange.common.result.Result;
import com.example.bookexchange.exchange.dto.ExchangeDTO;
import com.example.bookexchange.exchange.dto.ExchangeDetailsDTO;
import com.example.bookexchange.exchange.dto.RequestCreateDTO;
import org.springframework.data.domain.Page;

public interface RequestService {

    Result<ExchangeDetailsDTO> createRequest(Long senderUserId, RequestCreateDTO dto);

    Result<ExchangeDetailsDTO> getSenderRequestDetails(Long senderUserId, Long exchangeId);

    Result<Page<ExchangeDTO>> getSenderRequests(Long senderUserId, Integer pageIndex, Integer pageSize);

    Result<ExchangeDetailsDTO> declineUserRequest(Long senderUserId, Long exchangeId, Long version);
}
