package com.example.bookexchange.admin.api;

import com.example.bookexchange.admin.dto.BanUserDTO;
import com.example.bookexchange.admin.dto.UserAdminDTO;
import com.example.bookexchange.admin.service.AdminUserService;
import com.example.bookexchange.common.dto.PageQueryDTO;
import com.example.bookexchange.common.swagger.error_response.*;
import com.example.bookexchange.common.swagger.page_data_response.UserAdminPageData;
import com.example.bookexchange.common.util.ParserUtil;
import com.example.bookexchange.common.web.ResultResponseMapper;
import com.example.bookexchange.security.auth.CurrentUser;
import com.example.bookexchange.user.model.UserRole;
import com.example.bookexchange.user.model.UserType;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@Tag(name = "Administrator services - Users")
@RestController
@AllArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;
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
                                            "photoUrl": "https://book-exchange-prod.s3.eu-central-1.amazonaws.com/users/42/profile_photo_1712582410000.jpg",
                                            "bannedUntil": null,
                                            "bannedPermanently": false,
                                            "banReason": null,
                                            "roles": ["USER","ADMIN"],
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
                                            "photoUrl": "https://book-exchange-prod.s3.eu-central-1.amazonaws.com/users/42/profile_photo_1712582410000.jpg",
                                            "bannedUntil": null,
                                            "bannedPermanently": false,
                                            "banReason": null,
                                            "roles": ["USER"],
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

            @ParameterObject
            @Validated @ModelAttribute PageQueryDTO queryDTO,

            @Parameter(description = "Search text", example = "John Brown")
            @RequestParam(value = "searchText", required = false) String searchText,

            @Parameter(description = "Roles", example = "USER,ADMIN")
            @RequestParam(value = "roles", required = false) Set<UserRole> roles,

            @Parameter(description = "Only banned users", example = "true")
            @RequestParam(value = "onlyBannedUsers", required = false) Boolean onlyBannedUsers,

            @Parameter(description = "User type", example = "ALL")
            @RequestParam(value = "userType", defaultValue = "ALL") UserType userType,

            HttpServletRequest request
    ) {
        return responseMapper.map(
                adminUserService.findUsers(
                        userId,
                        queryDTO,
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
                                            "photoUrl": "https://book-exchange-prod.s3.eu-central-1.amazonaws.com/users/42/profile_photo_1712582410000.jpg",
                                            "bannedUntil": "2026-04-21T12:00:00Z",
                                            "bannedPermanently": false,
                                            "banReason": "Spam distribution",
                                            "roles": ["USER"],
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
                                            "photoUrl": "https://book-exchange-prod.s3.eu-central-1.amazonaws.com/users/42/profile_photo_1712582410000.jpg",
                                            "bannedUntil": null,
                                            "bannedPermanently": false,
                                            "banReason": null,
                                            "roles": ["USER"],
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
                                            "email": "anonymized-1@anonymized.anonymized",
                                            "nickname": "anonymized",
                                            "photoUrl": null,
                                            "bannedUntil": "2026-04-21T12:00:00Z",
                                            "bannedPermanently": false,
                                            "banReason": "Spam distribution",
                                            "roles": ["USER"],
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
}
