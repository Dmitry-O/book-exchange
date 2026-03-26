package com.example.bookexchange.admin.service;

import com.example.bookexchange.admin.dto.BanUserDTO;
import com.example.bookexchange.admin.dto.UserAdminDTO;
import com.example.bookexchange.common.result.Result;
import com.example.bookexchange.user.model.UserRole;
import com.example.bookexchange.user.model.UserType;
import org.springframework.data.domain.Page;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Set;

public interface AdminUserService {

    Result<Page<UserAdminDTO>> findUsers(Long userId, Integer pageIndex, Integer pageSize, String searchText, Set<UserRole> roles, Boolean onlyBannedUsers, UserType userType);

    Result<UserAdminDTO> findUserById(UserDetails adminUser, Long userId);

    Result<UserAdminDTO> giveAdminRights(UserDetails adminUser, Long userId);

    Result<UserAdminDTO> revokeAdminRights(UserDetails adminUser, Long userId);

    Result<UserAdminDTO> banUserById(UserDetails adminUser, Long userId, BanUserDTO banUserDTO, Long version);

    Result<UserAdminDTO> unbanUserById(UserDetails adminUser, Long userId, Long version);

    Result<UserAdminDTO> deleteUser(UserDetails adminUser, Long userId, Long version);

}
