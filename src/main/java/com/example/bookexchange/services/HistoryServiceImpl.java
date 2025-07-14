package com.example.bookexchange.services;

import com.example.bookexchange.dto.ExchangeDTO;
import com.example.bookexchange.dto.ExchangeHistoryDetailsDTO;
import com.example.bookexchange.mapper.BookMapper;
import com.example.bookexchange.mapper.ExchangeMapper;
import com.example.bookexchange.models.Exchange;
import com.example.bookexchange.models.ExchangeStatus;
import com.example.bookexchange.repositories.ExchangeRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class HistoryServiceImpl implements HistoryService {

    private final ExchangeRepository exchangeRepository;

    @Override
    public List<ExchangeDTO> getUserExchangeHistory(Long userId) {
        List<Exchange> exchangesAsSender = exchangeRepository.findBySenderUserIdAndStatusNot(userId, ExchangeStatus.PENDING);
        List<Exchange> exchangesAsReceiver = exchangeRepository.findBySenderUserIdAndStatusNot(userId, ExchangeStatus.PENDING);

        List<ExchangeDTO> dto = new ArrayList<>();

        dto.addAll(
            exchangesAsSender.stream()
            .map(ExchangeMapper::fromEntity)
            .toList()
        );
        dto.addAll(
            exchangesAsReceiver.stream()
            .map(ExchangeMapper::fromEntity)
            .toList()
        );

        return dto;
    }

    @Override
    public ExchangeHistoryDetailsDTO getUserExchangeHistoryDetails(Long userId, Long exchangeId) {
        Exchange exchange;

        Optional<Exchange> exchangeAsSender = Optional.ofNullable(exchangeRepository.findByIdAndSenderUserId(exchangeId, userId));

        if (exchangeAsSender.isPresent()) {
            exchange = exchangeAsSender.get();

            return ExchangeMapper.fromEntityHistoryDetails(
                    exchange,
                    exchange.getSenderBook() != null ? BookMapper.fromEntity(exchange.getSenderBook()) : null,
                    BookMapper.fromEntity(exchange.getReceiverBook()),
                    exchange.getReceiverUser().getNickname(),
                    exchange.getReceiverBook().getContactDetails(),
                    "sender"
            );
        } else {
            Optional<Exchange> exchangeAsReceiver = Optional.ofNullable(exchangeRepository.findByIdAndReceiverUserId(exchangeId, userId));

            if (exchangeAsReceiver.isPresent()) {
                exchange = exchangeAsReceiver.get();

                return ExchangeMapper.fromEntityHistoryDetails(
                        exchange,
                        exchange.getSenderBook() != null ? BookMapper.fromEntity(exchange.getSenderBook()) : null,
                        BookMapper.fromEntity(exchange.getReceiverBook()),
                        exchange.getSenderUser().getNickname(),
                        exchange.getSenderBook() != null ? exchange.getSenderBook().getContactDetails() : null,
                        "receiver"
                );
            } else {
                throw new EntityNotFoundException("Der Umtauschantrag wurde nicht gefunden");
            }
        }
    }
}
