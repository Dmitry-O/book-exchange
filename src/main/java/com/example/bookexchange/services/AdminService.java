package com.example.bookexchange.services;

import com.example.bookexchange.dto.*;
import com.example.bookexchange.models.*;
import org.springframework.data.domain.Page;

import java.util.Set;

public interface AdminService {

    String giveAdminRights(Long userId);

    String revokeAdminRights(Long userId);

    Page<UserAdminDTO> findUsers(Long userId, Integer pageIndex, Integer pageSize, String searchText, Set<UserRole> roles, Boolean onlyBannedUsers, UserType userType);

    String banUserById(User adminUser, Long userId, BanUserDTO banUserDTO, Long version);

    String unbanUserById(Long userId, Long version);

    String deleteBookById(Long bookId, Long version);

    String updateBookById(Long bookId, BookUpdateDTO dto, Long version);

    Page<ReportAdminDTO> findReports(Integer pageIndex, Integer pageSize, Set<ReportStatus> statuses, String sortDirection);

    Report findReportById(Long reportId);

    Book findBookById(Long bookId);

    User findUserById(Long userId);

    String resolveReport(Long reportId, Long version);

    String rejectReport(Long reportId, Long version);

    Page<BookAdminDTO> findBooks(BookSearchDTO dto, Integer pageIndex, Integer pageSize, BookType bookType);

    String restoreBookById(Long bookId, Long version);

    Page<ExchangeAdminDTO> findExchanges(Integer pageIndex, Integer pageSize, Set<ExchangeStatus> exchangeStatuses);

    ExchangeAdminDTO findExchangeById(Long exchangeId);
}
