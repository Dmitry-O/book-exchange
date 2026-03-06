package com.example.bookexchange.controllers;

import com.example.bookexchange.dto.ExchangeDTO;
import com.example.bookexchange.dto.ExchangeDetailsDTO;
import com.example.bookexchange.models.User;
import com.example.bookexchange.services.OfferService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
public class OfferController {

    public static final String OFFER_PATH = "/api/v1/offer";
    public static final String EXCHANGE_ID_PATH = "/{exchangeId}";
    public static final String OFFER_PATH_EXCHANGE_ID = OFFER_PATH + EXCHANGE_ID_PATH;
    public static final String OFFER_PATH_APPROVE_OFFER = OFFER_PATH + EXCHANGE_ID_PATH + "/approve";
    public static final String OFFER_PATH_DECLINE_OFFER = OFFER_PATH + EXCHANGE_ID_PATH + "/decline";

    private final OfferService offerService;

    @GetMapping(OFFER_PATH)
    public Page<ExchangeDTO> getUserOffers(
            @AuthenticationPrincipal User user,
            @RequestParam(value = "pageIndex", defaultValue = "0") Integer pageIndex,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize
    ) {
        return offerService.getUserOffers(user.getId(), pageIndex, pageSize);
    }

    @GetMapping(OFFER_PATH_EXCHANGE_ID)
    public ExchangeDetailsDTO getUserOfferDetails(@AuthenticationPrincipal User user, @PathVariable Long exchangeId) {
        return offerService.getReceiverOfferDetails(user.getId(), exchangeId);
    }

    @PatchMapping(OFFER_PATH_APPROVE_OFFER)
    public ResponseEntity<String> approveUserOffer(@AuthenticationPrincipal User user, @PathVariable Long exchangeId ) {
        offerService.approveUserOffer(user.getId(), exchangeId);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PatchMapping(OFFER_PATH_DECLINE_OFFER)
    public ResponseEntity<String> declineUserOffer(@AuthenticationPrincipal User user, @PathVariable Long exchangeId ) {
        offerService.declineUserOffer(user.getId(), exchangeId);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
