package com.example.bookexchange.services;

import com.example.bookexchange.dto.ExchangeDTO;
import com.example.bookexchange.models.Exchange;
import org.springframework.data.domain.Page;

public interface OfferService {

    Page<ExchangeDTO> getUserOffers(Long receiverUserId, Integer pageIndex, Integer pageSize);

    Exchange getReceiverOfferDetails(Long senderUserId, Long exchangeId);

    String approveUserOffer(Long receiverUserId, Long exchangeId, Long version);

    String declineUserOffer(Long receiverUserId, Long exchangeId, Long version);
}
