package com.example.bookexchange.services;

import com.example.bookexchange.core.result.Result;
import com.example.bookexchange.dto.ExchangeHistoryDTO;
import com.example.bookexchange.dto.ExchangeHistoryDetailsDTO;
import org.springframework.data.domain.Page;

public interface HistoryService {

    Result<Page<ExchangeHistoryDTO>> getUserExchangeHistory(Long userId, Integer pageIndex, Integer pageSize);

    Result<ExchangeHistoryDetailsDTO> getUserExchangeHistoryDetails(Long userId, Long exchangeId);
}
