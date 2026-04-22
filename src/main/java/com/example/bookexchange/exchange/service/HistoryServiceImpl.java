package com.example.bookexchange.exchange.service;

import com.example.bookexchange.book.model.Book;
import com.example.bookexchange.common.dto.PageQueryDTO;
import com.example.bookexchange.common.i18n.MessageKey;
import com.example.bookexchange.common.result.Result;
import com.example.bookexchange.common.result.ResultFactory;
import com.example.bookexchange.exchange.dto.ExchangeHistoryDTO;
import com.example.bookexchange.exchange.dto.ExchangeHistoryDetailsDTO;
import com.example.bookexchange.exchange.dto.ExchangeUpdateDTO;
import com.example.bookexchange.exchange.dto.ExchangeUpdateQueryDTO;
import com.example.bookexchange.exchange.dto.ExchangeUpdateReadStateDTO;
import com.example.bookexchange.exchange.mapper.ExchangeMapper;
import com.example.bookexchange.exchange.model.Exchange;
import com.example.bookexchange.exchange.model.ExchangeStatus;
import com.example.bookexchange.exchange.model.UserExchangeRole;
import com.example.bookexchange.exchange.repository.ExchangeRepository;
import com.example.bookexchange.exchange.util.ExchangeReadStateUtil;
import com.example.bookexchange.exchange.util.ExchangeUtil;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class HistoryServiceImpl implements HistoryService {

    private final ExchangeRepository exchangeRepository;
    private final ExchangeUtil exchangeUtil;
    private final ExchangeMapper exchangeMapper;

    @Transactional(readOnly = true)
    @Override
    public Result<Page<ExchangeHistoryDTO>> getUserExchangeHistory(Long userId, PageQueryDTO queryDTO) {
        Pageable pageable = PageRequest.of(
                queryDTO.getPageIndex(),
                queryDTO.getPageSize(),
                Sort.by(Sort.Direction.DESC, "updatedAt")
        );

        Page<ExchangeHistoryDTO> page = exchangeRepository
                .findUserExchangeHistory(userId, ExchangeStatus.PENDING, pageable)
                .map(exchange -> exchangeMapper.exchangeToExchangeHistoryDto(
                        exchange,
                        resolveUserExchangeRole(exchange, userId)
                ));

        return ResultFactory.ok(page);
    }

    @Transactional(readOnly = true)
    @Override
    public Result<Page<ExchangeUpdateDTO>> getExchangeUpdates(Long userId, ExchangeUpdateQueryDTO queryDTO) {
        Pageable pageable = PageRequest.of(
                queryDTO.getPageIndex(),
                queryDTO.getPageSize(),
                Sort.by(Sort.Direction.DESC, "updatedAt")
                        .and(Sort.by(Sort.Direction.DESC, "id"))
        );

        Page<ExchangeUpdateDTO> page = loadExchangeUpdates(userId, queryDTO.getReadState(), pageable)
                .map(exchange -> exchangeMapper.exchangeToExchangeUpdateDto(
                        exchange,
                        resolveUserExchangeRole(exchange, userId)
                ));

        return ResultFactory.ok(page);
    }

    @Transactional(readOnly = true)
    @Override
    public Result<Page<ExchangeUpdateDTO>> getUnreadExchangeUpdates(Long userId, PageQueryDTO queryDTO) {
        ExchangeUpdateQueryDTO updatesQueryDTO = new ExchangeUpdateQueryDTO();
        updatesQueryDTO.setPageIndex(queryDTO.getPageIndex());
        updatesQueryDTO.setPageSize(queryDTO.getPageSize());
        updatesQueryDTO.setReadState(ExchangeUpdateReadStateDTO.UNREAD);

        return getExchangeUpdates(userId, updatesQueryDTO);
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

    private UserExchangeRole resolveUserExchangeRole(Exchange exchange, Long userId) {
        return exchange.getSenderUser().getId().equals(userId)
                ? UserExchangeRole.SENDER
                : UserExchangeRole.RECEIVER;
    }

    private Page<Exchange> loadExchangeUpdates(Long userId, ExchangeUpdateReadStateDTO readState, Pageable pageable) {
        if (readState == null || readState == ExchangeUpdateReadStateDTO.ALL) {
            return exchangeRepository.findUpdatesForUser(userId, pageable);
        }

        if (readState == ExchangeUpdateReadStateDTO.READ) {
            return exchangeRepository.findUpdatesForUserByReadState(userId, true, pageable);
        }

        return exchangeRepository.findUpdatesForUserByReadState(userId, false, pageable);
    }

    private Exchange markAsReadBySender(Exchange exchange) {
        if (!exchange.getIsReadBySender()) {
            ExchangeReadStateUtil.markReadBySender(exchange);

            return exchangeRepository.saveAndFlush(exchange);
        }

        return exchange;
    }

    private Exchange markAsReadByReceiver(Exchange exchange) {
        if (!exchange.getIsReadByReceiver()) {
            ExchangeReadStateUtil.markReadByReceiver(exchange);

            return exchangeRepository.saveAndFlush(exchange);
        }

        return exchange;
    }

    private ExchangeHistoryDetailsDTO toExchangeHistoryDetailsDto(Exchange exchange, UserExchangeRole userRole) {
        return switch (userRole) {
            case SENDER -> exchangeMapper.exchangeToExchangeHistoryDetailsDto(
                    exchange,
                    exchange.getReceiverUser().getId(),
                    exchange.getReceiverUser().getNickname(),
                    resolveContactDetails(exchange, userRole),
                    userRole
            );
            case RECEIVER -> exchangeMapper.exchangeToExchangeHistoryDetailsDto(
                    exchange,
                    exchange.getSenderUser().getId(),
                    exchange.getSenderUser().getNickname(),
                    resolveContactDetails(exchange, userRole),
                    userRole
            );
        };
    }

    private String resolveContactDetails(Exchange exchange, UserExchangeRole userRole) {
        if (exchange.getStatus() != ExchangeStatus.APPROVED) {
            return null;
        }

        return switch (userRole) {
            case SENDER -> exchange.getReceiverBook().getContactDetails();
            case RECEIVER -> resolveContactDetails(exchange.getSenderBook());
        };
    }

    private String resolveContactDetails(Book book) {
        return book != null ? book.getContactDetails() : null;
    }
}
