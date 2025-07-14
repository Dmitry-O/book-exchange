package com.example.bookexchange.services;

import com.example.bookexchange.dto.ExchangeHistoryDTO;
import com.example.bookexchange.dto.ExchangeHistoryDetailsDTO;

import java.util.List;

public interface HistoryService {

    List<ExchangeHistoryDTO> getUserExchangeHistory(Long userId);

    ExchangeHistoryDetailsDTO getUserExchangeHistoryDetails(Long userId, Long exchangeId);
}
