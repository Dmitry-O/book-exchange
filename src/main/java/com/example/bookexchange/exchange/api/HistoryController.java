package com.example.bookexchange.exchange.api;

import com.example.bookexchange.common.dto.PageQueryDTO;
import com.example.bookexchange.security.auth.CurrentUser;
import com.example.bookexchange.common.swagger.error_response.NotFoundErrorResponse;
import com.example.bookexchange.common.swagger.error_response.UnauthorizedErrorResponse;
import com.example.bookexchange.common.swagger.page_data_response.ExchangeUpdatePageData;
import com.example.bookexchange.common.swagger.page_data_response.HistoryPageData;
import com.example.bookexchange.common.web.ResultResponseMapper;
import com.example.bookexchange.exchange.dto.ExchangeHistoryDetailsDTO;
import com.example.bookexchange.exchange.dto.ExchangeUpdateDTO;
import com.example.bookexchange.exchange.dto.ExchangeUpdateQueryDTO;
import com.example.bookexchange.exchange.dto.ExchangeUpdateReadStateChangeDTO;
import com.example.bookexchange.exchange.service.HistoryService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Exchange history")
@RestController
@AllArgsConstructor
public class HistoryController {

    private final HistoryService historyService;
    private final ResultResponseMapper responseMapper;

    @UnauthorizedErrorResponse
    @ApiResponse(
            responseCode = "200",
            description = "A chronological list of exchange updates for the current user",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExchangeUpdatePageData.class)
            )
    )
    @GetMapping(ExchangePaths.UPDATES_PATH)
    public ResponseEntity<?> getExchangeUpdates(
            @Parameter(description = "User ID", example = "1")
            @CurrentUser Long userId,

            @ParameterObject
            @Validated @ModelAttribute ExchangeUpdateQueryDTO queryDTO,

            HttpServletRequest request
    ) {
        return responseMapper.map(historyService.getExchangeUpdates(userId, queryDTO), request);
    }

    @UnauthorizedErrorResponse
    @ApiResponse(
            responseCode = "200",
            description = "A list of unread exchange updates for the current user",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExchangeUpdatePageData.class)
            )
    )
    @GetMapping(ExchangePaths.UPDATES_PATH_UNREAD)
    public ResponseEntity<?> getUnreadExchangeUpdates(
            @Parameter(description = "User ID", example = "1")
            @CurrentUser Long userId,

            @ParameterObject
            @Validated @ModelAttribute PageQueryDTO queryDTO,

            HttpServletRequest request
    ) {
        return responseMapper.map(historyService.getUnreadExchangeUpdates(userId, queryDTO), request);
    }

    @UnauthorizedErrorResponse
    @NotFoundErrorResponse
    @ApiResponse(
            responseCode = "200",
            description = "Updates read state for the current user in exchange updates feed",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExchangeUpdateDTO.class)
            )
    )
    @PatchMapping(ExchangePaths.UPDATES_PATH_EXCHANGE_ID_READ_STATE)
    public ResponseEntity<?> updateExchangeUpdateReadState(
            @Parameter(description = "User ID", example = "1")
            @CurrentUser Long userId,

            @Parameter(description = "Exchange ID", example = "1")
            @PathVariable Long exchangeId,

            @Validated @RequestBody ExchangeUpdateReadStateChangeDTO dto,

            HttpServletRequest request
    ) {
        return responseMapper.map(
                historyService.updateExchangeUpdateReadState(userId, exchangeId, dto),
                request
        );
    }

    @UnauthorizedErrorResponse
    @NotFoundErrorResponse
    @ApiResponse(
            responseCode = "200",
            description = "Updates read state for a non-exchange notification in the current user's updates feed",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExchangeUpdateDTO.class)
            )
    )
    @PatchMapping(ExchangePaths.UPDATES_PATH_NOTIFICATION_ID_READ_STATE)
    public ResponseEntity<?> updateNotificationUpdateReadState(
            @Parameter(description = "User ID", example = "1")
            @CurrentUser Long userId,

            @Parameter(description = "Notification update ID", example = "1")
            @PathVariable Long notificationId,

            @Validated @RequestBody ExchangeUpdateReadStateChangeDTO dto,

            HttpServletRequest request
    ) {
        return responseMapper.map(
                historyService.updateNotificationUpdateReadState(userId, notificationId, dto),
                request
        );
    }

    @UnauthorizedErrorResponse
    @ApiResponse(
            responseCode = "200",
            description = "Marks all exchange and notification updates as read for the current user"
    )
    @PatchMapping(ExchangePaths.UPDATES_PATH_MARK_ALL_READ)
    public ResponseEntity<?> markAllUpdatesAsRead(
            @Parameter(description = "User ID", example = "1")
            @CurrentUser Long userId,

            HttpServletRequest request
    ) {
        return responseMapper.map(historyService.markAllUpdatesAsRead(userId), request);
    }

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
    @GetMapping(ExchangePaths.HISTORY_PATH)
    public ResponseEntity<?> getExchangeHistory(
            @Parameter(description = "User ID", example = "1")
            @CurrentUser Long userId,

            @ParameterObject
            @Validated @ModelAttribute PageQueryDTO queryDTO,

            HttpServletRequest request
    ) {
        return responseMapper.map(
                historyService.getUserExchangeHistory(userId, queryDTO),
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
    @GetMapping(ExchangePaths.HISTORY_PATH_EXCHANGE_ID)
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
