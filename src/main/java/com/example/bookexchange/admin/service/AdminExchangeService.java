package com.example.bookexchange.admin.service;

import com.example.bookexchange.admin.dto.ExchangeAdminDTO;
import com.example.bookexchange.common.result.Result;
import com.example.bookexchange.exchange.model.ExchangeStatus;
import org.springframework.data.domain.Page;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Set;

public interface AdminExchangeService {

    Result<Page<ExchangeAdminDTO>> findExchanges(Integer pageIndex, Integer pageSize, Set<ExchangeStatus> exchangeStatuses);

    Result<ExchangeAdminDTO> findExchangeById(UserDetails adminUser, Long exchangeId);
}
