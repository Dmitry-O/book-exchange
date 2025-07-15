package com.example.bookexchange.controllers;

import com.example.bookexchange.dto.ExchangeDTO;
import com.example.bookexchange.dto.ExchangeDetailsDTO;
import com.example.bookexchange.services.OfferService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/offer")
public class OfferController {

    private final OfferService offerService;

    @GetMapping("/{receiverUserId}")
    public Page<ExchangeDTO> getUserOffers(
            @PathVariable("receiverUserId") Long receiverUserId,
            @RequestParam(value = "pageIndex", defaultValue = "0") Integer pageIndex,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize
    ) {
        return offerService.getUserOffers(receiverUserId, pageIndex, pageSize);
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
