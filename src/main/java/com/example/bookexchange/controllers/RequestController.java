package com.example.bookexchange.controllers;

import com.example.bookexchange.authentication.CurrentUser;
import com.example.bookexchange.dto.ApiMessage;
import com.example.bookexchange.dto.ExchangeDetailsDTO;
import com.example.bookexchange.dto.RequestCreateDTO;
import com.example.bookexchange.dto.ExchangeDTO;
import com.example.bookexchange.services.RequestService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@AllArgsConstructor
@RestController
public class RequestController {

    public static final String REQUEST_PATH = "/api/v1/request";
    public static final String EXCHANGE_ID_PATH = "/{exchangeId}";
    public static final String REQUEST_PATH_EXCHANGE_ID = REQUEST_PATH + EXCHANGE_ID_PATH;
    public static final String REQUEST_PATH_DECLINE_REQUEST = REQUEST_PATH + EXCHANGE_ID_PATH + "/decline";

    private final RequestService requestService;

    @PostMapping(REQUEST_PATH)
    public ResponseEntity<ApiMessage> createRequest(@CurrentUser Long userId, @Validated @RequestBody RequestCreateDTO dto) {
        return ResponseEntity.ok(new ApiMessage(requestService.createRequest(userId, dto)));
    }

    @GetMapping(REQUEST_PATH_EXCHANGE_ID)
    public ExchangeDetailsDTO getUserRequestDetails(@CurrentUser Long userId, @PathVariable Long exchangeId) {
        return requestService.getSenderRequestDetails(userId, exchangeId);
    }

    @GetMapping(REQUEST_PATH)
    public Page<ExchangeDTO> getUserRequests(
            @CurrentUser Long userId,
            @RequestParam(value = "pageIndex", defaultValue = "0") Integer pageIndex,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize
    ) {
        return requestService.getSenderRequests(userId, pageIndex, pageSize);
    }

    @PatchMapping(REQUEST_PATH_DECLINE_REQUEST)
    public ResponseEntity<ApiMessage> declineUserRequest(@CurrentUser Long userId, @PathVariable Long exchangeId) {
        return ResponseEntity.ok(new ApiMessage(requestService.declineUserRequest(userId, exchangeId)));
    }
}
