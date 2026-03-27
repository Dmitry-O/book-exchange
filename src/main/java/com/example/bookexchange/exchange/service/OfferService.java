package com.example.bookexchange.exchange.service;

import com.example.bookexchange.common.dto.PageQueryDTO;
import com.example.bookexchange.common.result.Result;
import com.example.bookexchange.exchange.dto.ExchangeDTO;
import com.example.bookexchange.exchange.dto.ExchangeDetailsDTO;
import org.springframework.data.domain.Page;

public interface OfferService {

    Result<Page<ExchangeDTO>> getUserOffers(Long receiverUserId, PageQueryDTO queryDTO);

    Result<ExchangeDetailsDTO> getReceiverOfferDetails(Long senderUserId, Long exchangeId);

    Result<ExchangeDetailsDTO> approveUserOffer(Long receiverUserId, Long exchangeId, Long version);

    Result<ExchangeDetailsDTO> declineUserOffer(Long receiverUserId, Long exchangeId, Long version);
}
