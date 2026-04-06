package com.example.bookexchange.exchange.util;

import com.example.bookexchange.common.i18n.MessageKey;
import com.example.bookexchange.common.result.Result;
import com.example.bookexchange.exchange.model.Exchange;
import com.example.bookexchange.exchange.model.UserExchangeRole;
import com.example.bookexchange.exchange.repository.ExchangeRepository;
import com.example.bookexchange.support.unit.UnitFixtureIds;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static com.example.bookexchange.support.unit.ResultAssertions.assertFailure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExchangeUtilTest {

    @Mock
    private ExchangeRepository exchangeRepository;

    @InjectMocks
    private ExchangeUtil exchangeUtil;

    @Test
    void shouldReturnSender_whenUserOwnsSenderSide() {
        when(exchangeRepository.findByIdAndSenderUserId(UnitFixtureIds.EXCHANGE_ID, UnitFixtureIds.VERIFIED_USER_ID))
                .thenReturn(Optional.of(new Exchange()));

        Result<UserExchangeRole> result = exchangeUtil.identifyUserExchangeRole(
                UnitFixtureIds.VERIFIED_USER_ID,
                UnitFixtureIds.EXCHANGE_ID
        );

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void shouldReturnNotFound_whenUserDoesNotBelongToExchange() {
        when(exchangeRepository.findByIdAndSenderUserId(UnitFixtureIds.EXCHANGE_ID, UnitFixtureIds.VERIFIED_USER_ID))
                .thenReturn(Optional.empty());
        when(exchangeRepository.findByIdAndReceiverUserId(UnitFixtureIds.EXCHANGE_ID, UnitFixtureIds.VERIFIED_USER_ID))
                .thenReturn(Optional.empty());

        Result<UserExchangeRole> result = exchangeUtil.identifyUserExchangeRole(
                UnitFixtureIds.VERIFIED_USER_ID,
                UnitFixtureIds.EXCHANGE_ID
        );

        assertFailure(result, MessageKey.EXCHANGE_NOT_FOUND, HttpStatus.NOT_FOUND);
    }
}
