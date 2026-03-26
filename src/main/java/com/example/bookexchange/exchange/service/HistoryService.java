package com.example.bookexchange.exchange.service;

import com.example.bookexchange.common.result.Result;
import com.example.bookexchange.exchange.dto.ExchangeHistoryDTO;
import com.example.bookexchange.exchange.dto.ExchangeHistoryDetailsDTO;
import org.springframework.data.domain.Page;

public interface HistoryService {

    Result<Page<ExchangeHistoryDTO>> getUserExchangeHistory(Long userId, Integer pageIndex, Integer pageSize);

    Result<ExchangeHistoryDetailsDTO> getUserExchangeHistoryDetails(Long userId, Long exchangeId);
}
