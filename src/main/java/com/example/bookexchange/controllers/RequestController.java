package com.example.bookexchange.controllers;

import com.example.bookexchange.authentication.CurrentUser;
import com.example.bookexchange.core.web.ResultResponseMapper;
import com.example.bookexchange.dto.RequestCreateDTO;
import com.example.bookexchange.services.RequestService;
import com.example.bookexchange.util.ParserUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@AllArgsConstructor
@RestController
public class RequestController {

    private final RequestService requestService;
    private ParserUtil parserUtil;
    private final ResultResponseMapper responseMapper;

    public static final String REQUEST_PATH = "/api/v1/request";
    public static final String EXCHANGE_ID_PATH = "/{exchangeId}";
    public static final String REQUEST_PATH_EXCHANGE_ID = REQUEST_PATH + EXCHANGE_ID_PATH;
    public static final String REQUEST_PATH_DECLINE_REQUEST = REQUEST_PATH + EXCHANGE_ID_PATH + "/decline";

    @PostMapping(REQUEST_PATH)
    public ResponseEntity<?> createRequest(
            @CurrentUser Long userId,
            @Validated @RequestBody RequestCreateDTO dto,
            HttpServletRequest request
    ) {
        return responseMapper.map(
                requestService.createRequest(
                        userId,
                        dto
                ),
                request
        );
    }

    @GetMapping(REQUEST_PATH_EXCHANGE_ID)
    public ResponseEntity<?> getUserRequestDetails(
            @CurrentUser Long userId,
            @PathVariable Long exchangeId,
            HttpServletRequest request
    ) {
        return responseMapper.map(requestService.getSenderRequestDetails(userId, exchangeId), request);
    }

    @GetMapping(REQUEST_PATH)
    public ResponseEntity<?> getUserRequests(
            @CurrentUser Long userId,
            @RequestParam(value = "pageIndex", defaultValue = "0") Integer pageIndex,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize,
            HttpServletRequest request
    ) {
        return responseMapper.map(
                requestService.getSenderRequests(
                        userId,
                        pageIndex,
                        pageSize
                ),
                request
        );
    }

    @PatchMapping(REQUEST_PATH_DECLINE_REQUEST)
    public ResponseEntity<?> declineUserRequest(
            @CurrentUser Long userId,
            @PathVariable Long exchangeId,
            @RequestHeader("If-Match") String ifMatch,
            HttpServletRequest request
    ) {
        return responseMapper.map(
                requestService.declineUserRequest(
                        userId,
                        exchangeId,
                        parserUtil.ifMatchParser(ifMatch)
                ),
                request
        );
    }
}
