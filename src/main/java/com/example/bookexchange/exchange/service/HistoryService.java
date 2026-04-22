package com.example.bookexchange.exchange.service;

import com.example.bookexchange.common.dto.PageQueryDTO;
import com.example.bookexchange.common.result.Result;
import com.example.bookexchange.exchange.dto.ExchangeHistoryDTO;
import com.example.bookexchange.exchange.dto.ExchangeHistoryDetailsDTO;
import com.example.bookexchange.exchange.dto.ExchangeUpdateDTO;
import com.example.bookexchange.exchange.dto.ExchangeUpdateQueryDTO;
import org.springframework.data.domain.Page;

public interface HistoryService {

    Result<Page<ExchangeHistoryDTO>> getUserExchangeHistory(Long userId, PageQueryDTO queryDTO);

    Result<ExchangeHistoryDetailsDTO> getUserExchangeHistoryDetails(Long userId, Long exchangeId);

    Result<Page<ExchangeUpdateDTO>> getExchangeUpdates(Long userId, ExchangeUpdateQueryDTO queryDTO);

    Result<Page<ExchangeUpdateDTO>> getUnreadExchangeUpdates(Long userId, PageQueryDTO queryDTO);
}
