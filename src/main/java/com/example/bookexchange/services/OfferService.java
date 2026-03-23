package com.example.bookexchange.services;

import com.example.bookexchange.core.result.Result;
import com.example.bookexchange.dto.ExchangeDTO;
import com.example.bookexchange.dto.ExchangeDetailsDTO;
import org.springframework.data.domain.Page;

public interface OfferService {

    Result<Page<ExchangeDTO>> getUserOffers(Long receiverUserId, Integer pageIndex, Integer pageSize);

    Result<ExchangeDetailsDTO> getReceiverOfferDetails(Long senderUserId, Long exchangeId);

    Result<ExchangeDetailsDTO> approveUserOffer(Long receiverUserId, Long exchangeId, Long version);

    Result<ExchangeDetailsDTO> declineUserOffer(Long receiverUserId, Long exchangeId, Long version);
}
