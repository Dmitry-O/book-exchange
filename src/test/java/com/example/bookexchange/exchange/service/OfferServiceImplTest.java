package com.example.bookexchange.exchange.service;

import com.example.bookexchange.common.audit.service.AuditService;
import com.example.bookexchange.common.result.Result;
import com.example.bookexchange.exchange.dto.ExchangeDetailsDTO;
import com.example.bookexchange.exchange.mapper.ExchangeMapper;
import com.example.bookexchange.exchange.model.Exchange;
import com.example.bookexchange.exchange.model.ExchangeStatus;
import com.example.bookexchange.exchange.repository.ExchangeRepository;
import com.example.bookexchange.support.unit.UnitFixtureIds;
import com.example.bookexchange.support.unit.UnitTestDataFactory;
import com.example.bookexchange.user.model.User;
import com.example.bookexchange.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static com.example.bookexchange.common.i18n.MessageKey.EXCHANGE_APPROVED;
import static com.example.bookexchange.common.i18n.MessageKey.EXCHANGE_DECLINED;
import static com.example.bookexchange.common.result.ResultFactory.ok;
import static com.example.bookexchange.support.unit.ResultAssertions.assertSuccess;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OfferServiceImplTest {

    @Mock
    private ExchangeRepository exchangeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ExchangeMapper exchangeMapper;

    @Mock
    private AuditService auditService;

    @Mock
    private ExchangeTransitionHelper exchangeTransitionHelper;

    @InjectMocks
    private OfferServiceImpl offerService;

    @Test
    void shouldApproveExchangeAndDeclineCompetingExchanges_whenReceiverApprovesOffer() {
        User sender = UnitTestDataFactory.user(UnitFixtureIds.VERIFIED_USER_ID, "sender@example.com", "sender_one");
        User receiver = UnitTestDataFactory.user(UnitFixtureIds.RECEIVER_USER_ID, "receiver@example.com", "receiver_one");
        var senderBook = UnitTestDataFactory.book(UnitFixtureIds.SENDER_BOOK_ID, "Sender book", sender);
        var receiverBook = UnitTestDataFactory.book(UnitFixtureIds.RECEIVER_BOOK_ID, "Receiver book", receiver);
        Exchange approvedExchange = UnitTestDataFactory.exchange(
                UnitFixtureIds.EXCHANGE_ID,
                sender,
                receiver,
                senderBook,
                receiverBook,
                ExchangeStatus.PENDING
        );
        Exchange competingBySenderBook = UnitTestDataFactory.exchange(
                UnitFixtureIds.COMPETING_EXCHANGE_ID,
                sender,
                receiver,
                senderBook,
                UnitTestDataFactory.book(UnitFixtureIds.COMPETING_BOOK_ID, "Competing book", receiver),
                ExchangeStatus.PENDING
        );
        ExchangeDetailsDTO detailsDto = org.mockito.Mockito.mock(ExchangeDetailsDTO.class);

        when(exchangeRepository.findByIdAndReceiverUserId(approvedExchange.getId(), receiver.getId())).thenReturn(Optional.of(approvedExchange));
        when(exchangeTransitionHelper.requirePendingVersion(
                approvedExchange,
                approvedExchange.getVersion(),
                "APPROVE_USER_OFFER",
                receiver.getId(),
                receiver.getEmail(),
                com.example.bookexchange.common.i18n.MessageKey.EXCHANGE_CANT_BE_APPROVED,
                "EXCHANGE_CANT_BE_APPROVED"
        )).thenReturn(ok(approvedExchange));
        when(exchangeRepository.save(approvedExchange)).thenReturn(approvedExchange);
        when(exchangeRepository.findByIdNotAndSenderBookIdAndStatus(approvedExchange.getId(), senderBook.getId(), ExchangeStatus.PENDING))
                .thenReturn(List.of(competingBySenderBook));
        when(exchangeRepository.findByIdNotAndReceiverBookIdAndStatus(approvedExchange.getId(), receiverBook.getId(), ExchangeStatus.PENDING))
                .thenReturn(List.of());
        when(exchangeMapper.exchangeToExchangeDetailsDto(approvedExchange, sender.getNickname())).thenReturn(detailsDto);

        Result<ExchangeDetailsDTO> result = offerService.approveUserOffer(receiver.getId(), approvedExchange.getId(), approvedExchange.getVersion());

        assertSuccess(result, HttpStatus.OK, EXCHANGE_APPROVED);
        assertThat(approvedExchange.getStatus()).isEqualTo(ExchangeStatus.APPROVED);
        assertThat(senderBook.getIsExchanged()).isTrue();
        assertThat(receiverBook.getIsExchanged()).isTrue();
        assertThat(competingBySenderBook.getStatus()).isEqualTo(ExchangeStatus.DECLINED);

        ArgumentCaptor<Exchange> savedExchangeCaptor = ArgumentCaptor.forClass(Exchange.class);
        verify(exchangeRepository, times(2)).save(savedExchangeCaptor.capture());
        assertThat(savedExchangeCaptor.getAllValues().stream().anyMatch(exchange -> exchange == approvedExchange)).isTrue();
        assertThat(savedExchangeCaptor.getAllValues().stream().anyMatch(exchange -> exchange == competingBySenderBook)).isTrue();
    }

    @Test
    void shouldSetDeclinerAndDeclinedStatus_whenReceiverDeclinesOffer() {
        User sender = UnitTestDataFactory.user(UnitFixtureIds.VERIFIED_USER_ID, "sender@example.com", "sender_one");
        User receiver = UnitTestDataFactory.user(UnitFixtureIds.RECEIVER_USER_ID, "receiver@example.com", "receiver_one");
        var senderBook = UnitTestDataFactory.book(UnitFixtureIds.SENDER_BOOK_ID, "Sender book", sender);
        var receiverBook = UnitTestDataFactory.book(UnitFixtureIds.RECEIVER_BOOK_ID, "Receiver book", receiver);
        Exchange exchange = UnitTestDataFactory.exchange(
                UnitFixtureIds.EXCHANGE_ID,
                sender,
                receiver,
                senderBook,
                receiverBook,
                ExchangeStatus.PENDING
        );
        ExchangeDetailsDTO detailsDto = org.mockito.Mockito.mock(ExchangeDetailsDTO.class);

        when(exchangeRepository.findByIdAndReceiverUserId(exchange.getId(), receiver.getId())).thenReturn(Optional.of(exchange));
        when(exchangeTransitionHelper.requirePendingVersion(
                exchange,
                exchange.getVersion(),
                "DECLINE_USER_OFFER",
                receiver.getId(),
                receiver.getEmail(),
                com.example.bookexchange.common.i18n.MessageKey.EXCHANGE_CANT_BE_DECLINED,
                "EXCHANGE_CANT_BE_DECLINED"
        )).thenReturn(ok(exchange));
        when(userRepository.findById(receiver.getId())).thenReturn(Optional.of(receiver));
        when(exchangeRepository.save(exchange)).thenReturn(exchange);
        when(exchangeMapper.exchangeToExchangeDetailsDto(exchange, sender.getNickname())).thenReturn(detailsDto);

        Result<ExchangeDetailsDTO> result = offerService.declineUserOffer(receiver.getId(), exchange.getId(), exchange.getVersion());

        assertSuccess(result, HttpStatus.OK, EXCHANGE_DECLINED);
        assertThat(exchange.getStatus()).isEqualTo(ExchangeStatus.DECLINED);
        assertThat(exchange.getDeclinerUser()).isSameAs(receiver);
    }
}
