package com.example.bookexchange.exchange.service;

import com.example.bookexchange.book.model.Book;
import com.example.bookexchange.book.search.BookSearchIndexService;
import com.example.bookexchange.common.audit.model.AuditEvent;
import com.example.bookexchange.common.audit.model.AuditResult;
import com.example.bookexchange.common.audit.service.AuditService;
import com.example.bookexchange.common.dto.PageQueryDTO;
import com.example.bookexchange.common.notification.NotificationDispatchService;
import com.example.bookexchange.common.result.Result;
import com.example.bookexchange.common.result.ResultFactory;
import com.example.bookexchange.common.i18n.MessageKey;
import com.example.bookexchange.exchange.model.Exchange;
import com.example.bookexchange.exchange.mapper.ExchangeMapper;
import com.example.bookexchange.exchange.repository.ExchangeRepository;
import com.example.bookexchange.exchange.model.ExchangeStatus;
import com.example.bookexchange.exchange.model.UserExchangeRole;
import com.example.bookexchange.exchange.dto.ExchangeDTO;
import com.example.bookexchange.exchange.dto.ExchangeDetailsDTO;
import com.example.bookexchange.user.repository.UserRepository;
import com.example.bookexchange.common.util.ETagUtil;
import com.example.bookexchange.exchange.util.ExchangeReadStateUtil;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor
public class OfferServiceImpl implements OfferService {

    private final ExchangeRepository exchangeRepository;
    private final UserRepository userRepository;
    private final ExchangeMapper exchangeMapper;
    private final AuditService auditService;
    private final ExchangeTransitionHelper exchangeTransitionHelper;
    private final BookSearchIndexService bookSearchIndexService;
    private final NotificationDispatchService notificationDispatchService;

    @Transactional(readOnly = true)
    @Override
    public Result<Page<ExchangeDTO>> getUserOffers(Long receiverUserId, PageQueryDTO queryDTO) {
        Pageable pageable = PageRequest.of(
                queryDTO.getPageIndex(),
                queryDTO.getPageSize(),
                Sort.by(Sort.Direction.DESC, "updatedAt")
        );

        Page<Exchange> exchangesPage = exchangeRepository.findByReceiverUserIdAndStatus(receiverUserId, ExchangeStatus.PENDING, pageable);

        Page<ExchangeDTO> page = exchangesPage.map(exchange ->
                exchangeMapper.exchangeToExchangeDto(exchange, UserExchangeRole.RECEIVER)
        );

        return ResultFactory.ok(page);
    }

    @Transactional
    @Override
    public Result<ExchangeDetailsDTO> getReceiverOfferDetails(Long receiverUserId, Long exchangeId) {
        return ResultFactory.fromOptional(
                exchangeRepository.findByIdAndReceiverUserId(
                        exchangeId,
                        receiverUserId
                ),
                MessageKey.EXCHANGE_NOT_FOUND
        )
                .flatMap(exchange -> {
                            Exchange persistedExchange = markAsReadByReceiver(exchange);

                            return ResultFactory.okETag(
                                    toUserExchangeDetailsDto(
                                            persistedExchange,
                                            persistedExchange.getSenderUser().getId(),
                                            persistedExchange.getSenderUser().getNickname()
                                    ),
                                    ETagUtil.form(persistedExchange)
                            );
                        }
                );
    }

    @Transactional
    @Override
    public Result<ExchangeDetailsDTO> approveUserOffer(Long receiverUserId, Long exchangeId, Long version) {
        return findReceiverExchange(receiverUserId, exchangeId)
                .flatMap(exchange ->
                        exchangeTransitionHelper.requirePendingVersion(
                                exchange,
                                version,
                                "APPROVE_USER_OFFER",
                                receiverUserId,
                                exchange.getReceiverUser().getEmail(),
                                MessageKey.EXCHANGE_CANT_BE_APPROVED,
                                "EXCHANGE_CANT_BE_APPROVED"
                        )
                )
                .flatMap(exchange -> approveOffer(exchange, receiverUserId));
    }

    @Transactional
    @Override
    public Result<ExchangeDetailsDTO> declineUserOffer(Long receiverUserId, Long exchangeId, Long version) {
        return findReceiverExchange(receiverUserId, exchangeId)
                .flatMap(exchange ->
                        exchangeTransitionHelper.requirePendingVersion(
                                exchange,
                                version,
                                "DECLINE_USER_OFFER",
                                receiverUserId,
                                exchange.getReceiverUser().getEmail(),
                                MessageKey.EXCHANGE_CANT_BE_DECLINED,
                                "EXCHANGE_CANT_BE_DECLINED"
                        )
                )
                .flatMap(exchange -> declineOffer(exchange, receiverUserId));
    }

    private Result<Exchange> findReceiverExchange(Long receiverUserId, Long exchangeId) {
        return ResultFactory.fromOptional(
                exchangeRepository.findByIdAndReceiverUserId(exchangeId, receiverUserId),
                MessageKey.EXCHANGE_NOT_FOUND
        );
    }

