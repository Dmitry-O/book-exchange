package com.example.bookexchange.controllers;

import com.example.bookexchange.dto.ExchangeDTO;
import com.example.bookexchange.dto.ExchangeHistoryDetailsDTO;
import com.example.bookexchange.services.HistoryService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/history")
public class HistoryController {

    private final HistoryService historyService;

    @GetMapping("/{userId}")
    public List<ExchangeDTO> getExchangeHistory(@PathVariable("userId") Long userId) {
        return historyService.getUserExchangeHistory(userId);
    }

    @GetMapping("/{userId}/{exchangeId}")
    public ExchangeHistoryDetailsDTO getExchangeHistoryDetails(@PathVariable("userId") Long userId, @PathVariable("exchangeId") Long exchangeId) {
        return historyService.getUserExchangeHistoryDetails(userId, exchangeId);
    }
}
