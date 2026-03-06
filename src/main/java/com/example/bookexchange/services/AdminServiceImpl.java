package com.example.bookexchange.services;

import com.example.bookexchange.controllers.NotFoundException;
import com.example.bookexchange.dto.*;
import com.example.bookexchange.mappers.BookMapper;
import com.example.bookexchange.mappers.ReportMapper;
import com.example.bookexchange.mappers.UserMapper;
import com.example.bookexchange.models.*;
import com.example.bookexchange.repositories.BookRepository;
import com.example.bookexchange.repositories.ReportRepository;
import com.example.bookexchange.repositories.UserRepository;
import com.example.bookexchange.specification.UserSpecificationBuilder;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

@Service
@AllArgsConstructor
public class AdminServiceImpl extends BaseServiceImpl<User, Long> implements AdminService {

    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final ReportRepository reportRepository;
    private final UserMapper userMapper;
    private final BookMapper bookMapper;
    private final ReportMapper reportMapper;

    @Transactional
    @Override
    public void giveAdminRights(Long userId) {
        User user = findOrThrow(userRepository, userId, "Der Benutzer mit ID " + userId + " wurde nicht gefunden");

        if (!user.getRoles().contains(UserRole.ADMIN)) {
            user.addRole(UserRole.ADMIN);
            userRepository.save(user);
        } else {
            throw new IllegalStateException("Der Benutzer mit ID " + userId + " ist bereits Administrator");
        }
    }

    @Transactional
    @Override
    public void revokeAdminRights(Long userId) {
        User user = findOrThrow(userRepository, userId, "Der Benutzer mit ID " + userId + " wurde nicht gefunden");

        if (user.getRoles().contains(UserRole.ADMIN)) {
            user.removeRole(UserRole.ADMIN);
            userRepository.save(user);
        } else {
            throw new IllegalStateException("Der Benutzer mit ID " + userId + " ist kein Administrator");
        }
    }

    @Override
    public Page<UserDTO> findUsers(User user, Integer pageIndex, Integer pageSize, String searchText, Set<UserRole> roles, Boolean onlyBannedUsers) {
        Boolean isUserSuperAdmin = user.getRoles().contains(UserRole.SUPER_ADMIN);

        Specification<User> specification = UserSpecificationBuilder.build(searchText, roles, onlyBannedUsers, isUserSuperAdmin);

        Pageable pageable;

        pageable = PageRequest.of(pageIndex, pageSize);

        Page<User> userPage = userRepository.findAll(specification, pageable);

        return userPage.map(userMapper::userToUserDto);
    }

    @Transactional
    @Override
    public void banUserById(User adminUser, Long userId, BanUserDTO banUserDTO) {
        User user = findOrThrow(userRepository, userId, "Der Benutzer mit ID " + userId + " wurde nicht gefunden");

        if (adminUser.getId().equals(user.getId())){
            throw new IllegalArgumentException("Sie können sich nicht selbst sperren");
        }

        if (banUserDTO.isBannedPermanently()) {
            user.setBannedPermanently(true);
        } else if (banUserDTO.getBannedUntil() != null) {
            user.setBannedUntil(OffsetDateTime.parse(banUserDTO.getBannedUntil()).toInstant());
        } else {
            throw new IllegalArgumentException("Die Anfrage ist ungültig");
        }

        user.setBanReason(banUserDTO.getBanReason());

        userRepository.save(user);
    }

    @Transactional
    @Override
    public void unbanUserById(Long userId) {
        User user = findOrThrow(userRepository, userId, "Der Benutzer mit ID " + userId + " wurde nicht gefunden");

        user.setBannedUntil(null);
        user.setBannedPermanently(false);
        user.setBanReason(null);

        userRepository.save(user);
    }

    @Transactional
    @Override
    public Boolean deleteBookById(Long bookId) {
        if (bookRepository.existsById(bookId)) {
            bookRepository.deleteById(bookId);

            return true;
        }

        return false;
    }

    @Transactional
    @Override
    public Optional<BookDTO> updateBookById(Long bookId, BookUpdateDTO dto) {
        AtomicReference<Optional<BookDTO>> atomicReference = new AtomicReference<>();

        bookRepository.findById(bookId).ifPresentOrElse(foundBook -> {
            foundBook.setName(!dto.getName().isEmpty() ? dto.getName() : foundBook.getName());
            foundBook.setDescription(!dto.getDescription().isEmpty() ? dto.getDescription() : foundBook.getDescription());
            foundBook.setAuthor(!dto.getAuthor().isEmpty() ? dto.getAuthor() : foundBook.getAuthor());
            foundBook.setCategory(!dto.getCategory().isEmpty() ? dto.getCategory() : foundBook.getCategory());
            foundBook.setPublicationYear(dto.getPublicationYear() != null ? dto.getPublicationYear() : foundBook.getPublicationYear());
            foundBook.setPhotoBase64(!dto.getPhotoBase64().isEmpty() ? dto.getPhotoBase64() : foundBook.getPhotoBase64());
            foundBook.setCity(!dto.getCity().isEmpty() ? dto.getCity() : foundBook.getCity());
            foundBook.setIsGift(dto.getIsGift() != null ?  dto.getIsGift() : foundBook.getIsGift());
            foundBook.setContactDetails(!dto.getContactDetails().isEmpty() ? dto.getContactDetails() : foundBook.getContactDetails());

            atomicReference.set(
                    Optional.of(
                            bookMapper.bookToBookDto(
                                    bookRepository.save(foundBook)
                            )
                    )
            );
        }, () -> atomicReference.set(Optional.empty()));

        return atomicReference.get();
    }

    @Override
    public BookDTO findBookById(Long bookId) {
        Book book = bookRepository.findById(bookId).orElseThrow(() -> new NotFoundException("Das Buch mit ID " + bookId + " wurde nicht gefunden"));

        return bookMapper.bookToBookDto(book);
    }

    @Override
    public UserDTO findUserById(Long userId) {
        User user = findOrThrow(userRepository, userId, "Der Benutzer mit ID " + userId + " wurde nicht gefunden");

        return userMapper.userToUserDto(user);
    }

    @Override
    public void resolveReport(Long reportId) {
        Report report = reportRepository.findById(reportId).orElseThrow(() -> new NotFoundException("Die Berichtserstellung mit ID " + reportId + " wurde nicht gefunden"));

        report.setStatus(ReportStatus.RESOLVED);

        reportRepository.save(report);
    }

    @Override
    public void rejectReport(Long reportId) {
        Report report = reportRepository.findById(reportId).orElseThrow(() -> new NotFoundException("Die Berichtserstellung mit ID " + reportId + " wurde nicht gefunden"));

        report.setStatus(ReportStatus.REJECTED);

        reportRepository.save(report);
    }

    @Override
    public Page<ReportDTO> findReports(Integer pageIndex, Integer pageSize, Set<ReportStatus> statuses, String sortDirection) {
        Pageable pageable;

        Sort.Direction direction = sortDirection.equalsIgnoreCase(("desc"))
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        pageable = PageRequest.of(pageIndex, pageSize, Sort.by(direction, "createdAt"));

        Page<Report> reportPage = reportRepository.findByStatusIn(statuses, pageable);

        return reportPage.map(reportMapper::reportToReportDto);
    }
}
