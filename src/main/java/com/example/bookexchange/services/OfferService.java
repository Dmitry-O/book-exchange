package com.example.bookexchange.services;

import com.example.bookexchange.dto.ExchangeDTO;
import com.example.bookexchange.dto.ExchangeDetailsDTO;

import java.util.List;

public interface OfferService {

    List<ExchangeDTO> getUserOffers(Long receiverUserId);

    ExchangeDetailsDTO getReceiverOfferDetails(Long senderUserId, Long exchangeId);

    String approveUserOffer(Long receiverUserId, Long exchangeId);

    String declineUserOffer(Long receiverUserId, Long exchangeId);
}
