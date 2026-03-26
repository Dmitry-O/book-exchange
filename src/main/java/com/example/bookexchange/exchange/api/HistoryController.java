package com.example.bookexchange.exchange.api;

import com.example.bookexchange.security.auth.CurrentUser;
import com.example.bookexchange.common.swagger.error_response.NotFoundErrorResponse;
import com.example.bookexchange.common.swagger.error_response.UnauthorizedErrorResponse;
import com.example.bookexchange.common.swagger.page_data_response.HistoryPageData;
import com.example.bookexchange.common.web.ResultResponseMapper;
import com.example.bookexchange.exchange.dto.ExchangeHistoryDetailsDTO;
import com.example.bookexchange.exchange.service.HistoryService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Exchange history")
@RestController
@AllArgsConstructor
public class HistoryController {

    private final HistoryService historyService;
    private final ResultResponseMapper responseMapper;

    @UnauthorizedErrorResponse
    @NotFoundErrorResponse
    @ApiResponse(
            responseCode = "200",
            description = "A list of returned completed user's exchanges",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = HistoryPageData.class)
            )
    )
    @GetMapping(HistoryPaths.HISTORY_PATH)
    public ResponseEntity<?> getExchangeHistory(
            @Parameter(description = "User ID", example = "1")
            @CurrentUser Long userId,

            @Parameter(description = "Page index, starts from 0", example = "0")
            @RequestParam(value = "pageIndex", defaultValue = "0") Integer pageIndex,

            @Parameter(description = "Page size", example = "20")
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize,

            HttpServletRequest request
    ) {
        return responseMapper.map(
                historyService.getUserExchangeHistory(
                        userId,
                        pageIndex,
                        pageSize
                ),
                request
        );
    }

    @UnauthorizedErrorResponse
    @NotFoundErrorResponse
    @ApiResponse(
            responseCode = "200",
            description = "The returned user's history of a specific exchange ",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExchangeHistoryDetailsDTO.class)
            )
    )
    @GetMapping(HistoryPaths.HISTORY_PATH_EXCHANGE_ID)
    public ResponseEntity<?> getExchangeHistoryDetails(
            @Parameter(description = "User ID", example = "1")
            @CurrentUser Long userId,

            @Parameter(description = "Exchange ID", example = "1")
            @PathVariable Long exchangeId,

            HttpServletRequest request
    ) {
        return responseMapper.map(
                historyService.getUserExchangeHistoryDetails(
                        userId,
                        exchangeId
                ),
                request
        );
    }
}
