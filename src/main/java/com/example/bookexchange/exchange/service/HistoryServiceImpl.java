package com.example.bookexchange.exchange.service;

import com.example.bookexchange.common.result.Result;
import com.example.bookexchange.common.result.ResultFactory;
import com.example.bookexchange.common.i18n.MessageKey;
import com.example.bookexchange.exchange.dto.ExchangeHistoryDTO;
import com.example.bookexchange.exchange.dto.ExchangeHistoryDetailsDTO;
import com.example.bookexchange.exchange.mapper.ExchangeMapper;
import com.example.bookexchange.exchange.model.Exchange;
import com.example.bookexchange.exchange.model.ExchangeStatus;
import com.example.bookexchange.exchange.model.UserExchangeRole;
import com.example.bookexchange.exchange.repository.ExchangeRepository;
import com.example.bookexchange.exchange.util.ExchangeUtil;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@AllArgsConstructor
public class HistoryServiceImpl implements HistoryService {

    private final ExchangeRepository exchangeRepository;
    private final ExchangeUtil exchangeUtil;
    private final ExchangeMapper exchangeMapper;

    @Transactional(readOnly = true)
    @Override
    public Result<Page<ExchangeHistoryDTO>> getUserExchangeHistory(Long userId, Integer pageIndex, Integer pageSize) {
        Pageable pageable = PageRequest.of(pageIndex, pageSize);

        List<Exchange> exchangesAsSender = exchangeRepository
                .findBySenderUserIdAndStatusNot(userId, ExchangeStatus.PENDING);
        List<Exchange> exchangesAsReceiver = exchangeRepository
                .findByReceiverUserIdAndStatusNot(userId, ExchangeStatus.PENDING);

        List<ExchangeHistoryDTO> allExchanges = new ArrayList<>();

        allExchanges.addAll(
                exchangesAsSender.stream()
                        .map(exchange -> exchangeMapper.exchangeToExchangeHistoryDto(
                                exchange,
                                UserExchangeRole.SENDER)
                        )
                        .toList()
        );

        allExchanges.addAll(
                exchangesAsReceiver.stream()
                        .map(exchange -> exchangeMapper.exchangeToExchangeHistoryDto(
                                exchange,
                                UserExchangeRole.RECEIVER)
                        )
                        .toList()
        );

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allExchanges.size());

        List<ExchangeHistoryDTO> paginatedList = start > end
                ? Collections.emptyList()
                : allExchanges.subList(start, end);

        Page<ExchangeHistoryDTO> page = new PageImpl<>(paginatedList, pageable, allExchanges.size());

        return ResultFactory.ok(page);
    }

    @Transactional
    @Override
    public Result<ExchangeHistoryDetailsDTO> getUserExchangeHistoryDetails(Long userId, Long exchangeId) {
        return exchangeUtil.identifyUserExchangeRole(userId, exchangeId)
                .flatMap(userRole ->
                        ResultFactory.fromRepository(
                                exchangeRepository,
                                exchangeId,
                                MessageKey.EXCHANGE_NOT_FOUND
                        )
                        .map(exchange -> markExchangeAsRead(exchange, userRole))
                        .map(exchange -> toExchangeHistoryDetailsDto(exchange, userRole))
                );
    }

    private Exchange markExchangeAsRead(Exchange exchange, UserExchangeRole userRole) {
        return switch (userRole) {
            case SENDER -> markAsReadBySender(exchange);
            case RECEIVER -> markAsReadByReceiver(exchange);
        };
    }

    private Exchange markAsReadBySender(Exchange exchange) {
        if (!exchange.getIsReadBySender()) {
            exchange.setIsReadBySender(true);

            return exchangeRepository.save(exchange);
        }

        return exchange;
    }

    private Exchange markAsReadByReceiver(Exchange exchange) {
        if (!exchange.getIsReadByReceiver()) {
            exchange.setIsReadByReceiver(true);

            return exchangeRepository.save(exchange);
        }

        return exchange;
    }

    private ExchangeHistoryDetailsDTO toExchangeHistoryDetailsDto(Exchange exchange, UserExchangeRole userRole) {
        return switch (userRole) {
            case SENDER -> exchangeMapper.exchangeToExchangeHistoryDetailsDto(
                    exchange,
                    exchange.getReceiverUser().getNickname(),
                    exchange.getReceiverBook().getContactDetails(),
                    userRole
            );
            case RECEIVER -> exchangeMapper.exchangeToExchangeHistoryDetailsDto(
                    exchange,
                    exchange.getSenderUser().getNickname(),
                    exchange.getSenderBook().getContactDetails(),
                    userRole
            );
        };
    }
}
