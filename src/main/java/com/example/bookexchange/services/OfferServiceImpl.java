package com.example.bookexchange.services;

import com.example.bookexchange.dto.ExchangeDTO;
import com.example.bookexchange.dto.ExchangeDetailsDTO;
import com.example.bookexchange.mapper.BookMapper;
import com.example.bookexchange.mapper.ExchangeMapper;
import com.example.bookexchange.models.Book;
import com.example.bookexchange.models.Exchange;
import com.example.bookexchange.models.ExchangeStatus;
import com.example.bookexchange.models.User;
import com.example.bookexchange.repositories.ExchangeRepository;
import com.example.bookexchange.repositories.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class OfferServiceImpl implements OfferService {

    private final ExchangeRepository exchangeRepository;
    private final UserRepository userRepository;

    @Override
    public List<ExchangeDTO> getUserOffers(Long receiverUserId) {
        List<Exchange> exchanges = exchangeRepository.findByReceiverUserIdAndStatus(receiverUserId, ExchangeStatus.PENDING);

        return exchanges.stream()
                .map(ExchangeMapper::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public ExchangeDetailsDTO getReceiverOfferDetails(Long receiverUserId, Long exchangeId) {
        Exchange exchange = exchangeRepository.findByIdAndReceiverUserId(exchangeId, receiverUserId).orElseThrow(() -> new EntityNotFoundException("Der Umtauschantrag wurde nicht gefunden"));

        return ExchangeMapper.fromEntityDetails(
                exchange,
                exchange.getSenderBook() != null ? BookMapper.fromEntity(exchange.getSenderBook()) : null,
                BookMapper.fromEntity(exchange.getReceiverBook()),
                exchange.getSenderUser().getNickname()
        );
    }

    @Override
    public String approveUserOffer(Long receiverUserId, Long exchangeId) {
        Exchange exchange = exchangeRepository.findByIdAndReceiverUserId(exchangeId, receiverUserId).orElseThrow(() -> new EntityNotFoundException("Der Umtauschantrag mit ID " + exchangeId + " und mit einer Empfängerbenutzer mit ID " + receiverUserId + " wurde nicht gefunden"));
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

            return "Der Umtauschantrag wurde genehmigt";
        } else {
            return "Der Umtauschantrag kann nicht bestätigt werden";
        }
    }

    @Override
    public String declineUserOffer(Long receiverUserId, Long exchangeId) {
        Exchange exchange = exchangeRepository.findByIdAndReceiverUserId(exchangeId, receiverUserId).orElseThrow(() -> new EntityNotFoundException("Der Umtauschantrag mit ID " + exchangeId + " und mit einer Empfängerbenutzer mit ID " + receiverUserId + " wurde nicht gefunden"));
        User declinerUser = userRepository.findById(receiverUserId).orElseThrow(() -> new EntityNotFoundException("Der Benutzer mit ID " + receiverUserId + " wurde nicht gefunden"));

        if (exchange.getStatus().equals(ExchangeStatus.PENDING)) {
            exchange.setStatus(ExchangeStatus.DECLINED);
            exchange.setDeclinerUser(declinerUser);
            exchangeRepository.save(exchange);

            return "Der Umtauschantrag wurde abgelehnt";
        } else {
            return "Der Umtauschantrag kann nicht storniert werden";
        }
    }
}
