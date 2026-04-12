package com.example.bookexchange.exchange.api;

import com.example.bookexchange.common.dto.PageQueryDTO;
import com.example.bookexchange.security.auth.CurrentUser;
import com.example.bookexchange.common.swagger.error_response.BadRequestErrorResponse;
import com.example.bookexchange.common.swagger.error_response.ConflictErrorResponse;
import com.example.bookexchange.common.swagger.error_response.NotFoundErrorResponse;
import com.example.bookexchange.common.swagger.error_response.UnauthorizedErrorResponse;
import com.example.bookexchange.common.swagger.page_data_response.ExchangePageData;
import com.example.bookexchange.common.web.ResultResponseMapper;
import com.example.bookexchange.exchange.dto.ExchangeDetailsDTO;
import com.example.bookexchange.exchange.service.OfferService;
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
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Exchange offers")
@RestController
@AllArgsConstructor
public class OfferController {

    private final OfferService offerService;
    private ParserUtil parserUtil;
    private final ResultResponseMapper responseMapper;

    @UnauthorizedErrorResponse
    @NotFoundErrorResponse
    @ApiResponse(
            responseCode = "200",
            description = "A list of returned user's exchange offers",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExchangePageData.class)
            )
    )
    @GetMapping(ExchangePaths.OFFER_PATH)
    public ResponseEntity<?> getUserOffers(
            @Parameter(description = "User ID", example = "1")
            @CurrentUser Long userId,

            @ParameterObject
            @Validated @ModelAttribute PageQueryDTO queryDTO,

            HttpServletRequest request
    ) {
        return responseMapper.map(
                offerService.getUserOffers(userId, queryDTO),
                request
        );
    }

    @UnauthorizedErrorResponse
    @NotFoundErrorResponse
    @ApiResponse(
            responseCode = "200",
            description = "The returned user's exchange offer details",
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
    @GetMapping(ExchangePaths.OFFER_PATH_EXCHANGE_ID)
    public ResponseEntity<?> getUserOfferDetails(
            @Parameter(description = "User ID", example = "1")
            @CurrentUser Long userId,

            @Parameter(description = "Exchange ID", example = "1")
            @PathVariable Long exchangeId,

            HttpServletRequest request
    ) {
        return responseMapper.map(
                offerService.getReceiverOfferDetails(
                        userId,
                        exchangeId
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
            description = "User's exchange offer has been approved",
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
                                                       "status": "APPROVED",
                                                       "userNickname": "user_12345",
                                                       "senderBook": {
                                                         "id": 1,
                                                         "name": "Charley Smash",
                                                         "description": "An interesting book about ...",
                                                         "author": "Frank Oester",
                                                         "category": "Drama",
                                                         "publicationYear": 1765,
                                                         "photoUrl": "https://book-exchange-prod.s3.eu-central-1.amazonaws.com/users/42/books/15_1712582410000.jpg",
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
                                                         "photoUrl": "https://book-exchange-prod.s3.eu-central-1.amazonaws.com/users/84/books/19_1712582411000.jpg",
                                                         "city": "London",
                                                         "isGift": true,
                                                         "isExchanged": false
                                                       }
                                                    },
                                                  "message": "The exchange request has been confirmed",
                                                  "error": null
                                                }
                                        """
                    )
            )
    )
    @PatchMapping(ExchangePaths.OFFER_PATH_APPROVE_OFFER)
    public ResponseEntity<?> approveUserOffer(
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
                offerService.approveUserOffer(
                        userId,
                        exchangeId,
                        parserUtil.ifMatchParser(ifMatch)
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
            description = "User's exchange offer has been declined",
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
                                                 "photoUrl": "https://book-exchange-prod.s3.eu-central-1.amazonaws.com/users/42/books/15_1712582410000.jpg",
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
                                                 "photoUrl": "https://book-exchange-prod.s3.eu-central-1.amazonaws.com/users/84/books/19_1712582411000.jpg",
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
    @PatchMapping(ExchangePaths.OFFER_PATH_DECLINE_OFFER)
    public ResponseEntity<?> declineUserOffer(
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
                offerService.declineUserOffer(
                        userId,
                        exchangeId,
                        parserUtil.ifMatchParser(ifMatch)
                ),
                request
        );
    }
}
