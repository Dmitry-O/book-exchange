package com.example.bookexchange.admin.api;

import com.example.bookexchange.admin.dto.ExchangeAdminDTO;
import com.example.bookexchange.admin.service.AdminExchangeService;
import com.example.bookexchange.common.dto.PageQueryDTO;
import com.example.bookexchange.common.swagger.error_response.BadRequestErrorResponse;
import com.example.bookexchange.common.swagger.error_response.ForbiddenErrorResponse;
import com.example.bookexchange.common.swagger.error_response.NotFoundErrorResponse;
import com.example.bookexchange.common.swagger.error_response.UnauthorizedErrorResponse;
import com.example.bookexchange.common.swagger.page_data_response.ExchangeAdminPageData;
import com.example.bookexchange.common.web.ResultResponseMapper;
import com.example.bookexchange.exchange.model.ExchangeStatus;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@Tag(name = "Administrator services - Exchanges")
@RestController
@AllArgsConstructor
public class AdminExchangeController {

    private final AdminExchangeService adminExchangeService;
    private final ResultResponseMapper responseMapper;

    @UnauthorizedErrorResponse
    @NotFoundErrorResponse
    @ForbiddenErrorResponse
    @BadRequestErrorResponse
    @ApiResponse(
            responseCode = "200",
            description = "A list of returned exchanges",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExchangeAdminPageData.class)
            )
    )
    @GetMapping(AdminPaths.ADMIN_PATH_EXCHANGES)
    public ResponseEntity<?> adminGetExchanges(
            @ParameterObject
            @Validated @ModelAttribute PageQueryDTO queryDTO,

            @Parameter(description = "Exchange statuses", example = "APPROVED,DECLINED,PENDING")
            @RequestParam(value = "exchangeStatuses", required = false) Set<ExchangeStatus> exchangeStatuses,

            HttpServletRequest request
    ) {
        return responseMapper.map(
                adminExchangeService.findExchanges(
                        queryDTO,
                        exchangeStatuses
                ),
                request
        );
    }

    @UnauthorizedErrorResponse
    @NotFoundErrorResponse
    @ForbiddenErrorResponse
    @ApiResponse(
            responseCode = "200",
            description = "The returned exchange",
            headers = @Header(
                    name = "ETag",
                    description = "Entity version",
                    schema = @Schema(type = "string", example = "\"3\"")
            ),
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExchangeAdminDTO.class)
            )
    )
    @GetMapping(AdminPaths.ADMIN_PATH_EXCHANGES_ID)
    public ResponseEntity<?> adminGetExchangeById(
            @AuthenticationPrincipal UserDetails adminUser,

            @Parameter(description = "Exchange ID", example = "1")
            @PathVariable Long exchangeId,

            HttpServletRequest request
    ) {
        return responseMapper.map(adminExchangeService.findExchangeById(adminUser, exchangeId), request);
    }
}
