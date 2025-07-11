package com.example.bookexchange.services;

import com.example.bookexchange.dto.ExchangeDTO;
import com.example.bookexchange.dto.ExchangeHistoryDetailsDTO;

import java.util.List;

public interface HistoryService {
    List<ExchangeDTO> getUserExchangeHistory(Long userId);

    ExchangeHistoryDetailsDTO getUserExchangeHistoryDetails(Long userId, Long exchangeId);
}
