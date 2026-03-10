package com.example.bookexchange.controllers;

import com.example.bookexchange.authentication.CurrentUser;
import com.example.bookexchange.dto.ExchangeHistoryDTO;
import com.example.bookexchange.dto.ExchangeHistoryDetailsDTO;
import com.example.bookexchange.services.HistoryService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
public class HistoryController {

    public static final String HISTORY_PATH = "/api/v1/history";
    public static final String HISTORY_PATH_EXCHANGE_ID = HISTORY_PATH + "/{exchangeId}";

    private final HistoryService historyService;

    @GetMapping(HISTORY_PATH)
    public Page<ExchangeHistoryDTO> getExchangeHistory(
            @CurrentUser Long userId,
            @RequestParam(value = "pageIndex", defaultValue = "0") Integer pageIndex,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize
    ) {
        return historyService.getUserExchangeHistory(userId, pageIndex, pageSize);
    }

    @GetMapping(HISTORY_PATH_EXCHANGE_ID)
    public ExchangeHistoryDetailsDTO getExchangeHistoryDetails(@CurrentUser Long userId, @PathVariable Long exchangeId) {
        return historyService.getUserExchangeHistoryDetails(userId, exchangeId);
    }
}
