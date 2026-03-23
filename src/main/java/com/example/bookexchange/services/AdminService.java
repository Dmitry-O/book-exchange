package com.example.bookexchange.services;

import com.example.bookexchange.core.result.Result;
import com.example.bookexchange.dto.*;
import com.example.bookexchange.models.*;
import org.springframework.data.domain.Page;

import java.util.Set;

public interface AdminService {

    Result<UserAdminDTO> giveAdminRights(Long userId);

    Result<UserAdminDTO> revokeAdminRights(Long userId);

    Result<Page<UserAdminDTO>> findUsers(Long userId, Integer pageIndex, Integer pageSize, String searchText, Set<UserRole> roles, Boolean onlyBannedUsers, UserType userType);

    Result<UserAdminDTO> banUserById(User adminUser, Long userId, BanUserDTO banUserDTO, Long version);

    Result<UserAdminDTO> unbanUserById(Long userId, Long version);

    Result<BookAdminDTO> deleteBookById(Long bookId, Long version);

    Result<BookAdminDTO> updateBookById(Long bookId, BookUpdateDTO dto, Long version);

    Result<Page<ReportAdminDTO>> findReports(Integer pageIndex, Integer pageSize, Set<ReportStatus> statuses, String sortDirection);

    Result<ReportAdminDTO> findReportById(Long reportId);

    Result<BookAdminDTO> findBookById(Long bookId);

    Result<UserAdminDTO> findUserById(Long userId);

    Result<ReportAdminDTO> resolveReport(Long reportId, Long version);

    Result<ReportAdminDTO> rejectReport(Long reportId, Long version);

    Result<Page<BookAdminDTO>> findBooks(BookSearchDTO dto, Integer pageIndex, Integer pageSize, BookType bookType);

    Result<BookAdminDTO> restoreBookById(Long bookId, Long version);

    Result<Page<ExchangeAdminDTO>> findExchanges(Integer pageIndex, Integer pageSize, Set<ExchangeStatus> exchangeStatuses);

    Result<ExchangeAdminDTO> findExchangeById(Long exchangeId);

    Result<UserAdminDTO> deleteUser(Long userId, Long version);
}
