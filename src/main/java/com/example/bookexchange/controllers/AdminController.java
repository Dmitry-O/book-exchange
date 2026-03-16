package com.example.bookexchange.controllers;

import com.example.bookexchange.authentication.CurrentUser;
import com.example.bookexchange.dto.*;
import com.example.bookexchange.mappers.AdminMapper;
import com.example.bookexchange.models.*;
import com.example.bookexchange.services.AdminService;
import com.example.bookexchange.services.UserService;
import com.example.bookexchange.util.ParserUtil;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
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
    private final AdminMapper adminMapper;
    private ParserUtil parserUtil;

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
    public ResponseEntity<ApiMessage> superAdminGiveAdminRights(@PathVariable Long userId) {
        return ResponseEntity.ok(new ApiMessage(adminService.giveAdminRights(userId)));
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PatchMapping(ADMIN_PATH_USERS_ID_REVOKE_ADMIN_RIGHTS)
    public ResponseEntity<ApiMessage> superAdminRevokeAdminRights(@PathVariable Long userId) {
        return ResponseEntity.ok(new ApiMessage(adminService.revokeAdminRights(userId)));
    }

    @GetMapping(ADMIN_PATH_USERS)
    public Page<UserAdminDTO> adminGetUsers(
            @CurrentUser Long userId,
            @RequestParam(value = "pageIndex", defaultValue = "0") Integer pageIndex,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize,
            @RequestParam(value = "searchText", required = false) String searchText,
            @RequestParam(value = "roles", required = false) Set<UserRole> roles,
            @RequestParam(value = "onlyBannedUsers", required = false) Boolean onlyBannedUsers,
            @RequestParam(value = "userType", defaultValue = "ALL") UserType userType
    ) {
        return adminService.findUsers(userId, pageIndex, pageSize, searchText, roles, onlyBannedUsers, userType);
    }

    @GetMapping(ADMIN_PATH_USERS_ID)
    public ResponseEntity<UserAdminDTO> adminGetUserById(@PathVariable Long userId) {
        User user = adminService.findUserById(userId);

        return ResponseEntity
                .ok()
                .eTag("\"" + user.getVersion() + "\"")
                .body(adminMapper.userToUserAdminDto(user));
    }

    @PatchMapping(ADMIN_PATH_USERS_ID_BAN)
    public ResponseEntity<ApiMessage> adminBanUser(
            @AuthenticationPrincipal User adminUser,
            @PathVariable Long userId,
            @RequestHeader("If-Match") String ifMatch,
            @Validated @RequestBody BanUserDTO banUserDTO
    ) {
        return ResponseEntity.ok(new ApiMessage(adminService.banUserById(adminUser, userId, banUserDTO, parserUtil.ifMatchParser(ifMatch))));
    }

    @PatchMapping(ADMIN_PATH_USERS_ID_UNBAN)
    public ResponseEntity<ApiMessage> adminUnbanUser(@PathVariable Long userId, @RequestHeader("If-Match") String ifMatch) {
        return ResponseEntity.ok(new ApiMessage(adminService.unbanUserById(userId, parserUtil.ifMatchParser(ifMatch))));
    }

    @DeleteMapping(ADMIN_PATH_USERS_ID)
    public ResponseEntity<ApiMessage> adminDeleteUserById(@PathVariable Long userId, @RequestHeader("If-Match") String ifMatch) {
        return ResponseEntity.ok(new ApiMessage(userService.deleteUser(userId, true, parserUtil.ifMatchParser(ifMatch))));
    }

    @GetMapping(ADMIN_PATH_BOOKS_SEARCH)
    public Page<BookAdminDTO> adminGetBooks(
            @RequestParam(value = "pageIndex", defaultValue = "0") Integer pageIndex,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize,
            @RequestParam(value = "bookType", defaultValue = "ALL") BookType bookType,
            @Validated @RequestBody(required = false) BookSearchDTO dto
    ) {
        return adminService.findBooks(dto, pageIndex, pageSize, bookType);
    }

    @GetMapping(ADMIN_PATH_BOOKS_ID)
    public ResponseEntity<BookAdminDTO> adminGetBookById(@PathVariable Long bookId) {
        Book book = adminService.findBookById(bookId);

        return ResponseEntity
                .ok()
                .eTag("\"" + book.getVersion() + "\"")
                .body(adminMapper.bookToBookAdminDto(book));
    }

    @PatchMapping(ADMIN_PATH_BOOKS_ID)
    public ResponseEntity<ApiMessage> adminUpdateBookById(
            @PathVariable Long bookId,
            @RequestHeader("If-Match") String ifMatch,
            @Validated @RequestBody BookUpdateDTO dto
    ) {
        return ResponseEntity.ok(new ApiMessage(adminService.updateBookById(bookId, dto, parserUtil.ifMatchParser(ifMatch))));
    }

    @DeleteMapping(ADMIN_PATH_BOOKS_ID)
    public ResponseEntity<ApiMessage> adminDeleteBookById(@PathVariable Long bookId, @RequestHeader("If-Match") String ifMatch) {
        return ResponseEntity.ok(new ApiMessage(adminService.deleteBookById(bookId, parserUtil.ifMatchParser(ifMatch))));
    }

    @PatchMapping(ADMIN_PATH_BOOKS_ID_RESTORE)
    public ResponseEntity<ApiMessage> adminRestoreBookById(@PathVariable Long bookId, @RequestHeader("If-Match") String ifMatch) {
        return ResponseEntity.ok(new ApiMessage(adminService.restoreBookById(bookId, parserUtil.ifMatchParser(ifMatch))));
    }

    @GetMapping(ADMIN_PATH_EXCHANGES)
    public Page<ExchangeAdminDTO> adminGetExchanges(
            @RequestParam(value = "pageIndex", defaultValue = "0") Integer pageIndex,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize,
            @RequestParam(value = "exchangeStatuses", required = false) Set<ExchangeStatus> exchangeStatuses
    ) {
        return adminService.findExchanges(pageIndex, pageSize, exchangeStatuses);
    }

    @GetMapping(ADMIN_PATH_EXCHANGES_ID)
    public ExchangeAdminDTO adminGetExchangeById(@PathVariable Long exchangeId) {
        return adminService.findExchangeById(exchangeId);
    }

    @GetMapping(ADMIN_PATH_REPORTS)
    public Page<ReportAdminDTO> adminGetReports(
            @RequestParam(value = "pageIndex", defaultValue = "0") Integer pageIndex,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize,
            @RequestParam(value = "reportStatuses", required = false) Set<ReportStatus> reportStatuses,
            @RequestParam(value = "sortDirection", defaultValue = "asc") String sortDirection
    ) {
        return adminService.findReports(pageIndex, pageSize, reportStatuses, sortDirection);
    }

    @GetMapping(ADMIN_PATH_REPORTS_ID)
    public ResponseEntity<ReportAdminDTO> adminGetReportById(@PathVariable Long reportId) {
        Report report = adminService.findReportById(reportId);

        return ResponseEntity
                .ok()
                .eTag("\"" + report.getVersion() + "\"")
                .body(adminMapper.reportToReportAdminDto(report));

    }

    @PatchMapping(ADMIN_PATH_REPORTS_ID_RESOLVE)
    public ResponseEntity<ApiMessage> adminReportResolve(@PathVariable Long reportId, @RequestHeader("If-Match") String ifMatch) {
        return ResponseEntity.ok(new ApiMessage(adminService.resolveReport(reportId, parserUtil.ifMatchParser(ifMatch))));
    }

    @PatchMapping(ADMIN_PATH_REPORTS_ID_REJECT)
    public ResponseEntity<ApiMessage> adminReportReject(@PathVariable Long reportId, @RequestHeader("If-Match") String ifMatch) {
        return ResponseEntity.ok(new ApiMessage(adminService.rejectReport(reportId, parserUtil.ifMatchParser(ifMatch))));
    }
}
