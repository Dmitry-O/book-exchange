package com.example.bookexchange.services;

import com.example.bookexchange.dto.ExchangeHistoryDTO;
import com.example.bookexchange.dto.ExchangeHistoryDetailsDTO;
import org.springframework.data.domain.Page;

public interface HistoryService {

    Page<ExchangeHistoryDTO> getUserExchangeHistory(Long userId, Integer pageIndex, Integer pageSize);

    ExchangeHistoryDetailsDTO getUserExchangeHistoryDetails(Long userId, Long exchangeId);
}
