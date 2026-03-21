package com.example.bookexchange.services;

import com.example.bookexchange.core.result.Result;
import com.example.bookexchange.dto.*;
import com.example.bookexchange.models.*;
import org.springframework.data.domain.Page;

import java.util.Set;

public interface AdminService {

    Result<Void> giveAdminRights(Long userId);

    Result<Void> revokeAdminRights(Long userId);

    Result<Page<UserAdminDTO>> findUsers(Long userId, Integer pageIndex, Integer pageSize, String searchText, Set<UserRole> roles, Boolean onlyBannedUsers, UserType userType);

    Result<Void> banUserById(User adminUser, Long userId, BanUserDTO banUserDTO, Long version);

    Result<Void> unbanUserById(Long userId, Long version);

    Result<Void> deleteBookById(Long bookId, Long version);

    Result<BookAdminDTO> updateBookById(Long bookId, BookUpdateDTO dto, Long version);

    Result<Page<ReportAdminDTO>> findReports(Integer pageIndex, Integer pageSize, Set<ReportStatus> statuses, String sortDirection);

    Result<ReportAdminDTO> findReportById(Long reportId);

    Result<BookAdminDTO> findBookById(Long bookId);

    Result<UserAdminDTO> findUserById(Long userId);

    Result<Void> resolveReport(Long reportId, Long version);

    Result<Void> rejectReport(Long reportId, Long version);

    Result<Page<BookAdminDTO>> findBooks(BookSearchDTO dto, Integer pageIndex, Integer pageSize, BookType bookType);

    Result<BookAdminDTO> restoreBookById(Long bookId, Long version);

    Result<Page<ExchangeAdminDTO>> findExchanges(Integer pageIndex, Integer pageSize, Set<ExchangeStatus> exchangeStatuses);

    Result<ExchangeAdminDTO> findExchangeById(Long exchangeId);
}
