package com.example.bookexchange.services;

import com.example.bookexchange.dto.*;
import com.example.bookexchange.models.ReportStatus;
import com.example.bookexchange.models.User;
import com.example.bookexchange.models.UserRole;
import org.springframework.data.domain.Page;

import java.util.Optional;
import java.util.Set;

public interface AdminService {

    void giveAdminRights(Long userId);

    void revokeAdminRights(Long userId);

    Page<UserDTO> findUsers(User user, Integer pageIndex, Integer pageSize, String searchText, Set<UserRole> roles, Boolean onlyBannedUsers);

    void banUserById(User adminUser, Long userId, BanUserDTO banUserDTO);

    void unbanUserById(Long userId);

    Boolean deleteBookById(Long bookId);

    Optional<BookDTO> updateBookById(Long bookId, BookUpdateDTO dto);

    Page<ReportDTO> findReports(Integer pageIndex, Integer pageSize, Set<ReportStatus> statuses, String sortDirection);

    BookDTO findBookById(Long bookId);

    UserDTO findUserById(Long userId);

    void resolveReport(Long reportId);

    void rejectReport(Long reportId);
}
