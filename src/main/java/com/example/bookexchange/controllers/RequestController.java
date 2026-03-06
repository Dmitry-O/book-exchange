package com.example.bookexchange.controllers;

import com.example.bookexchange.dto.ExchangeDetailsDTO;
import com.example.bookexchange.dto.RequestCreateDTO;
import com.example.bookexchange.dto.ExchangeDTO;
import com.example.bookexchange.models.User;
import com.example.bookexchange.services.RequestService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    public ResponseEntity<String> createRequest(@AuthenticationPrincipal User user, @Validated @RequestBody RequestCreateDTO dto) {
        ExchangeDTO savedRequest = requestService.createRequest(user.getId(), dto);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.LOCATION, REQUEST_PATH + "/" + savedRequest.getId());

        return new ResponseEntity<>(headers, HttpStatus.CREATED);
    }

    @GetMapping(REQUEST_PATH_EXCHANGE_ID)
    public ExchangeDetailsDTO getUserRequestDetails(@AuthenticationPrincipal User user, @PathVariable Long exchangeId) {
        return requestService.getSenderRequestDetails(user.getId(), exchangeId);
    }

    @GetMapping(REQUEST_PATH)
    public Page<ExchangeDTO> getUserRequests(
            @AuthenticationPrincipal User user,
            @RequestParam(value = "pageIndex", defaultValue = "0") Integer pageIndex,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize
    ) {
        return requestService.getSenderRequests(user.getId(), pageIndex, pageSize);
    }

    @PatchMapping(REQUEST_PATH_DECLINE_REQUEST)
    public ResponseEntity<String> declineUserRequest(@AuthenticationPrincipal User user, @PathVariable Long exchangeId) {
        requestService.declineUserRequest(user.getId(), exchangeId);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
