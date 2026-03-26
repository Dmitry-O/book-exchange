package com.example.bookexchange.admin.api;

import com.example.bookexchange.admin.service.AdminBookService;
import com.example.bookexchange.admin.service.AdminExchangeService;
import com.example.bookexchange.admin.service.AdminReportService;
import com.example.bookexchange.admin.service.AdminUserService;
import com.example.bookexchange.book.model.BookType;
import com.example.bookexchange.user.model.UserType;
import com.example.bookexchange.admin.dto.*;
import com.example.bookexchange.common.swagger.error_response.*;
import com.example.bookexchange.common.swagger.page_data_response.BookAdminPageData;
import com.example.bookexchange.common.swagger.page_data_response.ExchangeAdminPageData;
import com.example.bookexchange.common.swagger.page_data_response.ReportAdminPageData;
import com.example.bookexchange.common.swagger.page_data_response.UserAdminPageData;
import com.example.bookexchange.report.model.ReportStatus;
import com.example.bookexchange.security.auth.CurrentUser;
import com.example.bookexchange.book.dto.BookSearchDTO;
import com.example.bookexchange.book.dto.BookUpdateDTO;
import com.example.bookexchange.common.web.ResultResponseMapper;
import com.example.bookexchange.exchange.model.ExchangeStatus;
import com.example.bookexchange.common.util.ParserUtil;
import com.example.bookexchange.user.model.UserRole;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@Tag(name = "Administrator services")
@RestController
@AllArgsConstructor
public class AdminController {

    private final AdminUserService adminUserService;
    private final AdminBookService adminBookService;
    private final AdminReportService adminReportService;
    private final AdminExchangeService adminExchangeService;
    private ParserUtil parserUtil;
    private final ResultResponseMapper responseMapper;

