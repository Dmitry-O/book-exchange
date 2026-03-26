package com.example.bookexchange.exchange.service;

import com.example.bookexchange.book.model.Book;
import com.example.bookexchange.common.audit.model.AuditEvent;
import com.example.bookexchange.common.audit.model.AuditResult;
import com.example.bookexchange.common.audit.service.AuditService;
import com.example.bookexchange.common.result.Result;
import com.example.bookexchange.common.result.ResultFactory;
import com.example.bookexchange.common.i18n.MessageKey;
import com.example.bookexchange.exchange.model.Exchange;
import com.example.bookexchange.exchange.mapper.ExchangeMapper;
import com.example.bookexchange.exchange.repository.ExchangeRepository;
import com.example.bookexchange.exchange.model.ExchangeStatus;
import com.example.bookexchange.exchange.dto.ExchangeDTO;
import com.example.bookexchange.exchange.dto.ExchangeDetailsDTO;
import com.example.bookexchange.user.repository.UserRepository;
import com.example.bookexchange.common.util.ETagUtil;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class OfferServiceImpl implements OfferService {

    private final ExchangeRepository exchangeRepository;
    private final UserRepository userRepository;
    private final ExchangeMapper exchangeMapper;
    private final AuditService auditService;
    private final ExchangeTransitionHelper exchangeTransitionHelper;

    @Transactional(readOnly = true)
    @Override
    public Result<Page<ExchangeDTO>> getUserOffers(Long receiverUserId, Integer pageIndex, Integer pageSize) {
        Pageable pageable = PageRequest.of(pageIndex, pageSize);

        Page<Exchange> exchangesPage = exchangeRepository.findByReceiverUserIdAndStatus(receiverUserId, ExchangeStatus.PENDING, pageable);

        Page<ExchangeDTO> page = exchangesPage.map(exchangeMapper::exchangeToExchangeDto);

        return ResultFactory.ok(page);
    }

    @Transactional(readOnly = true)
    @Override
    public Result<ExchangeDetailsDTO> getReceiverOfferDetails(Long receiverUserId, Long exchangeId) {
        return ResultFactory.fromOptional(
                exchangeRepository.findByIdAndReceiverUserId(
                        exchangeId,
                        receiverUserId
                ),
                MessageKey.EXCHANGE_NOT_FOUND
        )
                .map(exchange ->
                        ResultFactory.okETag(
                                exchangeMapper.exchangeToExchangeDetailsDto(
                                        exchange,
                                        exchange.getSenderUser().getNickname()
                                ),
                                ETagUtil.form(exchange)
                        )
                )
                .flatMap(r -> r);
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
        markBooksAsExchanged(exchange);
        exchange.setStatus(ExchangeStatus.APPROVED);
        exchangeRepository.save(exchange);

        declineCompetingExchanges(exchange);
        logOfferSuccess("APPROVE_USER_OFFER", receiverUserId, exchange.getReceiverUser().getEmail());

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

            exchangeRepository.save(exchange);
            logOfferSuccess("DECLINE_USER_OFFER", receiverUserId, exchange.getReceiverUser().getEmail());

            return updatedOfferDetails(exchange, MessageKey.EXCHANGE_DECLINED);
        });
    }

    private void markBooksAsExchanged(Exchange exchange) {
        Book senderBook = exchange.getSenderBook();

        if (senderBook != null) {
            senderBook.setIsExchanged(true);
        }

        exchange.getReceiverBook().setIsExchanged(true);
    }

    private void declineCompetingExchanges(Exchange approvedExchange) {
        findCompetingExchanges(approvedExchange).forEach(exchange -> {
            exchange.setStatus(ExchangeStatus.DECLINED);
            exchangeRepository.save(exchange);
        });
    }

    private List<Exchange> findCompetingExchanges(Exchange approvedExchange) {
        List<Exchange> competingExchanges = new ArrayList<>();

        Book senderBook = approvedExchange.getSenderBook();

        if (senderBook != null) {
            competingExchanges.addAll(exchangeRepository.findByIdNotAndSenderBookIdAndStatus(
                    approvedExchange.getId(),
                    senderBook.getId(),
                    ExchangeStatus.PENDING
            ));
        }

        competingExchanges.addAll(exchangeRepository.findByIdNotAndReceiverBookIdAndStatus(
                approvedExchange.getId(),
                approvedExchange.getReceiverBook().getId(),
                ExchangeStatus.PENDING
        ));

        return competingExchanges;
    }

    private Result<ExchangeDetailsDTO> updatedOfferDetails(Exchange exchange, MessageKey messageKey) {
        return ResultFactory.updated(
                exchangeMapper.exchangeToExchangeDetailsDto(
                        exchange,
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
}
