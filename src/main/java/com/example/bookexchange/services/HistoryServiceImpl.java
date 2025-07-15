package com.example.bookexchange.services;

import com.example.bookexchange.dto.ExchangeHistoryDTO;
import com.example.bookexchange.dto.ExchangeHistoryDetailsDTO;
import com.example.bookexchange.mapper.BookMapper;
import com.example.bookexchange.mapper.ExchangeMapper;
import com.example.bookexchange.models.Exchange;
import com.example.bookexchange.models.ExchangeStatus;
import com.example.bookexchange.models.UserExchangeRole;
import com.example.bookexchange.repositories.ExchangeRepository;
import com.example.bookexchange.util.ExchangeUtil;
import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Service
@AllArgsConstructor
public class HistoryServiceImpl implements HistoryService {

    private final ExchangeRepository exchangeRepository;
    private final ExchangeUtil exchangeUtil;

    @Override
    public Page<ExchangeHistoryDTO> getUserExchangeHistory(Long userId, Integer pageIndex, Integer pageSize) {
        Pageable pageable = PageRequest.of(pageIndex, pageSize);

        List<Exchange> exchangesAsSender = exchangeRepository
                .findBySenderUserIdAndStatusNot(userId, ExchangeStatus.PENDING);
        List<Exchange> exchangesAsReceiver = exchangeRepository
                .findByReceiverUserIdAndStatusNot(userId, ExchangeStatus.PENDING);

        List<ExchangeHistoryDTO> allExchanges = new ArrayList<>();

        allExchanges.addAll(
                exchangesAsSender.stream()
                        .map(exchange -> ExchangeMapper.fromEntityHistory(exchange, UserExchangeRole.SENDER))
                        .toList()
        );

        allExchanges.addAll(
                exchangesAsReceiver.stream()
                        .map(exchange -> ExchangeMapper.fromEntityHistory(exchange, UserExchangeRole.RECEIVER))
                        .toList()
        );

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allExchanges.size());

        List<ExchangeHistoryDTO> paginatedList = start > end
                ? Collections.emptyList()
                : allExchanges.subList(start, end);

        return new PageImpl<>(paginatedList, pageable, allExchanges.size());
    }

    @Override
    public ExchangeHistoryDetailsDTO getUserExchangeHistoryDetails(Long userId, Long exchangeId) {
        UserExchangeRole userRole = exchangeUtil.identifyUserExchangeRole(userId, exchangeId);
        Exchange exchange = exchangeRepository.findById(exchangeId).orElseThrow(() -> new EntityNotFoundException("Der Umtauschantrag wurde nicht gefunden"));

        if (Objects.equals(userRole, UserExchangeRole.SENDER)) {
            if (!exchange.getIsReadBySender()) {
                exchange.setIsReadBySender(true);

                exchangeRepository.save(exchange);
            }

            return ExchangeMapper.fromEntityHistoryDetails(
                    exchange,
                    exchange.getSenderBook() != null ? BookMapper.fromEntity(exchange.getSenderBook()) : null,
                    BookMapper.fromEntity(exchange.getReceiverBook()),
                    exchange.getReceiverUser().getNickname(),
                    exchange.getReceiverBook().getContactDetails(),
                    userRole
            );
        } else {
            if (Objects.equals(userRole, UserExchangeRole.RECEIVER)) {
                if (!exchange.getIsReadByReceiver()) {
                    exchange.setIsReadByReceiver(true);

                    exchangeRepository.save(exchange);
                }

                return ExchangeMapper.fromEntityHistoryDetails(
                        exchange,
                        exchange.getSenderBook() != null ? BookMapper.fromEntity(exchange.getSenderBook()) : null,
                        BookMapper.fromEntity(exchange.getReceiverBook()),
                        exchange.getSenderUser().getNickname(),
                        exchange.getSenderBook() != null ? exchange.getSenderBook().getContactDetails() : null,
                        userRole
                );
            } else {
                throw new EntityNotFoundException("Der Umtauschantrag wurde nicht gefunden");
            }
        }
    }
}