    private Result<ExchangeDetailsDTO> approveOffer(Exchange exchange, Long receiverUserId) {
        List<Book> exchangedBooks = markBooksAsExchanged(exchange);
        exchange.setStatus(ExchangeStatus.APPROVED);
        exchange.setAutoDeclined(Boolean.FALSE);
        ExchangeReadStateUtil.markUpdatedByReceiver(exchange);
        exchangeRepository.save(exchange);
        bookSearchIndexService.scheduleUpsertAll(exchangedBooks);

        List<Exchange> autoDeclinedExchanges = declineCompetingExchanges(exchange);
        logOfferSuccess("APPROVE_USER_OFFER", receiverUserId, exchange.getReceiverUser().getEmail());
        notificationDispatchService.sendExchangeApprovedNotifications(exchange);
        notificationDispatchService.sendExchangeAutoDeclinedNotifications(autoDeclinedExchanges);

        return updatedOfferDetails(exchange, MessageKey.EXCHANGE_APPROVED);
    }

    private Result<ExchangeDetailsDTO> declineOffer(Exchange exchange, Long receiverUserId) {
        return ResultFactory.fromRepository(
                userRepository,
                receiverUserId,
                MessageKey.USER_ACCOUNT_NOT_FOUND
        ).flatMap(declinerUser -> {
            exchange.setStatus(ExchangeStatus.DECLINED);
            exchange.setDeclinerUser(declinerUser);
            exchange.setAutoDeclined(Boolean.FALSE);
            ExchangeReadStateUtil.markUpdatedByReceiver(exchange);

            exchangeRepository.save(exchange);
            logOfferSuccess("DECLINE_USER_OFFER", receiverUserId, exchange.getReceiverUser().getEmail());
            notificationDispatchService.sendExchangeDeclinedByReceiverNotifications(exchange);

            return updatedOfferDetails(exchange, MessageKey.EXCHANGE_DECLINED);
        });
    }

    private List<Book> markBooksAsExchanged(Exchange exchange) {
        List<Book> exchangedBooks = new ArrayList<>();
        Book senderBook = exchange.getSenderBook();

        if (senderBook != null) {
            senderBook.setIsExchanged(true);
            exchangedBooks.add(senderBook);
        }

        exchange.getReceiverBook().setIsExchanged(true);
        exchangedBooks.add(exchange.getReceiverBook());

        return exchangedBooks;
    }

    private List<Exchange> declineCompetingExchanges(Exchange approvedExchange) {
        List<Exchange> competingExchanges = findCompetingExchanges(approvedExchange);

        competingExchanges.forEach(exchange -> {
            exchange.setStatus(ExchangeStatus.DECLINED);
            exchange.setDeclinerUser(approvedExchange.getReceiverUser());
            exchange.setAutoDeclined(Boolean.TRUE);
            ExchangeReadStateUtil.markUpdatedForBoth(exchange);
            exchangeRepository.save(exchange);
        });

        return competingExchanges;
    }

    private List<Exchange> findCompetingExchanges(Exchange approvedExchange) {
        Map<Long, Exchange> competingExchanges = new LinkedHashMap<>();

        Book senderBook = approvedExchange.getSenderBook();

        if (senderBook != null) {
            exchangeRepository.findByIdNotAndSenderBookIdAndStatus(
                    approvedExchange.getId(),
                    senderBook.getId(),
                    ExchangeStatus.PENDING
            ).forEach(exchange -> competingExchanges.put(exchange.getId(), exchange));
        }

        exchangeRepository.findByIdNotAndReceiverBookIdAndStatus(
                approvedExchange.getId(),
                approvedExchange.getReceiverBook().getId(),
                ExchangeStatus.PENDING
        ).forEach(exchange -> competingExchanges.put(exchange.getId(), exchange));

        return new ArrayList<>(competingExchanges.values());
    }

    private Result<ExchangeDetailsDTO> updatedOfferDetails(Exchange exchange, MessageKey messageKey) {
        return ResultFactory.updated(
                toUserExchangeDetailsDto(
                        exchange,
                        exchange.getSenderUser().getId(),
                        exchange.getSenderUser().getNickname()
                ),
                messageKey,
                ETagUtil.form(exchange)
        );
    }

    private void logOfferSuccess(String action, Long actorId, String actorEmail) {
        auditService.log(AuditEvent.builder()
                .action(action)
                .result(AuditResult.SUCCESS)
                .actorId(actorId)
                .actorEmail(actorEmail)
                .build()
        );
    }

    private Exchange markAsReadByReceiver(Exchange exchange) {
        if (!Boolean.TRUE.equals(exchange.getIsReadByReceiver())) {
            ExchangeReadStateUtil.markReadByReceiver(exchange);

            return exchangeRepository.saveAndFlush(exchange);
        }

        return exchange;
    }

    private ExchangeDetailsDTO toUserExchangeDetailsDto(Exchange exchange, Long otherUserId, String otherUserNickname) {
        ExchangeDetailsDTO dto = exchangeMapper.exchangeToExchangeDetailsDto(exchange, otherUserId, otherUserNickname);

        if (dto.getSenderBook() != null) {
            dto.getSenderBook().setContactDetails(null);
        }

        if (dto.getReceiverBook() != null) {
            dto.getReceiverBook().setContactDetails(null);
        }

        return dto;
    }
}
