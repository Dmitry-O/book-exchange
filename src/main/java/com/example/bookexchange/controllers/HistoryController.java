package com.example.bookexchange.controllers;

import com.example.bookexchange.dto.ExchangeHistoryDTO;
import com.example.bookexchange.dto.ExchangeHistoryDetailsDTO;
import com.example.bookexchange.services.HistoryService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/history")
public class HistoryController {

    private final HistoryService historyService;

    @GetMapping("/{userId}")
    public List<ExchangeHistoryDTO> getExchangeHistory(@PathVariable("userId") Long userId) {
        return historyService.getUserExchangeHistory(userId);
    }

    @GetMapping("/{userId}/{exchangeId}")
    public ExchangeHistoryDetailsDTO getExchangeHistoryDetails(@PathVariable("userId") Long userId, @PathVariable("exchangeId") Long exchangeId) {
        return historyService.getUserExchangeHistoryDetails(userId, exchangeId);
    }
}
