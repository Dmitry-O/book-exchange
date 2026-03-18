package com.example.bookexchange.services;

import com.example.bookexchange.dto.*;
import com.example.bookexchange.exception.BadRequestException;
import com.example.bookexchange.exception.EntityExistsException;
import com.example.bookexchange.exception.NotFoundException;
import com.example.bookexchange.mappers.*;
import com.example.bookexchange.models.*;
import com.example.bookexchange.repositories.*;
import com.example.bookexchange.specification.BookSpecificationBuilder;
import com.example.bookexchange.specification.UserSpecificationBuilder;
import com.example.bookexchange.util.Helper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Service
@AllArgsConstructor
public class AdminServiceImpl extends BaseServiceImpl<User, Long> implements AdminService {

    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final ReportRepository reportRepository;
    private final ExchangeRepository exchangeRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final BookMapper bookMapper;
    private final ReportMapper reportMapper;
    private final AdminMapper adminMapper;
    private final Helper helper;

    private final MessageService messageService;

    @Transactional
    @Override
    public String giveAdminRights(Long userId) {
        User user = findOrThrow(userRepository, userId, MessageKey.ADMIN_USER_NOT_FOUND);

        if (!user.getRoles().contains(UserRole.ADMIN)) {
            user.addRole(UserRole.ADMIN);
            userRepository.save(user);
        } else {
            throw new EntityExistsException(MessageKey.ADMIN_USER_ALREADY_ADMIN, user.getEmail());
        }

        return messageService.getMessage(MessageKey.ADMIN_RIGHTS_GIVEN, user.getEmail());
    }

    @Transactional
    @Override
    public String revokeAdminRights(Long userId) {
        User user = findOrThrow(userRepository, userId, MessageKey.ADMIN_USER_NOT_FOUND);

        if (user.getRoles().contains(UserRole.ADMIN)) {
            user.removeRole(UserRole.ADMIN);
            userRepository.save(user);
        } else {
            throw new BadRequestException(MessageKey.ADMIN_USER_NOT_ADMIN, user.getEmail());
        }

        return messageService.getMessage(MessageKey.ADMIN_RIGHTS_REVOKED, user.getEmail());
    }

