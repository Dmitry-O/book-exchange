package com.example.bookexchange.admin.api;

import com.example.bookexchange.admin.dto.BookAdminDTO;
import com.example.bookexchange.admin.service.AdminBookService;
import com.example.bookexchange.book.dto.BookSearchDTO;
import com.example.bookexchange.book.dto.BookUpdateDTO;
import com.example.bookexchange.book.model.BookType;
import com.example.bookexchange.common.dto.PageQueryDTO;
import com.example.bookexchange.common.swagger.error_response.*;
import com.example.bookexchange.common.swagger.page_data_response.BookAdminPageData;
import com.example.bookexchange.common.util.ParserUtil;
import com.example.bookexchange.common.web.ResultResponseMapper;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Administrator services - Books")
@RestController
@AllArgsConstructor
public class AdminBookController {

    private final AdminBookService adminBookService;
    private ParserUtil parserUtil;
    private final ResultResponseMapper responseMapper;

    @UnauthorizedErrorResponse
    @NotFoundErrorResponse
    @ForbiddenErrorResponse
    @BadRequestErrorResponse
    @ApiResponse(
            responseCode = "200",
            description = "A list of returned books",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = BookAdminPageData.class)
            )
    )
    @GetMapping(AdminPaths.ADMIN_PATH_BOOKS_SEARCH)
    public ResponseEntity<?> adminGetBooks(
            @ParameterObject
            @Validated @ModelAttribute PageQueryDTO queryDTO,

            @Parameter(description = "Book type", example = "ALL")
            @RequestParam(value = "bookType", defaultValue = "ALL") BookType bookType,

            @ParameterObject
            @Validated @ModelAttribute BookSearchDTO dto,

            HttpServletRequest request
    ) {
        return responseMapper.map(
                adminBookService.findBooks(
                        dto,
                        queryDTO,
                        bookType
                ),
                request
        );
    }

    @UnauthorizedErrorResponse
    @NotFoundErrorResponse
    @ForbiddenErrorResponse
    @ApiResponse(
            responseCode = "200",
            description = "The returned book",
            headers = @Header(
                    name = "ETag",
                    description = "Entity version",
                    schema = @Schema(type = "string", example = "\"3\"")
            ),
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = BookAdminDTO.class)
            )
    )
    @GetMapping(AdminPaths.ADMIN_PATH_BOOKS_ID)
    public ResponseEntity<?> adminGetBookById(
            @AuthenticationPrincipal UserDetails adminUser,

            @Parameter(description = "Book ID", example = "1")
            @PathVariable Long bookId,

            HttpServletRequest request
    ) {
        return responseMapper.map(adminBookService.findBookById(adminUser, bookId), request);
    }

    @UnauthorizedErrorResponse
    @ConflictErrorResponse
    @NotFoundErrorResponse
    @BadRequestErrorResponse
    @ForbiddenErrorResponse
    @ApiResponse(
            responseCode = "200",
            description = "The book has been updated",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = BookAdminDTO.class),
                    examples = @ExampleObject(
                            value = """
                                        {
                                          "success": true,
                                          "data": {
                                            "id": 1,
                                            "name": "Charley Smash updated",
                                            "description": "An interesting book about ...",
                                            "author": "Frank Oester",
                                            "category": "Drama",
                                            "publicationYear": 1765,
                                            "photoBase64": "Book photo",
                                            "city": "London",
                                            "isGift": true,
                                            "isExchanged": false,
                                            "meta": {
                                              "createdAt": "2026-03-23T13:00:58.815Z",
                                              "updatedAt": "2026-03-23T14:00:58.815Z",
                                              "deletedAt": null,
                                              "createdBy": 1,
                                              "updatedBy": 2,
                                              "createdRequestId": "4fa6b455-d7d0-4f90-9a07-df090a324401",
                                              "updatedRequestId": "3fa6b455-d7d0-4f90-9a07-df090a324401"
                                            }
                                          },
                                          "message": "The book 'Charley Smash updated' has been updated",
                                          "error": null
                                        }
                                """
                    )
            )
    )
    @PatchMapping(AdminPaths.ADMIN_PATH_BOOKS_ID)
    public ResponseEntity<?> adminUpdateBookById(
            @AuthenticationPrincipal UserDetails adminUser,

            @Parameter(description = "Book ID", example = "1")
            @PathVariable Long bookId,

            @Parameter(
                    name = "If-Match",
                    description = "Entity version for optimistic locking",
                    example = "\"3\"",
                    required = true
            )
            @RequestHeader("If-Match") String ifMatch,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Book data for the update", required = true)
            @Validated @RequestBody BookUpdateDTO dto,

            HttpServletRequest request
    ) {
        return responseMapper.map(
                adminBookService.updateBookById(
                        adminUser,
                        bookId,
                        dto,
                        parserUtil.ifMatchParser(ifMatch)
                ),
                request
        );
    }

    @UnauthorizedErrorResponse
    @ConflictErrorResponse
    @NotFoundErrorResponse
    @ForbiddenErrorResponse
    @ApiResponse(
            responseCode = "200",
            description = "The book has been deleted",
            headers = @Header(
                    name = "ETag",
                    description = "Entity version",
                    schema = @Schema(type = "string", example = "\"3\"")
            ),
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = BookAdminDTO.class),
                    examples = @ExampleObject(
                            value = """
                                        {
                                          "success": true,
                                          "data": {
                                            "id": 1,
                                            "name": "Charley Smash",
                                            "description": "An interesting book about ...",
                                            "author": "Frank Oester",
                                            "category": "Drama",
                                            "publicationYear": 1765,
                                            "photoBase64": "Book photo",
                                            "city": "London",
                                            "isGift": true,
                                            "isExchanged": false,
                                            "meta": {
                                              "createdAt": "2026-03-23T13:00:58.815Z",
                                              "updatedAt": "2026-03-23T14:00:58.815Z",
                                              "deletedAt": "2026-03-23T14:00:58.815Z",
                                              "createdBy": 1,
                                              "updatedBy": 2,
                                              "createdRequestId": "4fa6b455-d7d0-4f90-9a07-df090a324401",
                                              "updatedRequestId": "3fa6b455-d7d0-4f90-9a07-df090a324401"
                                            }
                                          },
                                          "message": "The book 'Charley Smash' has been deleted",
                                          "error": null
                                        }
                                """
                    )
            )
    )
    @DeleteMapping(AdminPaths.ADMIN_PATH_BOOKS_ID)
    public ResponseEntity<?> adminDeleteBookById(
            @AuthenticationPrincipal UserDetails adminUser,

            @Parameter(description = "Book ID", example = "1")
            @PathVariable Long bookId,

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
                adminBookService.deleteBookById(
                        adminUser,
                        bookId,
                        parserUtil.ifMatchParser(ifMatch)
                ),
                request
        );
    }

    @UnauthorizedErrorResponse
    @ConflictErrorResponse
    @NotFoundErrorResponse
    @BadRequestErrorResponse
    @ForbiddenErrorResponse
    @ApiResponse(
            responseCode = "200",
            description = "The book has been restored",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = BookAdminDTO.class),
                    examples = @ExampleObject(
                            value = """
                                        {
                                          "success": true,
                                          "data": {
                                            "id": 1,
                                            "name": "Charley Smash",
                                            "description": "An interesting book about ...",
                                            "author": "Frank Oester",
                                            "category": "Drama",
                                            "publicationYear": 1765,
                                            "photoBase64": "Book photo",
                                            "city": "London",
                                            "isGift": true,
                                            "isExchanged": false,
                                            "meta": {
                                              "createdAt": "2026-03-23T13:00:58.815Z",
                                              "updatedAt": "2026-03-23T15:00:58.815Z",
                                              "deletedAt": null,
                                              "createdBy": 1,
                                              "updatedBy": 2,
                                              "createdRequestId": "4fa6b455-d7d0-4f90-9a07-df090a324401",
                                              "updatedRequestId": "5fa6b455-d7d0-4f90-9a07-df090a324401"
                                            }
                                          },
                                          "message": "The book 'Charley Smash' has been restored",
                                          "error": null
                                        }
                                """
                    )
            )
    )
    @PatchMapping(AdminPaths.ADMIN_PATH_BOOKS_ID_RESTORE)
    public ResponseEntity<?> adminRestoreBookById(
            @AuthenticationPrincipal UserDetails adminUser,

            @Parameter(description = "Book ID", example = "1")
            @PathVariable Long bookId,

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
                adminBookService.restoreBookById(
                        adminUser,
                        bookId,
                        parserUtil.ifMatchParser(ifMatch)
                ),
                request
        );
    }
}
