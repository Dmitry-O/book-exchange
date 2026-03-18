package com.example.bookexchange.services;

import com.example.bookexchange.dto.RequestCreateDTO;
import com.example.bookexchange.dto.ExchangeDTO;
import com.example.bookexchange.exception.BadRequestException;
import com.example.bookexchange.exception.EntityExistsException;
import com.example.bookexchange.exception.NotFoundException;
import com.example.bookexchange.mappers.ExchangeMapper;
import com.example.bookexchange.models.*;
import com.example.bookexchange.repositories.BookRepository;
import com.example.bookexchange.repositories.ExchangeRepository;
import com.example.bookexchange.repositories.UserRepository;
import com.example.bookexchange.util.Helper;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class RequestServiceImpl implements RequestService {

    private final ExchangeRepository exchangeRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final ExchangeMapper exchangeMapper;
    private final Helper helper;
    private final MessageService messageService;

    @Transactional
    @Override
    public String createRequest(Long senderUserId, RequestCreateDTO dto) {
        Long senderBookId = dto.getSenderBookId();
        Long receiverUserId = dto.getReceiverUserId();
        Long receiverBookId = dto.getReceiverBookId();

        Book senderBook = null;

        Book receiverBook = bookRepository.findByIdAndUserId(receiverBookId, receiverUserId).orElseThrow(() -> new NotFoundException(MessageKey.BOOK_NOT_FOUND));

        if (receiverBook.getIsExchanged()) {
            throw new BadRequestException(MessageKey.BOOK_ALREADY_EXCHANGED);
        }

        Boolean isReceiverBookGift = receiverBook.getIsGift();

        if (isReceiverBookGift) {
            exchangeRepository.findBySenderUserIdAndReceiverUserIdAndReceiverBookIdAndStatusNot(senderUserId, receiverUserId, receiverBookId, ExchangeStatus.DECLINED).ifPresent(exchange -> { throw new NotFoundException(MessageKey.BOOK_EXCHANGE_ALREADY_EXISTS); });
        } else {
            senderBook = bookRepository.findByIdAndUserId(senderBookId, senderUserId).orElseThrow(() -> new NotFoundException(MessageKey.BOOK_NOT_FOUND));

            if (senderBook.getIsExchanged()) {
                throw new BadRequestException(MessageKey.BOOK_ALREADY_EXCHANGED);
            }

            exchangeRepository.findBySenderUserIdAndSenderBookIdAndReceiverUserIdAndReceiverBookIdAndStatusNot(senderUserId, senderBookId, receiverUserId, receiverBookId, ExchangeStatus.DECLINED).ifPresent(exchange -> { throw new NotFoundException(MessageKey.EXCHANGE_BETWEEN_BOOKS_EXISTS); });
        }

        if (senderUserId.equals(receiverUserId)) {
            throw new BadRequestException(MessageKey.EXCHANGE_CANT_BE_WITH_YOURSELF);
        }

        bookRepository.findByIdAndUserId(receiverBookId, senderUserId).ifPresent(book -> { throw new EntityExistsException(MessageKey.BOOK_ALREADY_IN_YOUR_LIST); });

        User senderUser = userRepository.findById(senderUserId).orElseThrow(
            () -> new NotFoundException(MessageKey.USER_ACCOUNT_NOT_FOUND)
        );

        User receiverUser = userRepository.findById(receiverUserId).orElseThrow(
                () -> new NotFoundException(MessageKey.USER_RECEIVER_NOT_FOUND)
        );

        Exchange exchange = new Exchange();

        exchange.setSenderUser(senderUser);
        exchange.setReceiverUser(receiverUser);
        exchange.setReceiverBook(receiverBook);
        exchange.setStatus(ExchangeStatus.PENDING);

        if (!isReceiverBookGift) {
            if (senderBookId.equals(receiverBookId)) {
                throw new BadRequestException(MessageKey.BOOK_CANT_EXCHANGE_SAME_BOOK);
            }

            bookRepository.findByIdAndUserId(senderBookId, receiverUserId).ifPresent(book ->  { throw new NotFoundException(MessageKey.BOOK_ALREADY_IN_RECEIVERS_LIST); });

            exchange.setSenderBook(senderBook);
        }

        return messageService.getMessage(MessageKey.EXCHANGE_CREATED);
    }

    @Transactional(readOnly = true)
    @Override
    public Exchange getSenderRequestDetails(Long senderUserId, Long exchangeId) {
        Exchange exchange = exchangeRepository.findByIdAndSenderUserId(exchangeId, senderUserId).orElseThrow(() -> new NotFoundException(MessageKey.EXCHANGE_NOT_FOUND));
        userRepository.findById(senderUserId).orElseThrow(() -> new NotFoundException(MessageKey.USER_ACCOUNT_NOT_FOUND));

        return exchange;
    }

    @Transactional(readOnly = true)
    @Override
    public Page<ExchangeDTO> getSenderRequests(Long senderUserId, Integer pageIndex, Integer pageSize) {
        Pageable pageable = PageRequest.of(pageIndex, pageSize);

        Page<Exchange> pendingExchangesPage = exchangeRepository.findBySenderUserIdAndStatus(senderUserId, ExchangeStatus.PENDING, pageable);

        return pendingExchangesPage.map(exchangeMapper::exchangeToExchangeDto);
    }

    @Transactional
    @Override
    public String declineUserRequest(Long senderUserId, Long exchangeId, Long version) {
        Exchange exchange = exchangeRepository.findByIdAndSenderUserId(exchangeId, senderUserId).orElseThrow(() -> new NotFoundException(MessageKey.EXCHANGE_NOT_FOUND));
        User declinerUser = userRepository.findById(senderUserId).orElseThrow(() -> new NotFoundException(MessageKey.USER_ACCOUNT_NOT_FOUND));

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