    @UnauthorizedErrorResponse
    @ConflictErrorResponse
    @NotFoundErrorResponse
    @ForbiddenErrorResponse
    @ApiResponse(
            responseCode = "200",
            description = "Administrator rights were given to the user",
            headers = @Header(
                    name = "ETag",
                    description = "Entity version",
                    schema = @Schema(type = "string", example = "\"3\"")
            ),
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = UserAdminDTO.class),
                    examples = @ExampleObject(
                            value = """
                                        {
                                          "success": true,
                                          "data": {
                                            "id": 1,
                                            "email": "example@info.com",
                                            "nickname": "user_12345",
                                            "photoBase64": "User photo",
                                            "bannedUntil": null,
                                            "bannedPermanently": false,
                                            "banReason": null,
                                            "roles": "[USER,ADMIN]",
                                            "locale": "de",
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
                                          "message": "The user example@info.com has been granted administrator rights",
                                          "error": null
                                        }
                                """
                    )
            )
    )
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PatchMapping(AdminPaths.ADMIN_PATH_USERS_ID_GIVE_ADMIN_RIGHTS)
    public ResponseEntity<?> superAdminGiveAdminRights(
            @AuthenticationPrincipal UserDetails adminUser,

            @Parameter(description = "User ID", example = "1")
            @PathVariable Long userId,

            HttpServletRequest request
    ) {
        return responseMapper.map(adminUserService.giveAdminRights(adminUser, userId), request);
    }

    @UnauthorizedErrorResponse
    @NotFoundErrorResponse
    @BadRequestErrorResponse
    @ForbiddenErrorResponse
    @ApiResponse(
            responseCode = "200",
            description = "Administrator rights were revoked from the user",
            headers = @Header(
                    name = "ETag",
                    description = "Entity version",
                    schema = @Schema(type = "string", example = "\"3\"")
            ),
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = UserAdminDTO.class),
                    examples = @ExampleObject(
                            value = """
                                        {
                                          "success": true,
                                          "data": {
                                            "id": 1,
                                            "email": "example@info.com",
                                            "nickname": "user_12345",
                                            "photoBase64": "User photo",
                                            "bannedUntil": null,
                                            "bannedPermanently": false,
                                            "banReason": null,
                                            "roles": "[USER]",
                                            "locale": "de",
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
                                          "message": "The user example@info.com has had their administrator rights revoked",
                                          "error": null
                                        }
                                """
                    )
            )
    )
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PatchMapping(AdminPaths.ADMIN_PATH_USERS_ID_REVOKE_ADMIN_RIGHTS)
    public ResponseEntity<?> superAdminRevokeAdminRights(
            @AuthenticationPrincipal UserDetails adminUser,

            @Parameter(description = "User ID", example = "1")
            @PathVariable Long userId,

            HttpServletRequest request
    ) {
        return responseMapper.map(adminUserService.revokeAdminRights(adminUser, userId), request);
    }

    @UnauthorizedErrorResponse
    @NotFoundErrorResponse
    @ForbiddenErrorResponse
    @ApiResponse(
            responseCode = "200",
            description = "A list of returned users",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = UserAdminPageData.class)
            )
    )
    @GetMapping(AdminPaths.ADMIN_PATH_USERS)
    public ResponseEntity<?> adminGetUsers(
            @Parameter(description = "User ID", example = "1")
            @CurrentUser Long userId,

            @Parameter(description = "Page index, starts from 0", example = "0")
            @RequestParam(value = "pageIndex", defaultValue = "0") Integer pageIndex,

            @Parameter(description = "Page size", example = "20")
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize,

            @Parameter(description = "Search text", example = "John Brown")
            @RequestParam(value = "searchText", required = false) String searchText,

            @Parameter(description = "Roles", example = "USER,ADMIN")
            @RequestParam(value = "roles", required = false) Set<UserRole> roles,

            @Parameter(description = "Only banned users", example = "true")
            @RequestParam(value = "onlyBannedUsers", required = false) Boolean onlyBannedUsers,

            @Parameter(description = "User type", example = "ACTIVE,DELETED,ALL")
            @RequestParam(value = "userType", defaultValue = "ALL") UserType userType,

            HttpServletRequest request
    ) {
        return responseMapper.map(
                adminUserService.findUsers(
                        userId,
                        pageIndex,
                        pageSize,
                        searchText,
                        roles,
                        onlyBannedUsers,
                        userType
                ),
                request
        );
    }

    @UnauthorizedErrorResponse
    @NotFoundErrorResponse
    @ForbiddenErrorResponse
    @ApiResponse(
            responseCode = "200",
            description = "The returned user",
            headers = @Header(
                    name = "ETag",
                    description = "Entity version",
                    schema = @Schema(type = "string", example = "\"3\"")
            ),
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = UserAdminDTO.class)
            )
    )
    @GetMapping(AdminPaths.ADMIN_PATH_USERS_ID)
    public ResponseEntity<?> adminGetUserById(
            @AuthenticationPrincipal UserDetails adminUser,

            @Parameter(description = "User ID", example = "1")
            @PathVariable Long userId,

            HttpServletRequest request
    ) {
        return responseMapper.map(adminUserService.findUserById(adminUser, userId), request);
    }

    @UnauthorizedErrorResponse
    @ConflictErrorResponse
    @NotFoundErrorResponse
    @BadRequestErrorResponse
    @ForbiddenErrorResponse
    @ApiResponse(
            responseCode = "200",
            description = "The user has been banned",
            headers = @Header(
                    name = "ETag",
                    description = "Entity version",
                    schema = @Schema(type = "string", example = "\"3\"")
            ),
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = UserAdminDTO.class),
                    examples = @ExampleObject(
                            value = """
                                        {
                                          "success": true,
                                          "data": {
                                            "id": 1,
                                            "email": "example@info.com",
                                            "nickname": "user_12345",
                                            "photoBase64": "User photo",
                                            "bannedUntil": "2026-04-21 12:00:00",
                                            "bannedPermanently": false,
                                            "banReason": "Spam distribution",
                                            "roles": "[USER]",
                                            "locale": "de",
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
                                          "message": "The user example@info.com was banned",
                                          "error": null
                                        }
                                """
                    )
            )
    )
    @PatchMapping(AdminPaths.ADMIN_PATH_USERS_ID_BAN)
    public ResponseEntity<?> adminBanUser(
            @AuthenticationPrincipal UserDetails adminUser,

            @Parameter(description = "User ID", example = "1")
            @PathVariable Long userId,

            @Parameter(
                    name = "If-Match",
                    description = "Entity version for optimistic locking",
                    example = "\"3\"",
                    required = true
            )
            @RequestHeader("If-Match") String ifMatch,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Data to ban the user", required = true)
            @Validated @RequestBody BanUserDTO banUserDTO,

            HttpServletRequest request
    ) {
        return responseMapper.map(
                adminUserService.banUserById(
                        adminUser,
                        userId,
                        banUserDTO,
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
            description = "The user has been unbanned",
            headers = @Header(
                    name = "ETag",
                    description = "Entity version",
                    schema = @Schema(type = "string", example = "\"3\"")
            ),
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = UserAdminDTO.class),
                    examples = @ExampleObject(
                            value = """
                                        {
                                          "success": true,
                                          "data": {
                                            "id": 1,
                                            "email": "example@info.com",
                                            "nickname": "user_12345",
                                            "photoBase64": "User photo",
                                            "bannedUntil": null,
                                            "bannedPermanently": false,
                                            "banReason": null,
                                            "roles": "[USER]",
                                            "locale": "de",
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
                                          "message": "The user example@info.com was unbanned",
                                          "error": null
                                        }
                                """
                    )
            )
    )
    @PatchMapping(AdminPaths.ADMIN_PATH_USERS_ID_UNBAN)
    public ResponseEntity<?> adminUnbanUser(
            @AuthenticationPrincipal UserDetails adminUser,

            @Parameter(description = "User ID", example = "1")
            @PathVariable Long userId,

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
                adminUserService.unbanUserById(
                        adminUser,
                        userId,
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
            description = "The user has been deleted",
            headers = @Header(
                    name = "ETag",
                    description = "Entity version",
                    schema = @Schema(type = "string", example = "\"3\"")
            ),
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = UserAdminDTO.class),
                    examples = @ExampleObject(
                            value = """
                                        {
                                          "success": true,
                                          "data": {
                                            "id": 1,
                                            "email": "anonymized@anonymized.anonymized",
                                            "nickname": "anonymized",
                                            "photoBase64": null,
                                            "bannedUntil": "2026-04-21 12:00:00",
                                            "bannedPermanently": false,
                                            "banReason": "Spam distribution",
                                            "roles": "[USER]",
                                            "locale": "de",
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
                                          "message": "The user example@info.com has been deleted",
                                          "error": null
                                        }
                                """
                    )
            )
    )
    @DeleteMapping(AdminPaths.ADMIN_PATH_USERS_ID)
    public ResponseEntity<?> adminDeleteUserById(
            @AuthenticationPrincipal UserDetails adminUser,

            @Parameter(description = "User ID", example = "1")
            @PathVariable Long userId,

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
                adminUserService.deleteUser(
                        adminUser,
                        userId,
                        parserUtil.ifMatchParser(ifMatch)
                ),
                request
        );
    }

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
            @Parameter(description = "Page index, starts from 0", example = "0")
            @RequestParam(value = "pageIndex", defaultValue = "0") Integer pageIndex,

            @Parameter(description = "Page size", example = "20")
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize,

            @Parameter(description = "Book type", example = "ACTIVE,DELETED,ALL")
            @RequestParam(value = "bookType", defaultValue = "ALL") BookType bookType,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Book search data")
            @Validated @RequestBody(required = false) BookSearchDTO dto,

            HttpServletRequest request
    ) {
        return responseMapper.map(
                adminBookService.findBooks(
                        dto,
                        pageIndex,
                        pageSize,
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
                    schema = @Schema(implementation = UserAdminDTO.class),
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
            @Parameter(description = "Page index, starts from 0", example = "0")
            @RequestParam(value = "pageIndex", defaultValue = "0") Integer pageIndex,

            @Parameter(description = "Page size", example = "20")
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize,

            @Parameter(description = "Exchange statuses", example = "APPROVED,DECLINED,PENDING")
            @RequestParam(value = "exchangeStatuses", required = false) Set<ExchangeStatus> exchangeStatuses,

            HttpServletRequest request
    ) {
        return responseMapper.map(
                adminExchangeService.findExchanges(
                        pageIndex,
                        pageSize,
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

    @UnauthorizedErrorResponse
    @NotFoundErrorResponse
    @ForbiddenErrorResponse
    @BadRequestErrorResponse
    @ApiResponse(
            responseCode = "200",
            description = "A list of returned users' reports",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ReportAdminPageData.class)
            )
    )
    @GetMapping(AdminPaths.ADMIN_PATH_REPORTS)
    public ResponseEntity<?> adminGetReports(
            @Parameter(description = "Page index, starts from 0", example = "0")
            @RequestParam(value = "pageIndex", defaultValue = "0") Integer pageIndex,

            @Parameter(description = "Page size", example = "20")
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize,

            @Parameter(description = "Report statuses", example = "OPEN,RESOLVED,REJECTED")
            @RequestParam(value = "reportStatuses", required = false) Set<ReportStatus> reportStatuses,

            @Parameter(description = "Sort direction", example = "desc")
            @RequestParam(value = "sortDirection", defaultValue = "asc") String sortDirection,

            HttpServletRequest request
    ) {
        return responseMapper.map(
                adminReportService.findReports(
                        pageIndex,
                        pageSize,
                        reportStatuses,
                        sortDirection
                ),
                request
        );
    }

    @UnauthorizedErrorResponse
    @NotFoundErrorResponse
    @ForbiddenErrorResponse
    @ApiResponse(
            responseCode = "200",
            description = "The returned user's report",
            headers = @Header(
                    name = "ETag",
                    description = "Entity version",
                    schema = @Schema(type = "string", example = "\"3\"")
            ),
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ReportAdminDTO.class)
            )
    )
    @GetMapping(AdminPaths.ADMIN_PATH_REPORTS_ID)
    public ResponseEntity<?> adminGetReportById(
            @AuthenticationPrincipal UserDetails adminUser,

            @Parameter(description = "Report ID", example = "1")
            @PathVariable Long reportId,

            HttpServletRequest request
    ) {
        return responseMapper.map(adminReportService.findReportById(adminUser, reportId), request);
    }

    @UnauthorizedErrorResponse
    @ConflictErrorResponse
    @NotFoundErrorResponse
    @ForbiddenErrorResponse
    @ApiResponse(
            responseCode = "200",
            description = "The report has been resolved",
            headers = @Header(
                    name = "ETag",
                    description = "Entity version",
                    schema = @Schema(type = "string", example = "\"3\"")
            ),
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ReportAdminDTO.class),
                    examples = @ExampleObject(
                            value = """
                                        {
                                          "success": true,
                                          "data": {
                                            "id": 1,
                                            "targetType": "USER",
                                            "targetUrl": "http://current_host/api/v1/admin/books/1",
                                            "reason": "SPAM",
                                            "comment": "This user spams a lot with same books",
                                            "status": "RESOLVED",
                                            "reporter": {
                                              "id": 1,
                                              "email": "example@info.com",
                                              "nickname": "user_12345",
                                              "photoBase64": "User photo",
                                              "bannedUntil": null,
                                              "bannedPermanently": false,
                                              "banReason": null,
                                              "roles": "[USER]",
                                              "locale": "de"
                                            },
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
                                          "message": "The report has been resolved",
                                          "error": null
                                        }
                                """
                    )
            )
    )
    @PatchMapping(AdminPaths.ADMIN_PATH_REPORTS_ID_RESOLVE)
    public ResponseEntity<?> adminReportResolve(
            @AuthenticationPrincipal UserDetails adminUser,

            @Parameter(description = "Report ID", example = "1")
            @PathVariable Long reportId,

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
                adminReportService.resolveReport(
                        adminUser,
                        reportId,
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
            description = "The report has been rejected",
            headers = @Header(
                    name = "ETag",
                    description = "Entity version",
                    schema = @Schema(type = "string", example = "\"3\"")
            ),
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ReportAdminDTO.class),
                    examples = @ExampleObject(
                            value = """
                                        {
                                          "success": true,
                                          "data": {
                                            "id": 1,
                                            "targetType": "USER",
                                            "targetUrl": "http://current_host/api/v1/admin/books/1",
                                            "reason": "SPAM",
                                            "comment": "This user has terrible books",
                                            "status": "REJECTED",
                                            "reporter": {
                                              "id": 1,
                                              "email": "example@info.com",
                                              "nickname": "user_12345",
                                              "photoBase64": "User photo",
                                              "bannedUntil": null,
                                              "bannedPermanently": false,
                                              "banReason": null,
                                              "roles": "[USER]",
                                              "locale": "de"
                                            },
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
                                          "message": "The report has been rejected",
                                          "error": null
                                        }
                                """
                    )
            )
    )
    @PatchMapping(AdminPaths.ADMIN_PATH_REPORTS_ID_REJECT)
    public ResponseEntity<?> adminReportReject(
            @AuthenticationPrincipal UserDetails adminUser,

            @Parameter(description = "Report ID", example = "1")
            @PathVariable Long reportId,

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
                adminReportService.rejectReport(
                        adminUser,
                        reportId,
                        parserUtil.ifMatchParser(ifMatch)
                ),
                request
        );
    }
}
