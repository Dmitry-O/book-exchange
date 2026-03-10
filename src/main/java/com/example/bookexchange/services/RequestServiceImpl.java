package com.example.bookexchange.services;

import com.example.bookexchange.dto.ExchangeDetailsDTO;
import com.example.bookexchange.dto.RequestCreateDTO;
import com.example.bookexchange.dto.ExchangeDTO;
import com.example.bookexchange.exception.BadRequestException;
import com.example.bookexchange.exception.EntityExistsException;
import com.example.bookexchange.exception.NotFoundException;
import com.example.bookexchange.mappers.BookMapper;
import com.example.bookexchange.mappers.ExchangeMapper;
import com.example.bookexchange.models.Book;
import com.example.bookexchange.models.Exchange;
import com.example.bookexchange.models.ExchangeStatus;
import com.example.bookexchange.models.User;
import com.example.bookexchange.repositories.BookRepository;
import com.example.bookexchange.repositories.ExchangeRepository;
import com.example.bookexchange.repositories.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class RequestServiceImpl implements RequestService {

    private final ExchangeRepository exchangeRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final BookMapper bookMapper;

    @Override
    public String createRequest(Long senderUserId, RequestCreateDTO dto) {
        Long senderBookId = dto.getSenderBookId();
        Long receiverUserId = dto.getReceiverUserId();
        Long receiverBookId = dto.getReceiverBookId();

        Book senderBook = null;

        Book receiverBook = bookRepository.findByIdAndUserId(receiverBookId, receiverUserId).orElseThrow(() -> new NotFoundException("Das Buch mit ID " + receiverBookId + " oder der Benutzer mit ID " + receiverUserId + " wurde nicht gefunden"));

        if (receiverBook.getIsExchanged()) {
            throw new BadRequestException("Das Buch mit ID " + receiverBookId + " wurde bereits umgetauscht");
        }

        Boolean isReceiverBookGift = receiverBook.getIsGift();

        if (isReceiverBookGift) {
            exchangeRepository.findBySenderUserIdAndReceiverUserIdAndReceiverBookIdAndStatusNot(senderUserId, receiverUserId, receiverBookId, ExchangeStatus.DECLINED).ifPresent(exchange -> { throw new NotFoundException("Für dieses Buch besteht bereits ein Umtauschantrag"); });
        } else {
            senderBook = bookRepository.findByIdAndUserId(senderBookId, senderUserId).orElseThrow(() -> new NotFoundException("Das Buch mit ID " + senderBookId + " oder mit user ID + " + senderUserId + " wurde nicht gefunden"));

            if (senderBook.getIsExchanged()) {
                throw new BadRequestException("Das Buch mit ID " + senderBookId + " wurde bereits umgetauscht");
            }

            exchangeRepository.findBySenderUserIdAndSenderBookIdAndReceiverUserIdAndReceiverBookIdAndStatusNot(senderUserId, senderBookId, receiverUserId, receiverBookId, ExchangeStatus.DECLINED).ifPresent(exchange -> { throw new NotFoundException("Der Umtauschantrag zwischen diesen Büchern besteht bereits"); });
        }

        if (senderUserId.equals(receiverUserId)) {
            throw new BadRequestException("Der Benutzer kann keine Anfrage an sich selbst senden");
        }

        bookRepository.findByIdAndUserId(receiverBookId, senderUserId).ifPresent(book -> { throw new EntityExistsException("Der Absenderbenutzer mit ID " + senderUserId + " hat schon das Buch mit ID  " + receiverBookId + " in seiner Liste"); });

        User senderUser = userRepository.findById(senderUserId).orElseThrow(
                () -> new NotFoundException("Der Absenderbenutzer mit ID " + senderUserId + " wurde nicht gefunden")
        );

        User receiverUser = userRepository.findById(receiverUserId).orElseThrow(
                () -> new NotFoundException("Der Empfängerbenutzer mit ID " + receiverUserId + " wurde nicht gefunden")
        );

        Exchange exchange = new Exchange();

        exchange.setSenderUser(senderUser);
        exchange.setReceiverUser(receiverUser);
        exchange.setReceiverBook(receiverBook);
        exchange.setStatus(ExchangeStatus.PENDING);

        if (!isReceiverBookGift) {
            if (senderBookId.equals(receiverBookId)) {
                throw new BadRequestException("Ein Umtauschantrag desselben Buches ist nicht möglich");
            }

            bookRepository.findByIdAndUserId(senderBookId, receiverUserId).ifPresent(book ->  { throw new NotFoundException("Der Empfängerbenutzer mit ID " + receiverUserId + " hat schon das Buch mit book ID + " + senderBookId + " in seiner Liste"); });

            exchange.setSenderBook(senderBook);
        }

        return "Der Umtauschantrag wurde erstellt";
    }

    @Override
    public ExchangeDetailsDTO getSenderRequestDetails(Long senderUserId, Long exchangeId) {
        Exchange exchange = exchangeRepository.findByIdAndSenderUserId(exchangeId, senderUserId).orElseThrow(() -> new NotFoundException("Der Umtauschantrag mit ID " + exchangeId + " und mit einem Absenderbenutzer mit ID " + senderUserId + " wurde nicht gefunden"));
        userRepository.findById(senderUserId).orElseThrow(() -> new NotFoundException("Der Benutzer mit ID " + senderUserId + " wurde nicht gefunden"));

        return ExchangeMapper.fromEntityDetails(
                exchange,
                exchange.getSenderBook() != null ? bookMapper.bookToBookDto(exchange.getSenderBook()) : null,
                bookMapper.bookToBookDto(exchange.getReceiverBook()),
                exchange.getReceiverUser().getNickname()
        );
    }

    @Override
    public Page<ExchangeDTO> getSenderRequests(Long senderUserId, Integer pageIndex, Integer pageSize) {
        Pageable pageable = PageRequest.of(pageIndex, pageSize);

        Page<Exchange> pendingExchangesPage = exchangeRepository.findBySenderUserIdAndStatus(senderUserId, ExchangeStatus.PENDING, pageable);

        return pendingExchangesPage.map(ExchangeMapper::fromEntity);
    }

    @Override
    public String declineUserRequest(Long senderUserId, Long exchangeId) {
        Exchange exchange = exchangeRepository.findByIdAndSenderUserId(exchangeId, senderUserId).orElseThrow(() -> new NotFoundException("Der Umtauschantrag mit ID " + exchangeId + " und mit einem Absenderbenutzer mit ID " + senderUserId + " wurde nicht gefunden"));
        User declinerUser = userRepository.findById(senderUserId).orElseThrow(() -> new NotFoundException("Der Benutzer mit ID " + senderUserId + " wurde nicht gefunden"));

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
