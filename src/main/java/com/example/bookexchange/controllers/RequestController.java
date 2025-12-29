package com.example.bookexchange.controllers;

import com.example.bookexchange.dto.ExchangeDetailsDTO;
import com.example.bookexchange.dto.RequestCreateDTO;
import com.example.bookexchange.dto.ExchangeDTO;
import com.example.bookexchange.services.RequestService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@AllArgsConstructor
@RestController
public class RequestController {

    public static final String REQUEST_PATH = "/api/v1/request";
    public static final String SENDER_USER_ID_PATH = "/{senderUserId}";
    public static final String EXCHANGE_ID_PATH = "/{exchangeId}";
    public static final String REQUEST_PATH_SENDER_USER_ID = REQUEST_PATH + SENDER_USER_ID_PATH;
    public static final String REQUEST_PATH_SENDER_USER_ID_EXCHANGE_ID = REQUEST_PATH + SENDER_USER_ID_PATH + EXCHANGE_ID_PATH;
    public static final String REQUEST_PATH_DECLINE_REQUEST = REQUEST_PATH + "/decline" + SENDER_USER_ID_PATH + EXCHANGE_ID_PATH;

    private final RequestService requestService;

    @PostMapping(REQUEST_PATH)
    public ResponseEntity createRequest(@RequestBody RequestCreateDTO dto) {
        ExchangeDTO savedRequest = requestService.createRequest(dto);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.LOCATION, REQUEST_PATH + "/" + dto.getSenderUserId() + "/" + savedRequest.getId());

        return new ResponseEntity(headers, HttpStatus.CREATED);
    }

    @GetMapping(REQUEST_PATH_SENDER_USER_ID_EXCHANGE_ID)
    public ExchangeDetailsDTO getUserRequestDetails(@PathVariable Long senderUserId, @PathVariable Long exchangeId) {
        return requestService.getSenderRequestDetails(senderUserId, exchangeId);
    }

    @GetMapping(REQUEST_PATH_SENDER_USER_ID)
    public Page<ExchangeDTO> getUserRequests(
            @PathVariable Long senderUserId,
            @RequestParam(value = "pageIndex", defaultValue = "0") Integer pageIndex,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize
    ) {
        return requestService.getSenderRequests(senderUserId, pageIndex, pageSize);
    }

    @PatchMapping(REQUEST_PATH_DECLINE_REQUEST)
    public ResponseEntity declineUserRequest(@PathVariable Long senderUserId, @PathVariable Long exchangeId) {
        requestService.declineUserRequest(senderUserId, exchangeId);

        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }
}
