package com.example.bookexchange.services;

import com.example.bookexchange.core.result.Result;
import com.example.bookexchange.dto.*;
import com.example.bookexchange.models.*;
import org.springframework.data.domain.Page;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Set;

public interface AdminService {

    Result<UserAdminDTO> giveAdminRights(UserDetails adminUser, Long userId);

    Result<UserAdminDTO> revokeAdminRights(UserDetails adminUser, Long userId);

    Result<Page<UserAdminDTO>> findUsers(Long userId, Integer pageIndex, Integer pageSize, String searchText, Set<UserRole> roles, Boolean onlyBannedUsers, UserType userType);

    Result<UserAdminDTO> banUserById(UserDetails adminUser, Long userId, BanUserDTO banUserDTO, Long version);

    Result<UserAdminDTO> unbanUserById(UserDetails adminUser, Long userId, Long version);

    Result<BookAdminDTO> deleteBookById(UserDetails adminUser, Long bookId, Long version);

    Result<BookAdminDTO> updateBookById(UserDetails adminUser, Long bookId, BookUpdateDTO dto, Long version);

    Result<Page<ReportAdminDTO>> findReports(Integer pageIndex, Integer pageSize, Set<ReportStatus> statuses, String sortDirection);

    Result<ReportAdminDTO> findReportById(UserDetails adminUser, Long reportId);

    Result<BookAdminDTO> findBookById(UserDetails adminUser, Long bookId);

    Result<UserAdminDTO> findUserById(UserDetails adminUser, Long userId);

    Result<ReportAdminDTO> resolveReport(UserDetails adminUser, Long reportId, Long version);

    Result<ReportAdminDTO> rejectReport(UserDetails adminUser, Long reportId, Long version);

    Result<Page<BookAdminDTO>> findBooks(BookSearchDTO dto, Integer pageIndex, Integer pageSize, BookType bookType);

    Result<BookAdminDTO> restoreBookById(UserDetails adminUser, Long bookId, Long version);

    Result<Page<ExchangeAdminDTO>> findExchanges(Integer pageIndex, Integer pageSize, Set<ExchangeStatus> exchangeStatuses);

    Result<ExchangeAdminDTO> findExchangeById(UserDetails adminUser, Long exchangeId);

    Result<UserAdminDTO> deleteUser(UserDetails adminUser, Long userId, Long version);
}
