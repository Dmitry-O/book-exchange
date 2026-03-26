package com.example.bookexchange.exchange.api;

import com.example.bookexchange.security.auth.CurrentUser;
import com.example.bookexchange.common.swagger.error_response.BadRequestErrorResponse;
import com.example.bookexchange.common.swagger.error_response.ConflictErrorResponse;
import com.example.bookexchange.common.swagger.error_response.NotFoundErrorResponse;
import com.example.bookexchange.common.swagger.error_response.UnauthorizedErrorResponse;
import com.example.bookexchange.common.swagger.page_data_response.ExchangePageData;
import com.example.bookexchange.common.web.ResultResponseMapper;
import com.example.bookexchange.exchange.dto.ExchangeDetailsDTO;
import com.example.bookexchange.exchange.dto.RequestCreateDTO;
import com.example.bookexchange.exchange.service.RequestService;
import com.example.bookexchange.common.util.ParserUtil;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Exchange requests")
@AllArgsConstructor
@RestController
public class RequestController {

    private final RequestService requestService;
    private ParserUtil parserUtil;
    private final ResultResponseMapper responseMapper;

    @UnauthorizedErrorResponse
    @BadRequestErrorResponse
    @NotFoundErrorResponse
    @ConflictErrorResponse
    @ApiResponse(
            responseCode = "201",
            description = "The exchange request has been created",
            headers = @Header(
                    name = "ETag",
                    description = "Entity version",
                    schema = @Schema(type = "string", example = "\"3\"")
            ),
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExchangeDetailsDTO.class),
                    examples = @ExampleObject(
                            value = """
                                        {
                                          "success": true,
                                          "data": {
                                            "id": 1,
                                            "status": "PENDING",
                                            "userNickname": "user_12345",
                                            "senderBook": {
                                              "id": 1,
                                              "name": "Charley Smash",
                                              "description": "An interesting book about ...",
                                              "author": "Frank Oester",
                                              "category": "Drama",
                                              "publicationYear": 1765,
                                              "photoBase64": "Book photo",
                                              "city": "London",
                                              "isGift": true,
                                              "isExchanged": false
                                            },
                                            "receiverBook": {
                                              "id": 1,
                                              "name": "Charley Smash",
                                              "description": "An interesting book about ...",
                                              "author": "Frank Oester",
                                              "category": "Drama",
                                              "publicationYear": 1765,
                                              "photoBase64": "Book photo",
                                              "city": "London",
                                              "isGift": true,
                                              "isExchanged": false
                                            }
                                          },
                                          "message": "The exchange request has been created",
                                          "error": null
                                        }
                                """
                    )
            )
    )
    @PostMapping(RequestPaths.REQUEST_PATH)
    public ResponseEntity<?> createRequest(
            @Parameter(description = "User ID", example = "1")
            @CurrentUser Long userId,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Exchange request data for the exchange creation", required = true)
            @Validated @RequestBody RequestCreateDTO dto,

            HttpServletRequest request
    ) {
        return responseMapper.map(
                requestService.createRequest(
                        userId,
                        dto
                ),
                request
        );
    }

    @UnauthorizedErrorResponse
    @NotFoundErrorResponse
    @ApiResponse(
            responseCode = "200",
            description = "The returned user's exchange request",
            headers = @Header(
                    name = "ETag",
                    description = "Entity version",
                    schema = @Schema(type = "string", example = "\"3\"")
            ),
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExchangeDetailsDTO.class)
            )
    )
    @GetMapping(RequestPaths.REQUEST_PATH_EXCHANGE_ID)
    public ResponseEntity<?> getUserRequestDetails(
            @Parameter(description = "User ID", example = "1")
            @CurrentUser Long userId,

            @Parameter(description = "Exchange ID", example = "1")
            @PathVariable Long exchangeId,

            HttpServletRequest request
    ) {
        return responseMapper.map(requestService.getSenderRequestDetails(userId, exchangeId), request);
    }

    @UnauthorizedErrorResponse
    @NotFoundErrorResponse
    @ApiResponse(
            responseCode = "200",
            description = "A list of returned user's exchange requests",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExchangePageData.class)
            )
    )
    @GetMapping(RequestPaths.REQUEST_PATH)
    public ResponseEntity<?> getUserRequests(
            @Parameter(description = "User ID", example = "1")
            @CurrentUser Long userId,

            @Parameter(description = "Page index, starts from 0", example = "0")
            @RequestParam(value = "pageIndex", defaultValue = "0") Integer pageIndex,

            @Parameter(description = "Page size", example = "20")
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize,

            HttpServletRequest request
    ) {
        return responseMapper.map(
                requestService.getSenderRequests(
                        userId,
                        pageIndex,
                        pageSize
                ),
                request
        );
    }

    @UnauthorizedErrorResponse
    @ConflictErrorResponse
    @NotFoundErrorResponse
    @BadRequestErrorResponse
    @ApiResponse(
            responseCode = "200",
            description = "User's exchange request has been declined",
            headers = @Header(
                    name = "ETag",
                    description = "Entity version",
                    schema = @Schema(type = "string", example = "\"3\"")
            ),
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExchangeDetailsDTO.class),
                    examples = @ExampleObject(
                            value = """
                                        {
                                          "success": true,
                                          "data": {
                                               "id": 1,
                                               "status": "DECLINED",
                                               "userNickname": "user_12345",
                                               "senderBook": {
                                                 "id": 1,
                                                 "name": "Charley Smash",
                                                 "description": "An interesting book about ...",
                                                 "author": "Frank Oester",
                                                 "category": "Drama",
                                                 "publicationYear": 1765,
                                                 "photoBase64": "Book photo",
                                                 "city": "London",
                                                 "isGift": true,
                                                 "isExchanged": false
                                               },
                                               "receiverBook": {
                                                 "id": 1,
                                                 "name": "Charley Smash",
                                                 "description": "An interesting book about ...",
                                                 "author": "Frank Oester",
                                                 "category": "Drama",
                                                 "publicationYear": 1765,
                                                 "photoBase64": "Book photo",
                                                 "city": "London",
                                                 "isGift": true,
                                                 "isExchanged": false
                                               }
                                          },
                                          "message": "The exchange request has been declined",
                                          "error": null
                                        }
                                """
                    )
            )
    )
    @PatchMapping(RequestPaths.REQUEST_PATH_DECLINE_REQUEST)
    public ResponseEntity<?> declineUserRequest(
            @Parameter(description = "User ID", example = "1")
            @CurrentUser Long userId,

            @Parameter(description = "Exchange ID", example = "1")
            @PathVariable Long exchangeId,

            @Parameter(
                    name = "If-Match",
                    description = "Entity version for optimistic locking",
                    example = "\"3\"",
                    required = true
            )
            @RequestHeader("If-Match") String ifMatch,

            HttpServletRequest request
    ) {
        return responseMapper.map(
                requestService.declineUserRequest(
                        userId,
                        exchangeId,
                        parserUtil.ifMatchParser(ifMatch)
                ),
                request
        );
    }
}
