package com.example.bookexchange.services;

import com.example.bookexchange.dto.ExchangeDetailsDTO;
import com.example.bookexchange.dto.RequestCreateDTO;
import com.example.bookexchange.dto.ExchangeDTO;
import com.example.bookexchange.mapper.BookMapper;
import com.example.bookexchange.mapper.ExchangeMapper;
import com.example.bookexchange.models.Book;
import com.example.bookexchange.models.Exchange;
import com.example.bookexchange.models.ExchangeStatus;
import com.example.bookexchange.models.User;
import com.example.bookexchange.repositories.BookRepository;
import com.example.bookexchange.repositories.ExchangeRepository;
import com.example.bookexchange.repositories.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class RequestServiceImpl implements RequestService {

    private final ExchangeRepository exchangeRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;

    @Override
    public ExchangeDTO createRequest(RequestCreateDTO dto) {
        Long senderUserId = dto.getSenderUserId();
        Long receiverUserId = dto.getReceiverUserId();
        Long senderBookId = dto.getSenderBookId();
        Long receiverBookId = dto.getReceiverBookId();

        if (senderUserId.equals(receiverUserId)) {
            throw new IllegalArgumentException("Der Benutzer kann keine Anfrage an sich selbst senden");
        }

        if (senderBookId.equals(receiverBookId)) {
            throw new IllegalArgumentException("Ein Umtausch desselben Buches ist nicht möglich");
        }

        bookRepository.findByIdAndUserId(senderUserId, senderBookId).orElseThrow(() -> new EntityNotFoundException("Das Buch mit ID " + senderBookId + " oder mit user ID + " + senderUserId + " wurde nicht gefunden"));
        bookRepository.findByIdAndUserId(receiverUserId, receiverBookId).orElseThrow(() -> new EntityNotFoundException("Das Buch mit ID " + receiverBookId + " oder mit user ID + " + receiverUserId + " wurde nicht gefunden"));
        bookRepository.findByIdAndUserId(senderUserId, receiverBookId).ifPresent(book -> { throw new EntityNotFoundException("Der Absenderbenutzer mit ID " + senderUserId + " hat schon das Buch mit book ID + " + receiverBookId + " in seiner Liste"); });
        bookRepository.findByIdAndUserId(receiverUserId, senderBookId).ifPresent(book ->  { throw new EntityNotFoundException("Der Empfängerbenutzer mit ID " + receiverUserId + " hat schon das Buch mit book ID + " + senderBookId + " in seiner Liste"); });

        User senderUser = userRepository.findById(senderUserId).orElseThrow(
                () -> new EntityNotFoundException("Der Absenderbenutzer mit ID " + senderUserId + " wurde nicht gefunden")
        );

        User receiverUser = userRepository.findById(receiverUserId).orElseThrow(
                () -> new EntityNotFoundException("Der Empfängerbenutzer mit ID " + receiverUserId + " wurde nicht gefunden")
        );

        Book senderBook = bookRepository.findById(senderBookId).orElseThrow(
                () -> new EntityNotFoundException("Das Buch mit ID " + senderBookId + " des Absenderbenutzers mit ID " + senderUserId + " wurde nicht gefunden")
        );

        Book receiverBook = bookRepository.findById(receiverBookId).orElseThrow(
                () -> new EntityNotFoundException("Das Buch mit ID " + receiverBookId + " des Absenderbenutzers mit ID " + receiverUserId + " wurde nicht gefunden")
        );

        Exchange exchange = new Exchange();

        exchange.setSenderUser(senderUser);
        exchange.setReceiverUser(receiverUser);
        exchange.setSenderBook(senderBook);
        exchange.setReceiverBook(receiverBook);
        exchange.setStatus(ExchangeStatus.PENDING);

        return ExchangeMapper.fromEntity(exchangeRepository.save(exchange));
    }

    @Override
    public ExchangeDetailsDTO getSenderRequestDetails(Long senderUserId, Long exchangeId) {
        Exchange exchange = exchangeRepository.findByIdAndSenderUserId(exchangeId, senderUserId);
        userRepository.findById(senderUserId).orElseThrow(() -> new EntityNotFoundException("Der Benutzer mit ID " + senderUserId + " wurde nicht gefunden"));

        return ExchangeMapper.fromEntityDetails(
                exchange,
                BookMapper.fromEntity(exchange.getSenderBook()),
                BookMapper.fromEntity(exchange.getReceiverBook()),
                exchange.getReceiverUser().getNickname()
        );
    }

    @Override
    public List<ExchangeDTO> getSenderRequests(Long senderUserId) {
        List<Exchange> pendingExchanges = exchangeRepository.findBySenderUserIdAndStatus(senderUserId, ExchangeStatus.PENDING);

        return pendingExchanges.stream()
                .map(ExchangeMapper::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public String declineUserRequest(Long senderUserId, Long exchangeId) {
        Exchange exchange = exchangeRepository.findByIdAndSenderUserId(exchangeId, senderUserId);
        User declinerUser = userRepository.findById(senderUserId).orElseThrow(() -> new EntityNotFoundException("Der Benutzer mit ID " + senderUserId + " wurde nicht gefunden"));

        if (exchange.getStatus().equals(ExchangeStatus.PENDING)) {
            exchange.setStatus(ExchangeStatus.DECLINED);
            exchange.setDeclinerUser(declinerUser);
            exchangeRepository.save(exchange);

            return "Der Umtauschantrag wurde von Ihnen abgelehnt";
        } else {
            return "Der Umtauschantrag kann nicht storniert werden";
        }
    }
}
