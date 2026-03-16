package com.example.bookexchange.controllers;

import com.example.bookexchange.authentication.CurrentUser;
import com.example.bookexchange.dto.ApiMessage;
import com.example.bookexchange.dto.ExchangeDTO;
import com.example.bookexchange.dto.ExchangeDetailsDTO;
import com.example.bookexchange.mappers.ExchangeMapper;
import com.example.bookexchange.models.Exchange;
import com.example.bookexchange.services.OfferService;
import com.example.bookexchange.util.ParserUtil;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
public class OfferController {

    private final OfferService offerService;
    private final ExchangeMapper exchangeMapper;
    private ParserUtil parserUtil;

    public static final String OFFER_PATH = "/api/v1/offer";
    public static final String EXCHANGE_ID_PATH = "/{exchangeId}";
    public static final String OFFER_PATH_EXCHANGE_ID = OFFER_PATH + EXCHANGE_ID_PATH;
    public static final String OFFER_PATH_APPROVE_OFFER = OFFER_PATH + EXCHANGE_ID_PATH + "/approve";
    public static final String OFFER_PATH_DECLINE_OFFER = OFFER_PATH + EXCHANGE_ID_PATH + "/decline";

    @GetMapping(OFFER_PATH)
    public Page<ExchangeDTO> getUserOffers(
            @CurrentUser Long userId,
            @RequestParam(value = "pageIndex", defaultValue = "0") Integer pageIndex,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize
    ) {
        return offerService.getUserOffers(userId, pageIndex, pageSize);
    }

    @GetMapping(OFFER_PATH_EXCHANGE_ID)
    public ResponseEntity<ExchangeDetailsDTO> getUserOfferDetails(@CurrentUser Long userId, @PathVariable Long exchangeId) {
        Exchange exchange = offerService.getReceiverOfferDetails(userId, exchangeId);

        return ResponseEntity
                .ok()
                .eTag("\"" + exchange.getVersion() + "\"")
                .body(exchangeMapper.exchangeToExchangeDetailsDto(exchange, exchange.getSenderUser().getNickname()));
    }

    @PatchMapping(OFFER_PATH_APPROVE_OFFER)
    public ResponseEntity<ApiMessage> approveUserOffer(
            @CurrentUser Long userId,
            @PathVariable Long exchangeId,
            @RequestHeader("If-Match") String ifMatch
    ) {
        return ResponseEntity.ok(new ApiMessage(offerService.approveUserOffer(userId, exchangeId, parserUtil.ifMatchParser(ifMatch))));
    }

    @PatchMapping(OFFER_PATH_DECLINE_OFFER)
    public ResponseEntity<ApiMessage> declineUserOffer(
            @CurrentUser Long userId,
            @PathVariable Long exchangeId,
            @RequestHeader("If-Match") String ifMatch
    ) {
        return ResponseEntity.ok(new ApiMessage(offerService.declineUserOffer(userId, exchangeId, parserUtil.ifMatchParser(ifMatch))));
    }
}
