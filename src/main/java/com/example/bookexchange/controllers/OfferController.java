package com.example.bookexchange.controllers;

import com.example.bookexchange.dto.ExchangeDTO;
import com.example.bookexchange.dto.ExchangeDetailsDTO;
import com.example.bookexchange.services.OfferService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/offer")
public class OfferController {

    private final OfferService offerService;

    @GetMapping("/{receiverUserId}")
    public List<ExchangeDTO> getUserOffers(@PathVariable("receiverUserId") Long receiverUserId) {
        return offerService.getUserOffers(receiverUserId);
    }

    @GetMapping("/{receiverUserId}/{exchangeId}")
    public ExchangeDetailsDTO getUserOfferDetails(@PathVariable("receiverUserId") Long receiverUserId, @PathVariable("exchangeId") Long exchangeId) {
        return offerService.getReceiverOfferDetails(receiverUserId, exchangeId);
    }

    @PatchMapping("/approve/{receiverUserId}/{exchangeId}")
    public String approveUserOffer(@PathVariable("receiverUserId") Long receiverUserId, @PathVariable("exchangeId") Long exchangeId ) {
        return offerService.approveUserOffer(receiverUserId, exchangeId);
    }

    @PatchMapping("/decline/{receiverUserId}/{exchangeId}")
    public String declineUserOffer(@PathVariable("receiverUserId") Long receiverUserId, @PathVariable("exchangeId") Long exchangeId ) {
        return offerService.declineUserOffer(receiverUserId, exchangeId);
    }
}
