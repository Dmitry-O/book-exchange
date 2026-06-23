package com.example.bookexchange.admin.api;

import com.example.bookexchange.common.demoreset.DemoResetService;
import com.example.bookexchange.common.i18n.MessageKey;
import com.example.bookexchange.common.result.Result;
import com.example.bookexchange.common.result.ResultFactory;
import com.example.bookexchange.common.web.ResultResponseMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDemoControllerTest {

    @Mock
    private DemoResetService demoResetService;

    @Mock
    private ResultResponseMapper responseMapper;

    @Mock
    private HttpServletRequest request;

    @Test
    void shouldDelegateManualDemoResetToService() {
        Result<Void> result = ResultFactory.okMessage(MessageKey.ADMIN_DEMO_RESET_COMPLETED);
        ResponseEntity<?> response = ResponseEntity.ok().build();
        AdminDemoController controller = new AdminDemoController(demoResetService, responseMapper);

        when(demoResetService.resetDemoEnvironment()).thenReturn(result);
        doReturn(response).when(responseMapper).map(result, request);

        ResponseEntity<?> actual = controller.resetDemoEnvironment(request);

        assertThat(actual).isSameAs(response);
        verify(demoResetService).resetDemoEnvironment();
        verify(responseMapper).map(result, request);
    }
}
