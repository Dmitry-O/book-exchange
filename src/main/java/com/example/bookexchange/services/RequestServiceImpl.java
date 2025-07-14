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
import jakarta.persistence.EntityExistsException;
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
        Long senderBookId = dto.getSenderBookId();
        Long receiverUserId = dto.getReceiverUserId();
        Long receiverBookId = dto.getReceiverBookId();

        Book senderBook = null;

        Book receiverBook = bookRepository.findByIdAndUserId(receiverBookId, receiverUserId).orElseThrow(() -> new EntityNotFoundException("Das Buch mit ID " + receiverBookId + " oder der Benutzer mit ID " + receiverUserId + " wurde nicht gefunden"));

        if (receiverBook.getIsExchanged()) {
            throw new IllegalArgumentException("Das Buch mit ID " + receiverBookId + " wurde bereits umgetauscht");
        }

        Boolean isReceiverBookGift = receiverBook.getIsGift();

        if (isReceiverBookGift) {
            exchangeRepository.findBySenderUserIdAndReceiverUserIdAndReceiverBookIdAndStatusNot(senderUserId, receiverUserId, receiverBookId, ExchangeStatus.DECLINED).ifPresent(exchange -> { throw new EntityNotFoundException("Für dieses Buch besteht bereits ein Umtauschantrag"); });
        } else {
            senderBook = bookRepository.findByIdAndUserId(senderBookId, senderUserId).orElseThrow(() -> new EntityNotFoundException("Das Buch mit ID " + senderBookId + " oder mit user ID + " + senderUserId + " wurde nicht gefunden"));

            if (senderBook.getIsExchanged()) {
                throw new IllegalArgumentException("Das Buch mit ID " + senderBookId + " wurde bereits umgetauscht");
            }

            exchangeRepository.findBySenderUserIdAndSenderBookIdAndReceiverUserIdAndReceiverBookIdAndStatusNot(senderUserId, senderBookId, receiverUserId, receiverBookId, ExchangeStatus.DECLINED).ifPresent(exchange -> { throw new EntityNotFoundException("Der Umtauschantrag zwischen diesen Büchern besteht bereits"); });
        }

        if (senderUserId.equals(receiverUserId)) {
            throw new IllegalArgumentException("Der Benutzer kann keine Anfrage an sich selbst senden");
        }

        bookRepository.findByIdAndUserId(receiverBookId, senderUserId).ifPresent(book -> { throw new EntityExistsException("Der Absenderbenutzer mit ID " + senderUserId + " hat schon das Buch mit ID  " + receiverBookId + " in seiner Liste"); });

        User senderUser = userRepository.findById(senderUserId).orElseThrow(
                () -> new EntityNotFoundException("Der Absenderbenutzer mit ID " + senderUserId + " wurde nicht gefunden")
        );

        User receiverUser = userRepository.findById(receiverUserId).orElseThrow(
                () -> new EntityNotFoundException("Der Empfängerbenutzer mit ID " + receiverUserId + " wurde nicht gefunden")
        );

        Exchange exchange = new Exchange();

        exchange.setSenderUser(senderUser);
        exchange.setReceiverUser(receiverUser);
        exchange.setReceiverBook(receiverBook);
        exchange.setStatus(ExchangeStatus.PENDING);

        if (!isReceiverBookGift) {
            if (senderBookId.equals(receiverBookId)) {
                throw new IllegalArgumentException("Ein Umtausch desselben Buches ist nicht möglich");
            }

            bookRepository.findByIdAndUserId(senderBookId, receiverUserId).ifPresent(book ->  { throw new EntityNotFoundException("Der Empfängerbenutzer mit ID " + receiverUserId + " hat schon das Buch mit book ID + " + senderBookId + " in seiner Liste"); });

            exchange.setSenderBook(senderBook);
        }

        return ExchangeMapper.fromEntity(exchangeRepository.save(exchange));
    }

    @Override
    public ExchangeDetailsDTO getSenderRequestDetails(Long senderUserId, Long exchangeId) {
        Exchange exchange = exchangeRepository.findByIdAndSenderUserId(exchangeId, senderUserId);
        userRepository.findById(senderUserId).orElseThrow(() -> new EntityNotFoundException("Der Benutzer mit ID " + senderUserId + " wurde nicht gefunden"));

        return ExchangeMapper.fromEntityDetails(
                exchange,
                exchange.getSenderBook() != null ? BookMapper.fromEntity(exchange.getSenderBook()) : null,
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
