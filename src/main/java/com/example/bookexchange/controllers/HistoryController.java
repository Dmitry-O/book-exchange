package com.example.bookexchange.controllers;

import com.example.bookexchange.dto.ExchangeHistoryDTO;
import com.example.bookexchange.dto.ExchangeHistoryDetailsDTO;
import com.example.bookexchange.services.HistoryService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/history")
public class HistoryController {

    private final HistoryService historyService;

    @GetMapping("/{userId}")
    public Page<ExchangeHistoryDTO> getExchangeHistory(
            @PathVariable("userId") Long userId,
            @RequestParam(value = "pageIndex", defaultValue = "0") Integer pageIndex,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize
    ) {
        return historyService.getUserExchangeHistory(userId, pageIndex, pageSize);
    }

    @GetMapping("/{userId}/{exchangeId}")
    public ExchangeHistoryDetailsDTO getExchangeHistoryDetails(@PathVariable("userId") Long userId, @PathVariable("exchangeId") Long exchangeId) {
        return historyService.getUserExchangeHistoryDetails(userId, exchangeId);
    }
}
