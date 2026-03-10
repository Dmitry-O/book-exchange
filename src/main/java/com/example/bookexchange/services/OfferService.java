package com.example.bookexchange.services;

import com.example.bookexchange.dto.ExchangeDTO;
import com.example.bookexchange.dto.ExchangeDetailsDTO;
import org.springframework.data.domain.Page;

public interface OfferService {

    Page<ExchangeDTO> getUserOffers(Long receiverUserId, Integer pageIndex, Integer pageSize);

    ExchangeDetailsDTO getReceiverOfferDetails(Long senderUserId, Long exchangeId);

    String approveUserOffer(Long receiverUserId, Long exchangeId);

    String declineUserOffer(Long receiverUserId, Long exchangeId);
}
