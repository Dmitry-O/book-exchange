package com.example.bookexchange.services;

import com.example.bookexchange.core.result.Result;
import com.example.bookexchange.dto.ExchangeDetailsDTO;
import com.example.bookexchange.dto.RequestCreateDTO;
import com.example.bookexchange.dto.ExchangeDTO;
import org.springframework.data.domain.Page;

public interface RequestService {

    Result<ExchangeDetailsDTO> createRequest(Long senderUserId, RequestCreateDTO dto);

    Result<ExchangeDetailsDTO> getSenderRequestDetails(Long senderUserId, Long exchangeId);

    Result<Page<ExchangeDTO>> getSenderRequests(Long senderUserId, Integer pageIndex, Integer pageSize);

    Result<ExchangeDetailsDTO> declineUserRequest(Long senderUserId, Long exchangeId, Long version);
}
