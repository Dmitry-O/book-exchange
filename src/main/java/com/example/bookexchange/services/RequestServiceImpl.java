package com.example.bookexchange.services;

import com.example.bookexchange.core.audit.AuditEvent;
import com.example.bookexchange.core.audit.AuditResult;
import com.example.bookexchange.core.audit.AuditService;
import com.example.bookexchange.core.result.Result;
import com.example.bookexchange.core.result.ResultFactory;
import com.example.bookexchange.dto.ExchangeDetailsDTO;
import com.example.bookexchange.dto.RequestCreateDTO;
import com.example.bookexchange.dto.ExchangeDTO;
import com.example.bookexchange.mappers.ExchangeMapper;
import com.example.bookexchange.models.*;
import com.example.bookexchange.repositories.BookRepository;
import com.example.bookexchange.repositories.ExchangeRepository;
import com.example.bookexchange.repositories.UserRepository;
import com.example.bookexchange.util.ETagUtil;
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
                                .map(user ->
                                        ResultFactory.okETag(
                                                exchangeMapper.exchangeToExchangeDetailsDto(
                                                        exchange,
                                                        exchange.getReceiverUser().getNickname()
                                                ),
                                                ETagUtil.form(exchange)
                                        )
                                )
                                .flatMap(r -> r)
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
                        .flatMap(declinerUser -> {
                            if (!exchange.getVersion().equals(version)) {
                                auditService.log(AuditEvent.builder()
                                        .action("DECLINE_USER_REQUEST")
                                        .result(AuditResult.FAILURE)
                                        .actorId(senderUserId)
                                        .actorEmail(exchange.getSenderUser().getEmail())
                                        .reason("SYSTEM_OPTIMISTIC_LOCK")
                                        .detail("currentVersion", exchange.getVersion())
                                        .detail("incomingVersion", exchange.getVersion())
                                        .detail("currentExchangeState", exchange.getStatus())
                                        .build()
                                );

                                return ResultFactory.error(MessageKey.SYSTEM_OPTIMISTIC_LOCK, HttpStatus.CONFLICT);
                            }

                            if (exchange.getStatus().equals(ExchangeStatus.PENDING)) {
                                exchange.setStatus(ExchangeStatus.DECLINED);
                                exchange.setDeclinerUser(declinerUser);

                                exchangeRepository.save(exchange);
                            } else {
                                auditService.log(AuditEvent.builder()
                                        .action("DECLINE_USER_REQUEST")
                                        .result(AuditResult.FAILURE)
                                        .actorId(senderUserId)
                                        .actorEmail(exchange.getSenderUser().getEmail())
                                        .reason("EXCHANGE_CANT_BE_DECLINED")
                                        .build()
                                );

                                return ResultFactory.error(MessageKey.EXCHANGE_CANT_BE_DECLINED, HttpStatus.BAD_REQUEST);
                            }

                            auditService.log(AuditEvent.builder()
                                    .action("DECLINE_USER_REQUEST")
                                    .result(AuditResult.SUCCESS)
                                    .actorId(senderUserId)
                                    .actorEmail(exchange.getSenderUser().getEmail())
                                    .build()
                            );

                            return ResultFactory.updated(
                                    exchangeMapper.exchangeToExchangeDetailsDto(
                                            exchange,
                                            exchange.getReceiverUser().getNickname()
                                    ),
                                    MessageKey.EXCHANGE_DECLINED,
                                    ETagUtil.form(exchange)
                            );
                        })
                );
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
                    .actorEmail(ctx.getSenderUser().getEmail())
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
                        .actorEmail(ctx.getSenderUser().getEmail())
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
                    .actorEmail(ctx.getSenderUser().getEmail())
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
                    .actorEmail(ctx.getSenderUser().getEmail())
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
                    .actorEmail(ctx.getSenderUser().getEmail())
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
