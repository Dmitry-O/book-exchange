package com.example.bookexchange.common.demoaccounts;

import com.example.bookexchange.common.result.Result;
import com.example.bookexchange.common.result.ResultFactory;
import com.example.bookexchange.common.web.ResultResponseMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DemoAccountControllerTest {

    @Mock
    private DemoAccountService demoAccountService;

    @Mock
    private ResultResponseMapper responseMapper;

    @Mock
    private HttpServletRequest request;

    @Test
    void shouldDelegateDemoAccountLookupToService() {
        List<DemoAccountDTO> accounts = List.of(new DemoAccountDTO(
                "demo_reader",
                "demo_reader",
                "reader.demo@example.com",
                "DemoPassword123!"
        ));
        Result<List<DemoAccountDTO>> result = ResultFactory.ok(accounts);
        ResponseEntity<?> response = ResponseEntity.ok().build();
        DemoAccountController controller = new DemoAccountController(demoAccountService, responseMapper);

        when(demoAccountService.findDemoAccounts()).thenReturn(result);
        doReturn(response).when(responseMapper).map(result, request);

        ResponseEntity<?> actual = controller.findDemoAccounts(request);

        assertThat(actual).isSameAs(response);
        verify(demoAccountService).findDemoAccounts();
        verify(responseMapper).map(result, request);
    }
}
