package com.example.bookexchange.services;

import com.example.bookexchange.dto.ExchangeDTO;
import com.example.bookexchange.exception.BadRequestException;
import com.example.bookexchange.exception.NotFoundException;
import com.example.bookexchange.mappers.ExchangeMapper;
import com.example.bookexchange.models.*;
import com.example.bookexchange.repositories.ExchangeRepository;
import com.example.bookexchange.repositories.UserRepository;
import com.example.bookexchange.util.Helper;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@AllArgsConstructor
public class OfferServiceImpl implements OfferService {

    private final ExchangeRepository exchangeRepository;
    private final UserRepository userRepository;
    private final ExchangeMapper exchangeMapper;
    private final Helper helper;
    private final MessageService messageService;

    @Transactional(readOnly = true)
    @Override
    public Page<ExchangeDTO> getUserOffers(Long receiverUserId, Integer pageIndex, Integer pageSize) {
        Pageable pageable = PageRequest.of(pageIndex, pageSize);

        Page<Exchange> exchangesPage = exchangeRepository.findByReceiverUserIdAndStatus(receiverUserId, ExchangeStatus.PENDING, pageable);

        return exchangesPage.map(exchangeMapper::exchangeToExchangeDto);
    }

    @Transactional(readOnly = true)
    @Override
    public Exchange getReceiverOfferDetails(Long receiverUserId, Long exchangeId) {
        return exchangeRepository.findByIdAndReceiverUserId(exchangeId, receiverUserId).orElseThrow(() -> new NotFoundException(MessageKey.EXCHANGE_NOT_FOUND));
    }

    @Transactional
    @Override
    public String approveUserOffer(Long receiverUserId, Long exchangeId, Long version) {
        Exchange exchange = exchangeRepository.findByIdAndReceiverUserId(exchangeId, receiverUserId).orElseThrow(() -> new NotFoundException(MessageKey.EXCHANGE_NOT_FOUND));

        helper.checkEntityVersion(exchange.getVersion(), version);

        Book senderBook = exchange.getSenderBook();
        Book receiverBook = exchange.getReceiverBook();

        if (senderBook != null) {
            senderBook.setIsExchanged(true);
        }

        receiverBook.setIsExchanged(true);

        if (exchange.getStatus().equals(ExchangeStatus.PENDING)) {
            exchange.setStatus(ExchangeStatus.APPROVED);
            exchangeRepository.save(exchange);

            List<Exchange> senderExchanges = exchangeRepository.findByIdNotAndSenderBookIdAndStatus(exchangeId, exchange.getSenderBook().getId(), ExchangeStatus.PENDING);
            List<Exchange> receiverExchanges = exchangeRepository.findByIdNotAndReceiverBookIdAndStatus(exchangeId, exchange.getReceiverBook().getId(), ExchangeStatus.PENDING);

            senderExchanges.forEach(e->{
                e.setStatus(ExchangeStatus.DECLINED);
                exchangeRepository.save(e);
            });

            receiverExchanges.forEach(e->{
                e.setStatus(ExchangeStatus.DECLINED);
                exchangeRepository.save(e);
            });
        } else {
            throw new BadRequestException(MessageKey.EXCHANGE_CANT_BE_APPROVED);
        }

        return messageService.getMessage(MessageKey.EXCHANGE_APPROVED);
    }

    @Transactional
    @Override
    public String declineUserOffer(Long receiverUserId, Long exchangeId, Long version) {
        Exchange exchange = exchangeRepository.findByIdAndReceiverUserId(exchangeId, receiverUserId).orElseThrow(() -> new NotFoundException(MessageKey.EXCHANGE_NOT_FOUND));
        User declinerUser = userRepository.findById(receiverUserId).orElseThrow(() -> new NotFoundException(MessageKey.USER_ACCOUNT_NOT_FOUND));

        helper.checkEntityVersion(exchange.getVersion(), version);

        if (exchange.getStatus().equals(ExchangeStatus.PENDING)) {
            exchange.setStatus(ExchangeStatus.DECLINED);
            exchange.setDeclinerUser(declinerUser);
            exchangeRepository.save(exchange);
        } else {
            throw new BadRequestException(MessageKey.EXCHANGE_CANT_BE_DECLINED);
        }

        return messageService.getMessage(MessageKey.EXCHANGE_DECLINED);
    }
}
