package com.example.bookexchange.controllers;

import com.example.bookexchange.dto.ExchangeDTO;
import com.example.bookexchange.dto.ExchangeDetailsDTO;
import com.example.bookexchange.services.OfferService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
public class OfferController {

    public static final String OFFER_PATH = "/api/v1/offer";
    public static final String RECEIVER_USER_ID_PATH = "/{receiverUserId}";
    public static final String EXCHANGE_ID_PATH = "/{exchangeId}";
    public static final String OFFER_PATH_RECEIVER_USER_ID = OFFER_PATH + RECEIVER_USER_ID_PATH;
    public static final String OFFER_PATH_RECEIVER_USER_ID_EXCHANGE_ID = OFFER_PATH + RECEIVER_USER_ID_PATH + EXCHANGE_ID_PATH;
    public static final String OFFER_PATH_APPROVE_OFFER = OFFER_PATH + "/approve" + RECEIVER_USER_ID_PATH + EXCHANGE_ID_PATH;
    public static final String OFFER_PATH_DECLINE_OFFER = OFFER_PATH + "/decline" + RECEIVER_USER_ID_PATH + EXCHANGE_ID_PATH;

    private final OfferService offerService;

    @GetMapping(OFFER_PATH_RECEIVER_USER_ID)
    public Page<ExchangeDTO> getUserOffers(
            @PathVariable("receiverUserId") Long receiverUserId,
            @RequestParam(value = "pageIndex", defaultValue = "0") Integer pageIndex,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize
    ) {
        return offerService.getUserOffers(receiverUserId, pageIndex, pageSize);
    }

    @GetMapping(OFFER_PATH_RECEIVER_USER_ID_EXCHANGE_ID)
    public ExchangeDetailsDTO getUserOfferDetails(@PathVariable("receiverUserId") Long receiverUserId, @PathVariable("exchangeId") Long exchangeId) {
        return offerService.getReceiverOfferDetails(receiverUserId, exchangeId);
    }

    @PatchMapping(OFFER_PATH_APPROVE_OFFER)
    public String approveUserOffer(@PathVariable("receiverUserId") Long receiverUserId, @PathVariable("exchangeId") Long exchangeId ) {
        return offerService.approveUserOffer(receiverUserId, exchangeId);
    }

    @PatchMapping(OFFER_PATH_DECLINE_OFFER)
    public String declineUserOffer(@PathVariable("receiverUserId") Long receiverUserId, @PathVariable("exchangeId") Long exchangeId ) {
        return offerService.declineUserOffer(receiverUserId, exchangeId);
    }
}
