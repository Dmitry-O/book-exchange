package com.example.bookexchange.controllers;

import com.example.bookexchange.dto.*;
import com.example.bookexchange.models.ReportStatus;
import com.example.bookexchange.models.User;
import com.example.bookexchange.models.UserRole;
import com.example.bookexchange.services.AdminService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
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

    public static final String ADMIN_PATH = "/api/v1/admin";
    public static final String ADMIN_PATH_USERS = ADMIN_PATH + "/users";
    public static final String ADMIN_PATH_BOOKS = ADMIN_PATH + "/books";
    public static final String ADMIN_PATH_BOOKS_ID = ADMIN_PATH_BOOKS + "/{bookId}";
    public static final String ADMIN_PATH_REPORTS = ADMIN_PATH + "/reports";
    public static final String ADMIN_PATH_USERS_ID = ADMIN_PATH_USERS + "/{userId}";
    public static final String ADMIN_PATH_USERS_ID_BAN = ADMIN_PATH_USERS_ID + "/ban";
    public static final String ADMIN_PATH_USERS_ID_UNBAN = ADMIN_PATH_USERS_ID + "/unban";
    public static final String ADMIN_PATH_REPORTS_ID = ADMIN_PATH_REPORTS + "/{reportId}";
    public static final String ADMIN_PATH_REPORTS_ID_RESOLVE = ADMIN_PATH_REPORTS_ID + "/resolve";
    public static final String ADMIN_PATH_REPORTS_ID_REJECT = ADMIN_PATH_REPORTS_ID + "/reject";
    public static final String ADMIN_PATH_USERS_ID_GIVE_ADMIN_RIGHTS = ADMIN_PATH_USERS_ID + "/make-admin";
    public static final String ADMIN_PATH_USERS_ID_REVOKE_ADMIN_RIGHTS = ADMIN_PATH_USERS_ID + "/remove-admin";

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PatchMapping(ADMIN_PATH_USERS_ID_GIVE_ADMIN_RIGHTS)
    public ResponseEntity<String> superAdminGiveAdminRights(@PathVariable Long userId) {
        adminService.giveAdminRights(userId);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PatchMapping(ADMIN_PATH_USERS_ID_REVOKE_ADMIN_RIGHTS)
    public ResponseEntity<String> superAdminRevokeAdminRights(@PathVariable Long userId) {
        adminService.revokeAdminRights(userId);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping(ADMIN_PATH_USERS)
    public Page<UserDTO> adminGetUsers(
            @AuthenticationPrincipal User user,
            @RequestParam(value = "pageIndex", defaultValue = "0") Integer pageIndex,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize,
            @RequestParam(value = "searchText", required = false) String searchText,
            @RequestParam(value = "roles", required = false) Set<UserRole> roles,
            @RequestParam(value = "onlyBannedUsers", required = false) Boolean onlyBannedUsers
    ) {
        return adminService.findUsers(user, pageIndex, pageSize, searchText, roles, onlyBannedUsers);
    }

    @PatchMapping(ADMIN_PATH_USERS_ID_BAN)
    public ResponseEntity<String> adminBanUser(@AuthenticationPrincipal User adminUser, @PathVariable Long userId, BanUserDTO banUserDTO) {
        adminService.banUserById(adminUser, userId, banUserDTO);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PatchMapping(ADMIN_PATH_USERS_ID_UNBAN)
    public ResponseEntity<String> adminUnbanUser(@PathVariable Long userId) {
        adminService.unbanUserById(userId);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @DeleteMapping(ADMIN_PATH_BOOKS_ID)
    public ResponseEntity<String> adminDeleteBookById(@PathVariable Long bookId) {
        if (!adminService.deleteBookById(bookId)) {
            throw new NotFoundException();
        }

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PatchMapping(ADMIN_PATH_BOOKS_ID)
    public ResponseEntity<String> adminUpdateBookById(@PathVariable Long bookId, @Validated @RequestBody BookUpdateDTO dto) {
        if (adminService.updateBookById(bookId, dto).isEmpty()) {
            throw new NotFoundException("Das Buch mit ID " + bookId + " wurde nicht gefunden");
        }

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping(ADMIN_PATH_BOOKS_ID)
    public BookDTO adminGetBookById(@PathVariable Long bookId) {
        return adminService.findBookById(bookId);
    }

    @GetMapping(ADMIN_PATH_USERS_ID)
    public UserDTO adminGetUserById(@PathVariable Long userId) {
        return adminService.findUserById(userId);
    }

    @GetMapping(ADMIN_PATH_REPORTS)
    public Page<ReportDTO> adminGetReports(
            @RequestParam(value = "pageIndex", defaultValue = "0") Integer pageIndex,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize,
            @RequestParam(value = "statuses", defaultValue = "OPEN,RESOLVED,REJECTED") Set<ReportStatus> statuses,
            @RequestParam(value = "sortDirection", defaultValue = "asc") String sortDirection
    ) {
        return adminService.findReports(pageIndex, pageSize, statuses, sortDirection);
    }

    @PatchMapping(ADMIN_PATH_REPORTS_ID_RESOLVE)
    public ResponseEntity<String> adminReportResolve(@PathVariable Long reportId) {
        adminService.resolveReport(reportId);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PatchMapping(ADMIN_PATH_REPORTS_ID_REJECT)
    public ResponseEntity<String> adminReportReject(@PathVariable Long reportId) {
        adminService.rejectReport(reportId);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
