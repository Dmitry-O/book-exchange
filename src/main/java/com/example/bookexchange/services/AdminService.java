package com.example.bookexchange.services;

import com.example.bookexchange.dto.*;
import com.example.bookexchange.models.ReportStatus;
import com.example.bookexchange.models.User;
import com.example.bookexchange.models.UserRole;
import org.springframework.data.domain.Page;

import java.util.Set;

public interface AdminService {

    String giveAdminRights(Long userId);

    String revokeAdminRights(Long userId);

    Page<UserDTO> findUsers(Long userId, Integer pageIndex, Integer pageSize, String searchText, Set<UserRole> roles, Boolean onlyBannedUsers);

    String banUserById(User adminUser, Long userId, BanUserDTO banUserDTO);

    String unbanUserById(Long userId);

    String deleteBookById(Long bookId);

    String updateBookById(Long bookId, BookUpdateDTO dto);

    Page<ReportDTO> findReports(Integer pageIndex, Integer pageSize, Set<ReportStatus> statuses, String sortDirection);

    BookDTO findBookById(Long bookId);

    UserDTO findUserById(Long userId);

    String resolveReport(Long reportId);

    String rejectReport(Long reportId);
}
