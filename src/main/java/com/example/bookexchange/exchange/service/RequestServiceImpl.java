package com.example.bookexchange.exchange.service;

import com.example.bookexchange.book.model.Book;
import com.example.bookexchange.common.audit.model.AuditEvent;
import com.example.bookexchange.common.audit.model.AuditResult;
import com.example.bookexchange.common.audit.service.AuditService;
import com.example.bookexchange.common.result.Result;
import com.example.bookexchange.common.result.ResultFactory;
import com.example.bookexchange.book.repository.BookRepository;
import com.example.bookexchange.common.i18n.MessageKey;
import com.example.bookexchange.exchange.dto.ExchangeDTO;
import com.example.bookexchange.exchange.dto.ExchangeDetailsDTO;
import com.example.bookexchange.exchange.dto.RequestCreateDTO;
import com.example.bookexchange.exchange.mapper.ExchangeMapper;
import com.example.bookexchange.exchange.model.Exchange;
import com.example.bookexchange.exchange.model.ExchangeContext;
import com.example.bookexchange.exchange.model.ExchangeStatus;
import com.example.bookexchange.exchange.repository.ExchangeRepository;
import com.example.bookexchange.user.repository.UserRepository;
import com.example.bookexchange.common.util.ETagUtil;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class RequestServiceImpl implements RequestService {

    private final ExchangeRepository exchangeRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final ExchangeMapper exchangeMapper;
    private final AuditService auditService;
    private final ExchangeTransitionHelper exchangeTransitionHelper;

    @Transactional
    @Override
    public Result<ExchangeDetailsDTO> createRequest(Long senderUserId, RequestCreateDTO dto) {
        ExchangeContext ctx = ExchangeContext.builder()
                .senderUserId(senderUserId)
                .receiverUserId(dto.getReceiverUserId())
                .senderBookId(dto.getSenderBookId())
                .receiverBookId(dto.getReceiverBookId())
                .build();

        return ResultFactory.ok(ctx)
                .flatMap(this::loadReceiverBook)
                .flatMap(this::validateReceiverBook)
                .flatMap(this::loadSenderBook)
                .flatMap(this::validateSenderBook)
                .flatMap(this::loadUsers)
                .flatMap(this::createExchange)
                .flatMap(this::formExchangeDetailsResponse);
    }

    @Transactional(readOnly = true)
    @Override
    public Result<ExchangeDetailsDTO> getSenderRequestDetails(Long senderUserId, Long exchangeId) {
        return ResultFactory.fromOptional(
                        exchangeRepository.findByIdAndSenderUserId(exchangeId, senderUserId),
                        MessageKey.EXCHANGE_NOT_FOUND
                )
                .flatMap(exchange ->
                        ResultFactory.fromRepository(
                                userRepository,
                                senderUserId,
                                MessageKey.USER_ACCOUNT_NOT_FOUND
                        )
                        .flatMap(u ->
                                ResultFactory.okETag(
                                        exchangeMapper.exchangeToExchangeDetailsDto(
                                                exchange,
                                                exchange.getReceiverUser().getNickname()
                                        ),
                                        ETagUtil.form(exchange)
                                )
                        )
                );
    }

    @Transactional(readOnly = true)
    @Override
    public Result<Page<ExchangeDTO>> getSenderRequests(Long senderUserId, Integer pageIndex, Integer pageSize) {
        Pageable pageable = PageRequest.of(pageIndex, pageSize);

        Page<Exchange> pendingExchangesPage = exchangeRepository.findBySenderUserIdAndStatus(senderUserId, ExchangeStatus.PENDING, pageable);

        Page<ExchangeDTO> page = pendingExchangesPage.map(exchangeMapper::exchangeToExchangeDto);

        return ResultFactory.ok(page);
    }

    @Transactional
    @Override
    public Result<ExchangeDetailsDTO> declineUserRequest(Long senderUserId, Long exchangeId, Long version) {
        return findSenderExchange(senderUserId, exchangeId)
                .flatMap(exchange ->
                        exchangeTransitionHelper.requirePendingVersion(
                                exchange,
                                version,
                                "DECLINE_USER_REQUEST",
                                senderUserId,
                                exchange.getSenderUser().getEmail(),
                                MessageKey.EXCHANGE_CANT_BE_DECLINED,
                                "EXCHANGE_CANT_BE_DECLINED"
                        )
                )
                .flatMap(exchange -> declineRequest(exchange, senderUserId));
    }

    private Result<ExchangeContext> loadReceiverBook(ExchangeContext ctx) {
        return ResultFactory.fromOptional(
                bookRepository.findByIdAndUserId(ctx.getReceiverBookId(), ctx.getReceiverUserId()),
                MessageKey.BOOK_NOT_FOUND
        ).map(book -> {
            ctx.setReceiverBook(book);

            return ctx;
        });
    }

    private Result<ExchangeContext> validateReceiverBook(ExchangeContext ctx) {
        Book receiverBook = ctx.getReceiverBook();

        if (receiverBook.getIsExchanged()) {
            auditService.log(AuditEvent.builder()
                    .action("CREATE_USER_REQUEST")
                    .result(AuditResult.FAILURE)
                    .actorId(ctx.getSenderUserId())
                    .reason("RECEIVER_BOOK_ALREADY_EXCHANGED")
                    .build()
            );

            return ResultFactory.error(MessageKey.BOOK_ALREADY_EXCHANGED, HttpStatus.BAD_REQUEST);
        }

        if (receiverBook.getIsGift()) {
            boolean exists = exchangeRepository
                    .findBySenderUserIdAndReceiverUserIdAndReceiverBookIdAndStatusNot(
                            ctx.getSenderUserId(),
                            ctx.getReceiverUserId(),
                            ctx.getReceiverBookId(),
                            ExchangeStatus.DECLINED
                    )
                    .isPresent();

            if (exists) {
                auditService.log(AuditEvent.builder()
                        .action("CREATE_USER_REQUEST")
                        .result(AuditResult.FAILURE)
                        .actorId(ctx.getSenderUserId())
                        .reason("RECEIVER_BOOK_EXCHANGE_ALREADY_EXISTS")
                        .build()
                );

                return ResultFactory.entityExists(MessageKey.BOOK_EXCHANGE_ALREADY_EXISTS);
            }
        }

        return ResultFactory.ok(ctx);
    }

    private Result<ExchangeContext> loadSenderBook(ExchangeContext ctx) {
        return ResultFactory.fromOptional(
                bookRepository.findByIdAndUserId(ctx.getSenderBookId(), ctx.getSenderUserId()),
                MessageKey.BOOK_NOT_FOUND
        ).map(book -> {
            ctx.setSenderBook(book);

            return ctx;
        });
    }

    private Result<ExchangeContext> validateSenderBook(ExchangeContext ctx) {
        Book senderBook = ctx.getSenderBook();
        Book receiverBook = ctx.getReceiverBook();

        if (senderBook.getIsExchanged()) {
            auditService.log(AuditEvent.builder()
                    .action("CREATE_USER_REQUEST")
                    .result(AuditResult.FAILURE)
                    .actorId(ctx.getSenderUserId())
                    .reason("SENDER_BOOK_ALREADY_EXCHANGED")
                    .build()
            );

            return ResultFactory.error(MessageKey.BOOK_ALREADY_EXCHANGED, HttpStatus.BAD_REQUEST);
        }

        if (ctx.getSenderUserId().equals(ctx.getReceiverUserId())) {
            auditService.log(AuditEvent.builder()
                    .action("CREATE_USER_REQUEST")
                    .result(AuditResult.FAILURE)
                    .actorId(ctx.getSenderUserId())
                    .reason("EXCHANGE_CANT_BE_WITH_YOURSELF")
                    .build()
            );

            return ResultFactory.error(MessageKey.EXCHANGE_CANT_BE_WITH_YOURSELF, HttpStatus.BAD_REQUEST);
        }

        if (senderBook.getId().equals(receiverBook.getId())) {
            auditService.log(AuditEvent.builder()
                    .action("CREATE_USER_REQUEST")
                    .result(AuditResult.FAILURE)
                    .actorId(ctx.getSenderUserId())
                    .reason("CANT_EXCHANGE_SAME_BOOK")
                    .build()
            );

            return ResultFactory.error(MessageKey.BOOK_CANT_EXCHANGE_SAME_BOOK, HttpStatus.BAD_REQUEST);
        }

        return ResultFactory.ok(ctx);
    }

    private Result<ExchangeContext> loadUsers(ExchangeContext ctx) {
        return ResultFactory.fromRepository(userRepository, ctx.getSenderUserId(), MessageKey.USER_ACCOUNT_NOT_FOUND)
                .flatMap(senderUser -> {
                    ctx.setSenderUser(senderUser);

                    return ResultFactory.fromRepository(
                                userRepository,
                                ctx.getReceiverUserId(),
                                MessageKey.USER_RECEIVER_NOT_FOUND
                            )
                            .map(receiverUser -> {
                                ctx.setReceiverUser(receiverUser);

                                return ctx;
                            });
                });
    }

    private Result<ExchangeContext> createExchange(ExchangeContext ctx) {
        Exchange exchange = new Exchange();

        exchange.setSenderUser(ctx.getSenderUser());
        exchange.setReceiverUser(ctx.getReceiverUser());
        exchange.setReceiverBook(ctx.getReceiverBook());
        exchange.setSenderBook(ctx.getSenderBook());
        exchange.setStatus(ExchangeStatus.PENDING);

        exchangeRepository.save(exchange);

        ctx.setExchange(exchange);

        auditService.log(AuditEvent.builder()
                .action("CREATE_USER_REQUEST")
                .result(AuditResult.SUCCESS)
                .actorId(ctx.getSenderUserId())
                .actorEmail(ctx.getSenderUser().getEmail())
                .build()
        );

        return ResultFactory.ok(ctx);
    }

    private Result<Exchange> findSenderExchange(Long senderUserId, Long exchangeId) {
        return ResultFactory.fromOptional(
                exchangeRepository.findByIdAndSenderUserId(exchangeId, senderUserId),
                MessageKey.EXCHANGE_NOT_FOUND
        );
    }

    private Result<ExchangeDetailsDTO> declineRequest(Exchange exchange, Long senderUserId) {
        return ResultFactory.fromRepository(
                userRepository,
                senderUserId,
                MessageKey.USER_ACCOUNT_NOT_FOUND
        ).flatMap(declinerUser -> {
            exchange.setStatus(ExchangeStatus.DECLINED);
            exchange.setDeclinerUser(declinerUser);

            exchangeRepository.save(exchange);
            logRequestSuccess("DECLINE_USER_REQUEST", senderUserId, exchange.getSenderUser().getEmail());

            return updatedExchangeDetails(exchange, MessageKey.EXCHANGE_DECLINED);
        });
    }

    private Result<ExchangeDetailsDTO> updatedExchangeDetails(Exchange exchange, MessageKey messageKey) {
        return ResultFactory.updated(
                exchangeMapper.exchangeToExchangeDetailsDto(
                        exchange,
                        exchange.getReceiverUser().getNickname()
                ),
                messageKey,
                ETagUtil.form(exchange)
        );
    }

    private void logRequestSuccess(String action, Long actorId, String actorEmail) {
        auditService.log(AuditEvent.builder()
                .action(action)
                .result(AuditResult.SUCCESS)
                .actorId(actorId)
                .actorEmail(actorEmail)
                .build()
        );
    }

    private Result<ExchangeDetailsDTO> formExchangeDetailsResponse(ExchangeContext ctx) {
        return ResultFactory.created(
                exchangeMapper.exchangeToExchangeDetailsDto(
                        ctx.getExchange(),
                        ctx.getReceiverUser().getNickname()
                ),
                MessageKey.EXCHANGE_CREATED,
                ETagUtil.form(ctx.getExchange())
        );
    }
}
