package com.example.bookexchange.services;

import com.example.bookexchange.core.result.Result;
import com.example.bookexchange.core.result.ResultFactory;
import com.example.bookexchange.dto.ExchangeDTO;
import com.example.bookexchange.dto.ExchangeDetailsDTO;
import com.example.bookexchange.mappers.ExchangeMapper;
import com.example.bookexchange.models.*;
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

import java.util.List;

@Service
@AllArgsConstructor
public class OfferServiceImpl implements OfferService {

    private final ExchangeRepository exchangeRepository;
    private final UserRepository userRepository;
    private final ExchangeMapper exchangeMapper;

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
        return ResultFactory.fromOptional(
                        exchangeRepository.findByIdAndReceiverUserId(
                                exchangeId,
                                receiverUserId
                        ),
                        MessageKey.EXCHANGE_NOT_FOUND
                )
                .flatMap(exchange -> {
                    if (!exchange.getVersion().equals(version)) {
                        return ResultFactory.error(MessageKey.SYSTEM_OPTIMISTIC_LOCK, HttpStatus.CONFLICT);
                    }

                    Book senderBook = exchange.getSenderBook();
                    Book receiverBook = exchange.getReceiverBook();

                    if (senderBook != null) {
                        senderBook.setIsExchanged(true);
                    }

                    receiverBook.setIsExchanged(true);

                    if (exchange.getStatus().equals(ExchangeStatus.PENDING)) {
                        exchange.setStatus(ExchangeStatus.APPROVED);
                        exchangeRepository.save(exchange);

                        List<Exchange> senderExchanges = exchangeRepository.findByIdNotAndSenderBookIdAndStatus(
                                exchangeId,
                                exchange.getSenderBook().getId(),
                                ExchangeStatus.PENDING
                        );
                        List<Exchange> receiverExchanges = exchangeRepository.findByIdNotAndReceiverBookIdAndStatus(
                                exchangeId,
                                exchange.getReceiverBook().getId(),
                                ExchangeStatus.PENDING
                        );

                        senderExchanges.forEach(e->{
                            e.setStatus(ExchangeStatus.DECLINED);
                            exchangeRepository.save(e);
                        });

                        receiverExchanges.forEach(e->{
                            e.setStatus(ExchangeStatus.DECLINED);
                            exchangeRepository.save(e);
                        });
                    } else {
                        return ResultFactory.error(MessageKey.EXCHANGE_CANT_BE_APPROVED, HttpStatus.BAD_REQUEST);
                    }

                    return ResultFactory.updated(
                            exchangeMapper.exchangeToExchangeDetailsDto(
                                    exchange,
                                    exchange.getSenderUser().getNickname()
                            ),
                            MessageKey.EXCHANGE_APPROVED,
                            ETagUtil.form(exchange)
                    );
                });
    }

    @Transactional
    @Override
    public Result<ExchangeDetailsDTO> declineUserOffer(Long receiverUserId, Long exchangeId, Long version) {
        return ResultFactory.fromOptional(
                        exchangeRepository.findByIdAndReceiverUserId(
                                exchangeId,
                                receiverUserId
                        ),
                        MessageKey.EXCHANGE_NOT_FOUND
                )
                .flatMap(exchange -> {
                    if (!exchange.getVersion().equals(version)) {
                        return ResultFactory.error(MessageKey.SYSTEM_OPTIMISTIC_LOCK, HttpStatus.CONFLICT);
                    }

                    return ResultFactory.fromRepository(
                                    userRepository,
                                    receiverUserId,
                                    MessageKey.USER_ACCOUNT_NOT_FOUND
                            )
                            .flatMap(declinerUser -> {
                                if (exchange.getStatus().equals(ExchangeStatus.PENDING)) {
                                    exchange.setStatus(ExchangeStatus.DECLINED);
                                    exchange.setDeclinerUser(declinerUser);

                                    exchangeRepository.save(exchange);
                                } else {
                                    return ResultFactory.error(MessageKey.EXCHANGE_CANT_BE_DECLINED, HttpStatus.BAD_REQUEST);
                                }

                                return ResultFactory.updated(
                                        exchangeMapper.exchangeToExchangeDetailsDto(
                                                exchange,
                                                exchange.getSenderUser().getNickname()
                                        ),
                                        MessageKey.EXCHANGE_DECLINED,
                                        ETagUtil.form(exchange)
                                );
                            });
                });
    }
}
