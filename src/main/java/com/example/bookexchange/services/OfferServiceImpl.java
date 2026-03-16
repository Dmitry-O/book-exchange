package com.example.bookexchange.services;

import com.example.bookexchange.dto.ExchangeDTO;
import com.example.bookexchange.exception.BadRequestException;
import com.example.bookexchange.exception.NotFoundException;
import com.example.bookexchange.mappers.ExchangeMapper;
import com.example.bookexchange.models.Book;
import com.example.bookexchange.models.Exchange;
import com.example.bookexchange.models.ExchangeStatus;
import com.example.bookexchange.models.User;
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
        return exchangeRepository.findByIdAndReceiverUserId(exchangeId, receiverUserId).orElseThrow(() -> new NotFoundException("Der Umtauschantrag wurde nicht gefunden"));
    }

    @Transactional
    @Override
    public String approveUserOffer(Long receiverUserId, Long exchangeId, Long version) {
        Exchange exchange = exchangeRepository.findByIdAndReceiverUserId(exchangeId, receiverUserId).orElseThrow(() -> new NotFoundException("Der Umtauschantrag mit ID " + exchangeId + " und mit einer Empfängerbenutzer mit ID " + receiverUserId + " wurde nicht gefunden"));

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
            throw new BadRequestException("Der Umtauschantrag kann nicht bestätigt werden");
        }

        return "Der Umtauschantrag wurde bestätigt";
    }

    @Transactional
    @Override
    public String declineUserOffer(Long receiverUserId, Long exchangeId, Long version) {
        Exchange exchange = exchangeRepository.findByIdAndReceiverUserId(exchangeId, receiverUserId).orElseThrow(() -> new NotFoundException("Der Umtauschantrag mit ID " + exchangeId + " und mit einer Empfängerbenutzer mit ID " + receiverUserId + " wurde nicht gefunden"));
        User declinerUser = userRepository.findById(receiverUserId).orElseThrow(() -> new NotFoundException("Der Benutzer mit ID " + receiverUserId + " wurde nicht gefunden"));

        helper.checkEntityVersion(exchange.getVersion(), version);

        if (exchange.getStatus().equals(ExchangeStatus.PENDING)) {
            exchange.setStatus(ExchangeStatus.DECLINED);
            exchange.setDeclinerUser(declinerUser);
            exchangeRepository.save(exchange);
        } else {
            throw new BadRequestException("Der Umtauschantrag kann nicht abgelehnt werden");
        }

        return "Der Umtauschantrag wurde abgelehnt";
    }
}
