package com.example.bookexchange.common.demoaccounts;

import com.example.bookexchange.common.web.ApiResponse;
import com.example.bookexchange.common.web.ResultResponseMapper;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Demo Accounts")
@RestController
@RequiredArgsConstructor
@Validated
@ConditionalOnProperty(name = "app.runtime-env", havingValue = "demo")
public class DemoAccountController {

    private final DemoAccountService demoAccountService;
    private final ResultResponseMapper responseMapper;

    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Returns non-admin demo accounts for the login form account picker",
            content = @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = DemoAccountDTO.class)),
                    examples = @ExampleObject(
                            value = """
                                    {
                                      "success": true,
                                      "data": [
                                        {
                                          "label": "demo_reader",
                                          "nickname": "demo_reader",
                                          "email": "reader.demo@example.com",
                                          "password": "DemoPassword123!"
                                        }
                                      ],
                                      "message": null,
                                      "error": null
                                    }
                                    """
                    )
            )
    )
    @GetMapping(DemoAccountsPaths.DEMO_ACCOUNTS_PATH)
    public ResponseEntity<?> findDemoAccounts(HttpServletRequest request) {
        return responseMapper.map(demoAccountService.findDemoAccounts(), request);
    }
}
