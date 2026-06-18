package com.example.bookexchange.common.demoemail;

import com.example.bookexchange.common.swagger.error_response.BadRequestErrorResponse;
import com.example.bookexchange.common.web.ApiResponse;
import com.example.bookexchange.common.web.ResultResponseMapper;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Demo Email Sandbox")
@RestController
@RequiredArgsConstructor
@Validated
@ConditionalOnProperty(name = "app.demo-email-sandbox.enabled", havingValue = "true")
public class DemoEmailSandboxController {

    private final DemoEmailSandboxService demoEmailSandboxService;
    private final ResultResponseMapper responseMapper;

    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Creates or refreshes a demo email sandbox session, optionally binding it to an email address",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = DemoEmailSandboxSessionDTO.class),
                    examples = @ExampleObject(
                            value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "sandboxId": "x9Qn3pQbeC3kJ1xtc5mTFMk27bENdExxRku07q-g2BU",
                                        "expiresAt": "2026-06-04T14:30:00Z",
                                        "enabled": true
                                      },
                                      "message": null,
                                      "error": null
                                    }
                                    """
                    )
            )
    )
    @PostMapping(DemoEmailSandboxPaths.DEMO_EMAIL_SANDBOX_SESSION_PATH)
    public ResponseEntity<?> createSession(
            @Parameter(description = "Existing demo sandbox id, if frontend already has one")
            @RequestHeader(value = DemoEmailSandboxService.REQUEST_HEADER_NAME, required = false) String sandboxId,

            @Parameter(description = "Email address to bind to this demo sandbox session")
            @RequestParam(required = false) @Email @Size(max = 254) String email,

            HttpServletRequest request,
            HttpServletResponse response
    ) {
        ResponseEntity<?> result = responseMapper.map(demoEmailSandboxService.createSession(sandboxId, email), request);
        exposeSandboxIdHeader(result, response);
        return result;
    }

    @BadRequestErrorResponse
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Returns messages captured for the requested demo sandbox",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = DemoEmailInboxDTO.class)
            )
    )
    @GetMapping(DemoEmailSandboxPaths.DEMO_EMAIL_SANDBOX_MESSAGES_PATH)
    public ResponseEntity<?> getMessages(
            @Parameter(description = "Demo sandbox id returned by the session endpoint")
            @RequestHeader(value = DemoEmailSandboxService.REQUEST_HEADER_NAME, required = false) String sandboxId,

            @Parameter(description = "Max number of messages to return", example = "20")
            @RequestParam(defaultValue = "20")
            @Min(1) @Max(100) Integer limit,

            HttpServletRequest request,
            HttpServletResponse response
    ) {
        ResponseEntity<?> result = responseMapper.map(demoEmailSandboxService.getInbox(sandboxId, limit), request);
        exposeSandboxIdHeader(result, response);
        return result;
    }

    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Deletes messages captured for the requested demo sandbox",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = DemoEmailInboxDTO.class)
            )
    )
    @DeleteMapping(DemoEmailSandboxPaths.DEMO_EMAIL_SANDBOX_MESSAGES_PATH)
    public ResponseEntity<?> clearMessages(
            @Parameter(description = "Demo sandbox id returned by the session endpoint")
            @RequestHeader(value = DemoEmailSandboxService.REQUEST_HEADER_NAME, required = false) String sandboxId,

            HttpServletRequest request,
            HttpServletResponse response
    ) {
        ResponseEntity<?> result = responseMapper.map(demoEmailSandboxService.clearInbox(sandboxId), request);
        exposeSandboxIdHeader(result, response);
        return result;
    }

    private void exposeSandboxIdHeader(ResponseEntity<?> result, HttpServletResponse response) {
        if (result.getBody() instanceof ApiResponse<?> apiResponse
                && apiResponse.getData() instanceof DemoEmailSandboxSessionDTO session
                && session.sandboxId() != null) {
            response.setHeader(DemoEmailSandboxService.REQUEST_HEADER_NAME, session.sandboxId());
        }

        if (result.getBody() instanceof ApiResponse<?> apiResponse
                && apiResponse.getData() instanceof DemoEmailInboxDTO inbox
                && inbox.sandboxId() != null) {
            response.setHeader(DemoEmailSandboxService.REQUEST_HEADER_NAME, inbox.sandboxId());
        }
    }
}
