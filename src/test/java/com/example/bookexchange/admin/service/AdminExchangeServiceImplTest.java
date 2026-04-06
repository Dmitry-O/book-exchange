package com.example.bookexchange.admin.service;

import com.example.bookexchange.admin.dto.ExchangeAdminDTO;
import com.example.bookexchange.admin.mapper.AdminMapper;
import com.example.bookexchange.common.dto.PageQueryDTO;
import com.example.bookexchange.common.result.Result;
import com.example.bookexchange.exchange.model.ExchangeStatus;
import com.example.bookexchange.exchange.repository.ExchangeRepository;
import com.example.bookexchange.support.unit.UnitFixtureIds;
import com.example.bookexchange.support.unit.UnitTestDataFactory;
import com.example.bookexchange.common.audit.service.AuditService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.example.bookexchange.support.unit.ResultAssertions.assertSuccess;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminExchangeServiceImplTest {

    @Mock
    private ExchangeRepository exchangeRepository;

    @Mock
    private AdminMapper adminMapper;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private AdminExchangeServiceImpl adminExchangeService;

    @Test
    void shouldUseFilteredQuery_whenAdminRequestsExchangesWithStatuses() {
        var sender = UnitTestDataFactory.user(UnitFixtureIds.VERIFIED_USER_ID, "sender@example.com", "sender_one");
        var receiver = UnitTestDataFactory.user(UnitFixtureIds.RECEIVER_USER_ID, "receiver@example.com", "receiver_one");
        var senderBook = UnitTestDataFactory.book(UnitFixtureIds.SENDER_BOOK_ID, "Sender book", sender);
        var receiverBook = UnitTestDataFactory.book(UnitFixtureIds.RECEIVER_BOOK_ID, "Receiver book", receiver);
        var exchange = UnitTestDataFactory.exchange(UnitFixtureIds.EXCHANGE_ID, sender, receiver, senderBook, receiverBook, ExchangeStatus.PENDING);
        ExchangeAdminDTO dto = org.mockito.Mockito.mock(ExchangeAdminDTO.class);
        PageQueryDTO queryDTO = UnitTestDataFactory.pageQuery(0, 20);

        when(exchangeRepository.findByStatusIn(any(), any())).thenReturn(new PageImpl<>(List.of(exchange)));
        when(adminMapper.exchangeToExchangeAdminDto(exchange)).thenReturn(dto);

        Result<org.springframework.data.domain.Page<ExchangeAdminDTO>> result =
                adminExchangeService.findExchanges(queryDTO, Set.of(ExchangeStatus.PENDING));

        assertSuccess(result, HttpStatus.OK);
        verify(exchangeRepository).findByStatusIn(any(), any());
    }

    @Test
    void shouldUseFindAll_whenAdminRequestsExchangesWithoutStatuses() {
        var sender = UnitTestDataFactory.user(UnitFixtureIds.VERIFIED_USER_ID, "sender@example.com", "sender_one");
        var receiver = UnitTestDataFactory.user(UnitFixtureIds.RECEIVER_USER_ID, "receiver@example.com", "receiver_one");
        var senderBook = UnitTestDataFactory.book(UnitFixtureIds.SENDER_BOOK_ID, "Sender book", sender);
        var receiverBook = UnitTestDataFactory.book(UnitFixtureIds.RECEIVER_BOOK_ID, "Receiver book", receiver);
        var exchange = UnitTestDataFactory.exchange(UnitFixtureIds.EXCHANGE_ID, sender, receiver, senderBook, receiverBook, ExchangeStatus.PENDING);
        ExchangeAdminDTO dto = org.mockito.Mockito.mock(ExchangeAdminDTO.class);
        PageQueryDTO queryDTO = UnitTestDataFactory.pageQuery(0, 20);

        when(exchangeRepository.findAll(any(org.springframework.data.domain.Pageable.class))).thenReturn(new PageImpl<>(List.of(exchange)));
        when(adminMapper.exchangeToExchangeAdminDto(exchange)).thenReturn(dto);

        Result<org.springframework.data.domain.Page<ExchangeAdminDTO>> result =
                adminExchangeService.findExchanges(queryDTO, null);

        assertSuccess(result, HttpStatus.OK);
        verify(exchangeRepository).findAll(any(org.springframework.data.domain.Pageable.class));
    }

    @Test
    void shouldReturnMappedExchange_whenAdminGetsExchangeById() {
        var sender = UnitTestDataFactory.user(UnitFixtureIds.VERIFIED_USER_ID, "sender@example.com", "sender_one");
        var receiver = UnitTestDataFactory.user(UnitFixtureIds.RECEIVER_USER_ID, "receiver@example.com", "receiver_one");
        var senderBook = UnitTestDataFactory.book(UnitFixtureIds.SENDER_BOOK_ID, "Sender book", sender);
        var receiverBook = UnitTestDataFactory.book(UnitFixtureIds.RECEIVER_BOOK_ID, "Receiver book", receiver);
        var exchange = UnitTestDataFactory.exchange(UnitFixtureIds.EXCHANGE_ID, sender, receiver, senderBook, receiverBook, ExchangeStatus.PENDING);
        ExchangeAdminDTO dto = org.mockito.Mockito.mock(ExchangeAdminDTO.class);
        var admin = UnitTestDataFactory.adminPrincipal("admin@example.com");

        when(exchangeRepository.findById(exchange.getId())).thenReturn(Optional.of(exchange));
        when(adminMapper.exchangeToExchangeAdminDto(exchange)).thenReturn(dto);

        Result<ExchangeAdminDTO> result = adminExchangeService.findExchangeById(admin, exchange.getId());

        assertSuccess(result, HttpStatus.OK);
        verify(auditService).log(any());
    }
}