    @Transactional(readOnly = true)
    @Override
    public Page<UserAdminDTO> findUsers(Long userId, Integer pageIndex, Integer pageSize, String searchText, Set<UserRole> roles, Boolean onlyBannedUsers, UserType userType) {
        User user = findOrThrow(userRepository, userId, MessageKey.ADMIN_USER_NOT_FOUND);

        Boolean isUserSuperAdmin = user.getRoles().contains(UserRole.SUPER_ADMIN);

        Specification<User> specification = UserSpecificationBuilder.build(searchText, roles, onlyBannedUsers, isUserSuperAdmin, userType);

        Pageable pageable;

        pageable = PageRequest.of(pageIndex, pageSize);

        Page<User> userPage = userRepository.findAll(specification, pageable);

        return userPage.map(adminMapper::userToUserAdminDto);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<BookAdminDTO> findBooks(BookSearchDTO dto, Integer pageIndex, Integer pageSize, BookType bookType) {
        Specification<Book> specification = BookSpecificationBuilder.build(dto, bookType);

        Pageable pageable;

        if (dto.getSortBy() != null && dto.getSortDirection() != null) {
            Sort.Direction direction = dto.getSortDirection().equalsIgnoreCase(("desc"))
                    ? Sort.Direction.DESC
                    : Sort.Direction.ASC;

            pageable = PageRequest.of(pageIndex, pageSize, Sort.by(direction, dto.getSortBy()));
        } else {
            pageable = PageRequest.of(pageIndex, pageSize);
        }

        Page<Book> bookPage = bookRepository.findAll(specification, pageable);

        return bookPage.map(adminMapper::bookToBookAdminDto);
    }

    @Transactional
    @Override
    public String banUserById(User adminUser, Long userId, BanUserDTO banUserDTO, Long version) {
        User user = findOrThrow(userRepository, userId, MessageKey.ADMIN_USER_NOT_FOUND);

        helper.checkEntityVersion(user.getVersion(), version);

        if (adminUser.getId().equals(user.getId())){
            throw new BadRequestException(MessageKey.ADMIN_CANT_BAN_YOURSELF);
        }

        if (banUserDTO.isBannedPermanently()) {
            user.setBannedPermanently(true);
        } else if (banUserDTO.getBannedUntil() != null) {
            user.setBannedUntil(OffsetDateTime.parse(banUserDTO.getBannedUntil()).toInstant());
        } else {
            throw new BadRequestException(MessageKey.ADMIN_REQUEST_NOT_VALID);
        }

        user.setBanReason(banUserDTO.getBanReason());

        refreshTokenRepository.deleteAll(new HashSet<>(user.getRefreshTokens()));

        return messageService.getMessage(MessageKey.ADMIN_USER_BANNED, user.getEmail());
    }

    @Transactional
    @Override
    public String unbanUserById(Long userId, Long version) {
        User user = findOrThrow(userRepository, userId, MessageKey.ADMIN_USER_NOT_FOUND);

        helper.checkEntityVersion(user.getVersion(), version);

        user.setBannedUntil(null);
        user.setBannedPermanently(false);
        user.setBanReason(null);

        userRepository.save(user);

        return messageService.getMessage(MessageKey.ADMIN_USER_UNBANNED, user.getEmail());
    }

    @Transactional
    @Override
    public String deleteBookById(Long bookId, Long version) {
        Book book = bookRepository.findById(bookId).orElseThrow(() -> new NotFoundException(MessageKey.ADMIN_BOOK_NOT_FOUND, bookId));

        helper.checkEntityVersion(book.getVersion(), version);

        book.setDeletedAt(Instant.now());

        return messageService.getMessage(MessageKey.ADMIN_BOOK_DELETED, book.getName());
    }

    @Transactional
    @Override
    public String updateBookById(Long bookId, BookUpdateDTO dto, Long version) {
        Book book = bookRepository.findById(bookId).orElseThrow(() -> new NotFoundException(MessageKey.ADMIN_BOOK_NOT_FOUND, bookId));

        helper.checkEntityVersion(book.getVersion(), version);
        bookMapper.updateBookDtoToBook(dto, book);
        bookRepository.save(book);

        return messageService.getMessage(MessageKey.ADMIN_BOOK_UPDATED, book.getName());
    }

    @Transactional(readOnly = true)
    @Override
    public Book findBookById(Long bookId) {
        return bookRepository.findById(bookId).orElseThrow(() -> new NotFoundException(MessageKey.ADMIN_BOOK_NOT_FOUND, bookId));
    }

    @Transactional(readOnly = true)
    @Override
    public User findUserById(Long userId) {
        return findOrThrow(userRepository, userId, MessageKey.ADMIN_USER_NOT_FOUND);
    }

    @Transactional
    @Override
    public String resolveReport(Long reportId, Long version) {
        Report report = reportRepository.findById(reportId).orElseThrow(() -> new NotFoundException(MessageKey.ADMIN_REPORT_NOT_FOUND, reportId));

        helper.checkEntityVersion(report.getVersion(), version);

        report.setStatus(ReportStatus.RESOLVED);

        reportRepository.save(report);

        return messageService.getMessage(MessageKey.ADMIN_REPORT_RESOLVED);
    }

    @Transactional
    @Override
    public String rejectReport(Long reportId, Long version) {
        Report report = reportRepository.findById(reportId).orElseThrow(() -> new NotFoundException(MessageKey.ADMIN_REPORT_NOT_FOUND, reportId));

        helper.checkEntityVersion(report.getVersion(), version);

        report.setStatus(ReportStatus.REJECTED);

        reportRepository.save(report);

        return messageService.getMessage(MessageKey.ADMIN_REPORT_REJECTED);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<ReportAdminDTO> findReports(Integer pageIndex, Integer pageSize, Set<ReportStatus> reportStatuses, String sortDirection) {
        Pageable pageable;

        Sort.Direction direction = sortDirection.equalsIgnoreCase(("desc"))
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        pageable = PageRequest.of(pageIndex, pageSize, Sort.by(direction, "createdAt"));

        Page<Report> reportPage;

        if (reportStatuses != null) {
            reportPage = reportRepository.findByStatusIn(reportStatuses, pageable);
        } else {
            reportPage = reportRepository.findAll(pageable);
        }

        return reportPage.map(reportMapper::reportToReportDto);
    }

    @Transactional
    @Override
    public String restoreBookById(Long bookId, Long version) {
        Book book = bookRepository.findById(bookId).orElseThrow(() -> new NotFoundException(MessageKey.ADMIN_BOOK_NOT_FOUND, bookId));

        helper.checkEntityVersion(book.getVersion(), version);

        book.setDeletedAt(null);

        return messageService.getMessage(MessageKey.ADMIN_BOOK_RESTORED, book.getName());
    }

    @Override
    public Page<ExchangeAdminDTO> findExchanges(Integer pageIndex, Integer pageSize, Set<ExchangeStatus> exchangeStatuses) {
        Pageable pageable = PageRequest.of(pageIndex, pageSize);

        Page<Exchange> pendingExchangesPage;

        if (exchangeStatuses != null) {
            pendingExchangesPage = exchangeRepository.findByStatusIn(exchangeStatuses, pageable);
        } else {
            pendingExchangesPage = exchangeRepository.findAll(pageable);
        }

        return pendingExchangesPage.map(adminMapper::exchangeToExchangeAdminDto);
    }

    @Override
    public ExchangeAdminDTO findExchangeById(Long exchangeId) {
        Exchange exchange = exchangeRepository.findById(exchangeId).orElseThrow(() -> new NotFoundException(MessageKey.ADMIN_EXCHANGE_NOT_FOUND, exchangeId));

        return adminMapper.exchangeToExchangeAdminDto(exchange);
    }

    @Override
    public Report findReportById(Long reportId) {
        return reportRepository.findById(reportId).orElseThrow(() -> new NotFoundException(MessageKey.ADMIN_REPORT_NOT_FOUND, reportId));
    }
}
