package com.example.bookexchange.exchange.service;

import com.example.bookexchange.book.model.Book;
import com.example.bookexchange.book.repository.BookRepository;
import com.example.bookexchange.common.audit.service.AuditService;
import com.example.bookexchange.common.i18n.MessageKey;
import com.example.bookexchange.common.result.Result;
import com.example.bookexchange.exchange.dto.ExchangeDetailsDTO;
import com.example.bookexchange.exchange.dto.RequestCreateDTO;
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

import java.util.Optional;

import static com.example.bookexchange.common.i18n.MessageKey.BOOK_ALREADY_EXCHANGED;
import static com.example.bookexchange.common.i18n.MessageKey.BOOK_CANT_EXCHANGE_SAME_BOOK;
import static com.example.bookexchange.common.i18n.MessageKey.BOOK_EXCHANGE_ALREADY_EXISTS;
import static com.example.bookexchange.common.i18n.MessageKey.EXCHANGE_CANT_BE_WITH_YOURSELF;
import static com.example.bookexchange.common.i18n.MessageKey.EXCHANGE_CREATED;
import static com.example.bookexchange.common.i18n.MessageKey.EXCHANGE_DECLINED;
import static com.example.bookexchange.common.i18n.MessageKey.EXCHANGE_SENDER_BOOK_REQUIRED;
import static com.example.bookexchange.common.result.ResultFactory.ok;
import static com.example.bookexchange.support.unit.ResultAssertions.assertFailure;
import static com.example.bookexchange.support.unit.ResultAssertions.assertSuccess;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestServiceImplTest {

    @Mock
    private ExchangeRepository exchangeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private ExchangeMapper exchangeMapper;

    @Mock
    private AuditService auditService;

    @Mock
    private ExchangeTransitionHelper exchangeTransitionHelper;

    @InjectMocks
    private RequestServiceImpl requestService;

    @Test
    void shouldReturnBadRequest_whenCreateRequestReceiverBookAlreadyExchanged() {
        User sender = UnitTestDataFactory.user(UnitFixtureIds.VERIFIED_USER_ID, "sender@example.com", "sender_one");
        User receiver = UnitTestDataFactory.user(UnitFixtureIds.RECEIVER_USER_ID, "receiver@example.com", "receiver_one");
        Book receiverBook = UnitTestDataFactory.book(UnitFixtureIds.RECEIVER_BOOK_ID, "Receiver book", receiver);
        receiverBook.setIsExchanged(true);
        RequestCreateDTO dto = UnitTestDataFactory.requestCreateDto(receiver.getId(), UnitFixtureIds.SENDER_BOOK_ID, receiverBook.getId());

        when(bookRepository.findByIdAndUserId(receiverBook.getId(), receiver.getId())).thenReturn(Optional.of(receiverBook));

        Result<ExchangeDetailsDTO> result = requestService.createRequest(sender.getId(), dto);

        assertFailure(result, BOOK_ALREADY_EXCHANGED, HttpStatus.BAD_REQUEST);
        verify(auditService).log(any());
    }

    @Test
    void shouldReturnConflict_whenCreateRequestGiftExchangeAlreadyExists() {
        User sender = UnitTestDataFactory.user(UnitFixtureIds.VERIFIED_USER_ID, "sender@example.com", "sender_one");
        User receiver = UnitTestDataFactory.user(UnitFixtureIds.RECEIVER_USER_ID, "receiver@example.com", "receiver_one");
        Book receiverBook = UnitTestDataFactory.book(UnitFixtureIds.RECEIVER_BOOK_ID, "Receiver book", receiver);
        receiverBook.setIsGift(true);
        RequestCreateDTO dto = UnitTestDataFactory.requestCreateDto(receiver.getId(), UnitFixtureIds.SENDER_BOOK_ID, receiverBook.getId());

        when(bookRepository.findByIdAndUserId(receiverBook.getId(), receiver.getId())).thenReturn(Optional.of(receiverBook));
        when(exchangeRepository.findBySenderUserIdAndReceiverUserIdAndReceiverBookIdAndStatusNot(
                sender.getId(),
                receiver.getId(),
                receiverBook.getId(),
                ExchangeStatus.DECLINED
        )).thenReturn(Optional.of(new Exchange()));

        Result<ExchangeDetailsDTO> result = requestService.createRequest(sender.getId(), dto);

        assertFailure(result, BOOK_EXCHANGE_ALREADY_EXISTS, HttpStatus.CONFLICT);
    }

    @Test
    void shouldReturnBadRequest_whenCreateRequestTargetsSameUser() {
        User sender = UnitTestDataFactory.user(UnitFixtureIds.VERIFIED_USER_ID, "sender@example.com", "sender_one");
        Book receiverBook = UnitTestDataFactory.book(UnitFixtureIds.RECEIVER_BOOK_ID, "Receiver book", sender);
        Book senderBook = UnitTestDataFactory.book(UnitFixtureIds.SENDER_BOOK_ID, "Sender book", sender);
        RequestCreateDTO dto = UnitTestDataFactory.requestCreateDto(sender.getId(), senderBook.getId(), receiverBook.getId());

        when(bookRepository.findByIdAndUserId(receiverBook.getId(), sender.getId())).thenReturn(Optional.of(receiverBook));

        Result<ExchangeDetailsDTO> result = requestService.createRequest(sender.getId(), dto);

        assertFailure(result, EXCHANGE_CANT_BE_WITH_YOURSELF, HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldReturnBadRequest_whenCreateRequestUsesSameBookOnBothSides() {
        User sender = UnitTestDataFactory.user(UnitFixtureIds.VERIFIED_USER_ID, "sender@example.com", "sender_one");
        User receiver = UnitTestDataFactory.user(UnitFixtureIds.RECEIVER_USER_ID, "receiver@example.com", "receiver_one");
        Book sharedBook = UnitTestDataFactory.book(UnitFixtureIds.SENDER_BOOK_ID, "Shared book", sender);
        RequestCreateDTO dto = UnitTestDataFactory.requestCreateDto(receiver.getId(), sharedBook.getId(), sharedBook.getId());

        when(bookRepository.findByIdAndUserId(sharedBook.getId(), receiver.getId())).thenReturn(Optional.of(sharedBook));
        when(bookRepository.findByIdAndUserId(sharedBook.getId(), sender.getId())).thenReturn(Optional.of(sharedBook));

        Result<ExchangeDetailsDTO> result = requestService.createRequest(sender.getId(), dto);

        assertFailure(result, BOOK_CANT_EXCHANGE_SAME_BOOK, HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldReturnBadRequest_whenCreateRequestOmitsSenderBookForNonGiftBook() {
        User sender = UnitTestDataFactory.user(UnitFixtureIds.VERIFIED_USER_ID, "sender@example.com", "sender_one");
        User receiver = UnitTestDataFactory.user(UnitFixtureIds.RECEIVER_USER_ID, "receiver@example.com", "receiver_one");
        Book receiverBook = UnitTestDataFactory.book(UnitFixtureIds.RECEIVER_BOOK_ID, "Receiver book", receiver);
        RequestCreateDTO dto = UnitTestDataFactory.requestCreateDto(receiver.getId(), null, receiverBook.getId());

        when(bookRepository.findByIdAndUserId(receiverBook.getId(), receiver.getId())).thenReturn(Optional.of(receiverBook));

        Result<ExchangeDetailsDTO> result = requestService.createRequest(sender.getId(), dto);

        assertFailure(result, EXCHANGE_SENDER_BOOK_REQUIRED, HttpStatus.BAD_REQUEST);
        verify(auditService).log(any());
    }

    @Test
    void shouldCreateGiftExchangeWithoutSenderBook_whenReceiverBookIsGift() {
        User sender = UnitTestDataFactory.user(UnitFixtureIds.VERIFIED_USER_ID, "sender@example.com", "sender_one");
        User receiver = UnitTestDataFactory.user(UnitFixtureIds.RECEIVER_USER_ID, "receiver@example.com", "receiver_one");
        Book receiverBook = UnitTestDataFactory.book(UnitFixtureIds.RECEIVER_BOOK_ID, "Receiver book", receiver);
        receiverBook.setIsGift(true);
        RequestCreateDTO dto = UnitTestDataFactory.requestCreateDto(receiver.getId(), null, receiverBook.getId());
        ExchangeDetailsDTO detailsDto = mock(ExchangeDetailsDTO.class);

        when(bookRepository.findByIdAndUserId(receiverBook.getId(), receiver.getId())).thenReturn(Optional.of(receiverBook));
        when(exchangeRepository.findBySenderUserIdAndReceiverUserIdAndReceiverBookIdAndStatusNot(
                sender.getId(),
                receiver.getId(),
                receiverBook.getId(),
                ExchangeStatus.DECLINED
        )).thenReturn(Optional.empty());
        when(userRepository.findById(sender.getId())).thenReturn(Optional.of(sender));
        when(userRepository.findById(receiver.getId())).thenReturn(Optional.of(receiver));
        when(exchangeRepository.save(any(Exchange.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(exchangeMapper.exchangeToExchangeDetailsDto(any(Exchange.class), any(Long.class), any(String.class))).thenReturn(detailsDto);

        Result<ExchangeDetailsDTO> result = requestService.createRequest(sender.getId(), dto);
        ArgumentCaptor<Exchange> exchangeCaptor = ArgumentCaptor.forClass(Exchange.class);

        assertSuccess(result, HttpStatus.CREATED, EXCHANGE_CREATED);
        verify(exchangeRepository).save(exchangeCaptor.capture());
        assertThat(exchangeCaptor.getValue().getSenderBook()).isNull();
        assertThat(exchangeCaptor.getValue().getReceiverBook()).isSameAs(receiverBook);
        assertThat(exchangeCaptor.getValue().getIsReadBySender()).isTrue();
        assertThat(exchangeCaptor.getValue().getIsReadByReceiver()).isFalse();
    }

    @Test
    void shouldCreatePendingExchange_whenCreateRequestPayloadIsValid() {
        User sender = UnitTestDataFactory.user(UnitFixtureIds.VERIFIED_USER_ID, "sender@example.com", "sender_one");
        User receiver = UnitTestDataFactory.user(UnitFixtureIds.RECEIVER_USER_ID, "receiver@example.com", "receiver_one");
        Book senderBook = UnitTestDataFactory.book(UnitFixtureIds.SENDER_BOOK_ID, "Sender book", sender);
        Book receiverBook = UnitTestDataFactory.book(UnitFixtureIds.RECEIVER_BOOK_ID, "Receiver book", receiver);
        RequestCreateDTO dto = UnitTestDataFactory.requestCreateDto(receiver.getId(), senderBook.getId(), receiverBook.getId());
        ExchangeDetailsDTO detailsDto = mock(ExchangeDetailsDTO.class);

        when(bookRepository.findByIdAndUserId(receiverBook.getId(), receiver.getId())).thenReturn(Optional.of(receiverBook));
        when(bookRepository.findByIdAndUserId(senderBook.getId(), sender.getId())).thenReturn(Optional.of(senderBook));
        when(userRepository.findById(sender.getId())).thenReturn(Optional.of(sender));
        when(userRepository.findById(receiver.getId())).thenReturn(Optional.of(receiver));
        when(exchangeRepository.save(any(Exchange.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(exchangeMapper.exchangeToExchangeDetailsDto(any(Exchange.class), any(Long.class), any(String.class))).thenReturn(detailsDto);

        Result<ExchangeDetailsDTO> result = requestService.createRequest(sender.getId(), dto);
        ArgumentCaptor<Exchange> exchangeCaptor = ArgumentCaptor.forClass(Exchange.class);

        assertSuccess(result, HttpStatus.CREATED, EXCHANGE_CREATED);
        verify(exchangeRepository).save(exchangeCaptor.capture());
        assertThat(exchangeCaptor.getValue().getIsReadBySender()).isTrue();
        assertThat(exchangeCaptor.getValue().getIsReadByReceiver()).isFalse();
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void shouldSetDeclinedStatusAndDecliner_whenSenderDeclinesPendingRequest() {
        User sender = UnitTestDataFactory.user(UnitFixtureIds.VERIFIED_USER_ID, "sender@example.com", "sender_one");
        User receiver = UnitTestDataFactory.user(UnitFixtureIds.RECEIVER_USER_ID, "receiver@example.com", "receiver_one");
        Book senderBook = UnitTestDataFactory.book(UnitFixtureIds.SENDER_BOOK_ID, "Sender book", sender);
        Book receiverBook = UnitTestDataFactory.book(UnitFixtureIds.RECEIVER_BOOK_ID, "Receiver book", receiver);
        Exchange exchange = UnitTestDataFactory.exchange(
                UnitFixtureIds.EXCHANGE_ID,
                sender,
                receiver,
                senderBook,
                receiverBook,
                ExchangeStatus.PENDING
        );
        ExchangeDetailsDTO detailsDto = mock(ExchangeDetailsDTO.class);

        when(exchangeRepository.findByIdAndSenderUserId(exchange.getId(), sender.getId())).thenReturn(Optional.of(exchange));
        when(exchangeTransitionHelper.requirePendingVersion(
                exchange,
                exchange.getVersion(),
                "DECLINE_USER_REQUEST",
                sender.getId(),
                sender.getEmail(),
                MessageKey.EXCHANGE_CANT_BE_DECLINED,
                "EXCHANGE_CANT_BE_DECLINED"
        )).thenReturn(ok(exchange));
        when(userRepository.findById(sender.getId())).thenReturn(Optional.of(sender));
        when(exchangeRepository.save(exchange)).thenReturn(exchange);
        when(exchangeMapper.exchangeToExchangeDetailsDto(exchange, receiver.getId(), receiver.getNickname())).thenReturn(detailsDto);

        Result<ExchangeDetailsDTO> result = requestService.declineUserRequest(sender.getId(), exchange.getId(), exchange.getVersion());

        assertSuccess(result, HttpStatus.OK, EXCHANGE_DECLINED);
        assertThat(exchange.getStatus()).isEqualTo(ExchangeStatus.DECLINED);
        assertThat(exchange.getDeclinerUser()).isSameAs(sender);
        assertThat(exchange.getIsReadBySender()).isTrue();
        assertThat(exchange.getIsReadByReceiver()).isFalse();
    }
}
