package com.example.bookexchange.controllers;

import com.example.bookexchange.authentication.CurrentUser;
import com.example.bookexchange.core.web.ResultResponseMapper;
import com.example.bookexchange.services.HistoryService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
public class HistoryController {

    private final HistoryService historyService;
    private final ResultResponseMapper responseMapper;

    public static final String HISTORY_PATH = "/api/v1/history";
    public static final String HISTORY_PATH_EXCHANGE_ID = HISTORY_PATH + "/{exchangeId}";

    @GetMapping(HISTORY_PATH)
    public ResponseEntity<?> getExchangeHistory(
            @CurrentUser Long userId,
            @RequestParam(value = "pageIndex", defaultValue = "0") Integer pageIndex,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize,
            HttpServletRequest request
    ) {
        return responseMapper.map(
                historyService.getUserExchangeHistory(
                        userId,
                        pageIndex,
                        pageSize
                ),
                request
        );
    }

    @GetMapping(HISTORY_PATH_EXCHANGE_ID)
    public ResponseEntity<?> getExchangeHistoryDetails(
            @CurrentUser Long userId,
            @PathVariable Long exchangeId,
            HttpServletRequest request
    ) {
        return responseMapper.map(
                historyService.getUserExchangeHistoryDetails(
                        userId,
                        exchangeId
                ),
                request
        );
    }
}
