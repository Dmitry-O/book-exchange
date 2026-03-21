package com.example.bookexchange.controllers;

import com.example.bookexchange.authentication.CurrentUser;
import com.example.bookexchange.core.web.ResultResponseMapper;
import com.example.bookexchange.services.OfferService;
import com.example.bookexchange.util.ParserUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
public class OfferController {

    private final OfferService offerService;
    private ParserUtil parserUtil;
    private final ResultResponseMapper responseMapper;

    public static final String OFFER_PATH = "/api/v1/offer";
    public static final String EXCHANGE_ID_PATH = "/{exchangeId}";
    public static final String OFFER_PATH_EXCHANGE_ID = OFFER_PATH + EXCHANGE_ID_PATH;
    public static final String OFFER_PATH_APPROVE_OFFER = OFFER_PATH + EXCHANGE_ID_PATH + "/approve";
    public static final String OFFER_PATH_DECLINE_OFFER = OFFER_PATH + EXCHANGE_ID_PATH + "/decline";

    @GetMapping(OFFER_PATH)
    public ResponseEntity<?> getUserOffers(
            @CurrentUser Long userId,
            @RequestParam(value = "pageIndex", defaultValue = "0") Integer pageIndex,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize,
            HttpServletRequest request
    ) {
        return responseMapper.map(
                offerService.getUserOffers(
                        userId,
                        pageIndex,
                        pageSize
                ),
                request
        );
    }

    @GetMapping(OFFER_PATH_EXCHANGE_ID)
    public ResponseEntity<?> getUserOfferDetails(
            @CurrentUser Long userId,
            @PathVariable Long exchangeId,
            HttpServletRequest request
    ) {
        return responseMapper.map(
                offerService.getReceiverOfferDetails(
                        userId,
                        exchangeId
                ),
                request
        );
    }

    @PatchMapping(OFFER_PATH_APPROVE_OFFER)
    public ResponseEntity<?> approveUserOffer(
            @CurrentUser Long userId,
            @PathVariable Long exchangeId,
            @RequestHeader("If-Match") String ifMatch,
            HttpServletRequest request
    ) {
        return responseMapper.map(
                offerService.approveUserOffer(
                        userId,
                        exchangeId,
                        parserUtil.ifMatchParser(ifMatch)
                ),
                request
        );
    }

    @PatchMapping(OFFER_PATH_DECLINE_OFFER)
    public ResponseEntity<?> declineUserOffer(
            @CurrentUser Long userId,
            @PathVariable Long exchangeId,
            @RequestHeader("If-Match") String ifMatch,
            HttpServletRequest request
    ) {
        return responseMapper.map(
                offerService.declineUserOffer(
                        userId,
                        exchangeId,
                        parserUtil.ifMatchParser(ifMatch)
                ),
                request
        );
    }
}
