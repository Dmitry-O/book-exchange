package com.example.bookexchange.controllers;

import com.example.bookexchange.authentication.CurrentUser;
import com.example.bookexchange.core.web.ResultResponseMapper;
import com.example.bookexchange.dto.*;
import com.example.bookexchange.models.*;
import com.example.bookexchange.services.AdminService;
import com.example.bookexchange.services.UserService;
import com.example.bookexchange.util.ParserUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@AllArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final UserService userService;
    private ParserUtil parserUtil;
    private final ResultResponseMapper responseMapper;

    public static final String ADMIN_PATH = "/api/v1/admin";
    public static final String ADMIN_PATH_USERS = ADMIN_PATH + "/users";
    public static final String ADMIN_PATH_BOOKS = ADMIN_PATH + "/books";
    public static final String ADMIN_PATH_BOOKS_SEARCH = ADMIN_PATH_BOOKS + "/search";
    public static final String ADMIN_PATH_BOOKS_ID = ADMIN_PATH_BOOKS + "/{bookId}";
    public static final String ADMIN_PATH_BOOKS_ID_RESTORE = ADMIN_PATH_BOOKS_ID + "/restore";
    public static final String ADMIN_PATH_REPORTS = ADMIN_PATH + "/reports";
    public static final String ADMIN_PATH_USERS_ID = ADMIN_PATH_USERS + "/{userId}";
    public static final String ADMIN_PATH_USERS_ID_BAN = ADMIN_PATH_USERS_ID + "/ban";
    public static final String ADMIN_PATH_USERS_ID_UNBAN = ADMIN_PATH_USERS_ID + "/unban";
    public static final String ADMIN_PATH_REPORTS_ID = ADMIN_PATH_REPORTS + "/{reportId}";
    public static final String ADMIN_PATH_REPORTS_ID_RESOLVE = ADMIN_PATH_REPORTS_ID + "/resolve";
    public static final String ADMIN_PATH_REPORTS_ID_REJECT = ADMIN_PATH_REPORTS_ID + "/reject";
    public static final String ADMIN_PATH_USERS_ID_GIVE_ADMIN_RIGHTS = ADMIN_PATH_USERS_ID + "/make-admin";
    public static final String ADMIN_PATH_USERS_ID_REVOKE_ADMIN_RIGHTS = ADMIN_PATH_USERS_ID + "/remove-admin";
    public static final String ADMIN_PATH_EXCHANGES = ADMIN_PATH + "/exchanges";
    public static final String ADMIN_PATH_EXCHANGES_ID = ADMIN_PATH_EXCHANGES + "/{exchangeId}";

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PatchMapping(ADMIN_PATH_USERS_ID_GIVE_ADMIN_RIGHTS)
    public ResponseEntity<?> superAdminGiveAdminRights(@PathVariable Long userId, HttpServletRequest request) {
        return responseMapper.map(adminService.giveAdminRights(userId), request);
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PatchMapping(ADMIN_PATH_USERS_ID_REVOKE_ADMIN_RIGHTS)
    public ResponseEntity<?> superAdminRevokeAdminRights(@PathVariable Long userId, HttpServletRequest request) {
        return responseMapper.map(adminService.revokeAdminRights(userId), request);
    }

    @GetMapping(ADMIN_PATH_USERS)
    public ResponseEntity<?> adminGetUsers(
            @CurrentUser Long userId,
            @RequestParam(value = "pageIndex", defaultValue = "0") Integer pageIndex,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize,
            @RequestParam(value = "searchText", required = false) String searchText,
            @RequestParam(value = "roles", required = false) Set<UserRole> roles,
            @RequestParam(value = "onlyBannedUsers", required = false) Boolean onlyBannedUsers,
            @RequestParam(value = "userType", defaultValue = "ALL") UserType userType,
            HttpServletRequest request
    ) {
        return responseMapper.map(
                adminService.findUsers(
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

    @GetMapping(ADMIN_PATH_USERS_ID)
    public ResponseEntity<?> adminGetUserById(@PathVariable Long userId, HttpServletRequest request) {
        return responseMapper.map(adminService.findUserById(userId), request);
    }

    @PatchMapping(ADMIN_PATH_USERS_ID_BAN)
    public ResponseEntity<?> adminBanUser(
            @AuthenticationPrincipal User adminUser,
            @PathVariable Long userId,
            @RequestHeader("If-Match") String ifMatch,
            @Validated @RequestBody BanUserDTO banUserDTO,
            HttpServletRequest request
    ) {
        return responseMapper.map(
                adminService.banUserById(
                        adminUser,
                        userId,
                        banUserDTO,
                        parserUtil.ifMatchParser(ifMatch)
                ),
                request
        );
    }

    @PatchMapping(ADMIN_PATH_USERS_ID_UNBAN)
    public ResponseEntity<?> adminUnbanUser(
            @PathVariable Long userId,
            @RequestHeader("If-Match") String ifMatch,
            HttpServletRequest request
    ) {
        return responseMapper.map(
                adminService.unbanUserById(
                        userId,
                        parserUtil.ifMatchParser(ifMatch)
                ),
                request
        );
    }

    @DeleteMapping(ADMIN_PATH_USERS_ID)
    public ResponseEntity<?> adminDeleteUserById(
            @PathVariable Long userId,
            @RequestHeader("If-Match") String ifMatch,
            HttpServletRequest request
    ) {
        return responseMapper.map(
                userService.deleteUser(
                        userId,
                        true,
                        parserUtil.ifMatchParser(ifMatch)
                ),
                request
        );
    }

    @GetMapping(ADMIN_PATH_BOOKS_SEARCH)
    public ResponseEntity<?> adminGetBooks(
            @RequestParam(value = "pageIndex", defaultValue = "0") Integer pageIndex,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize,
            @RequestParam(value = "bookType", defaultValue = "ALL") BookType bookType,
            @Validated @RequestBody(required = false) BookSearchDTO dto,
            HttpServletRequest request
    ) {
        return responseMapper.map(
                adminService.findBooks(
                        dto,
                        pageIndex,
                        pageSize,
                        bookType
                ),
                request
        );
    }

    @GetMapping(ADMIN_PATH_BOOKS_ID)
    public ResponseEntity<?> adminGetBookById(@PathVariable Long bookId, HttpServletRequest request) {
        return responseMapper.map(adminService.findBookById(bookId), request);
    }

    @PatchMapping(ADMIN_PATH_BOOKS_ID)
    public ResponseEntity<?> adminUpdateBookById(
            @PathVariable Long bookId,
            @RequestHeader("If-Match") String ifMatch,
            @Validated @RequestBody BookUpdateDTO dto,
            HttpServletRequest request
    ) {
        return responseMapper.map(
                adminService.updateBookById(
                        bookId,
                        dto,
                        parserUtil.ifMatchParser(ifMatch)
                ),
                request
        );
    }

    @DeleteMapping(ADMIN_PATH_BOOKS_ID)
    public ResponseEntity<?> adminDeleteBookById(
            @PathVariable Long bookId,
            @RequestHeader("If-Match") String ifMatch,
            HttpServletRequest request
    ) {
        return responseMapper.map(
                adminService.deleteBookById(
                        bookId,
                        parserUtil.ifMatchParser(ifMatch)
                ),
                request
        );
    }

    @PatchMapping(ADMIN_PATH_BOOKS_ID_RESTORE)
    public ResponseEntity<?> adminRestoreBookById(
            @PathVariable Long bookId,
            @RequestHeader("If-Match") String ifMatch,
            HttpServletRequest request
    ) {
        return responseMapper.map(
                adminService.restoreBookById(
                        bookId,
                        parserUtil.ifMatchParser(ifMatch)
                ),
                request
        );
    }

    @GetMapping(ADMIN_PATH_EXCHANGES)
    public ResponseEntity<?> adminGetExchanges(
            @RequestParam(value = "pageIndex", defaultValue = "0") Integer pageIndex,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize,
            @RequestParam(value = "exchangeStatuses", required = false) Set<ExchangeStatus> exchangeStatuses,
            HttpServletRequest request
    ) {
        return responseMapper.map(
                adminService.findExchanges(
                        pageIndex,
                        pageSize,
                        exchangeStatuses
                ),
                request
        );
    }

    @GetMapping(ADMIN_PATH_EXCHANGES_ID)
    public ResponseEntity<?> adminGetExchangeById(@PathVariable Long exchangeId, HttpServletRequest request) {
        return responseMapper.map(adminService.findExchangeById(exchangeId), request);
    }

    @GetMapping(ADMIN_PATH_REPORTS)
    public ResponseEntity<?> adminGetReports(
            @RequestParam(value = "pageIndex", defaultValue = "0") Integer pageIndex,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize,
            @RequestParam(value = "reportStatuses", required = false) Set<ReportStatus> reportStatuses,
            @RequestParam(value = "sortDirection", defaultValue = "asc") String sortDirection,
            HttpServletRequest request
    ) {
        return responseMapper.map(
                adminService.findReports(
                        pageIndex,
                        pageSize,
                        reportStatuses,
                        sortDirection
                ),
                request
        );
    }

    @GetMapping(ADMIN_PATH_REPORTS_ID)
    public ResponseEntity<?> adminGetReportById(@PathVariable Long reportId, HttpServletRequest request) {
        return responseMapper.map(adminService.findReportById(reportId), request);
    }

    @PatchMapping(ADMIN_PATH_REPORTS_ID_RESOLVE)
    public ResponseEntity<?> adminReportResolve(
            @PathVariable Long reportId,
            @RequestHeader("If-Match") String ifMatch,
            HttpServletRequest request
    ) {
        return responseMapper.map(
                adminService.resolveReport(
                        reportId,
                        parserUtil.ifMatchParser(ifMatch)
                ),
                request
        );
    }

    @PatchMapping(ADMIN_PATH_REPORTS_ID_REJECT)
    public ResponseEntity<?> adminReportReject(
            @PathVariable Long reportId,
            @RequestHeader("If-Match") String ifMatch,
            HttpServletRequest request
    ) {
        return responseMapper.map(
                adminService.rejectReport(
                        reportId,
                        parserUtil.ifMatchParser(ifMatch)
                ),
                request
        );
    }
}
