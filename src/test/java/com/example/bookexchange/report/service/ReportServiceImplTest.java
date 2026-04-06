package com.example.bookexchange.report.service;

import com.example.bookexchange.book.repository.BookRepository;
import com.example.bookexchange.common.audit.service.AuditService;
import com.example.bookexchange.common.i18n.MessageKey;
import com.example.bookexchange.common.result.Result;
import com.example.bookexchange.report.dto.ReportCreateDTO;
import com.example.bookexchange.report.model.TargetType;
import com.example.bookexchange.report.repository.ReportRepository;
import com.example.bookexchange.support.unit.UnitFixtureIds;
import com.example.bookexchange.support.unit.UnitTestDataFactory;
import com.example.bookexchange.user.model.User;
import com.example.bookexchange.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static com.example.bookexchange.common.i18n.MessageKey.BOOK_NOT_FOUND;
import static com.example.bookexchange.common.i18n.MessageKey.REPORT_CANNOT_REPORT_YOUR_BOOK;
import static com.example.bookexchange.common.i18n.MessageKey.REPORT_CANNOT_REPORT_YOURSELF;
import static com.example.bookexchange.common.i18n.MessageKey.REPORT_SENT;
import static com.example.bookexchange.common.i18n.MessageKey.SYSTEM_INVALID_DATA;
import static com.example.bookexchange.support.unit.ResultAssertions.assertFailure;
import static com.example.bookexchange.support.unit.ResultAssertions.assertSuccess;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private ReportServiceImpl reportService;

    @Test
    void shouldReturnBadRequest_whenCreateReportTargetsReporterThemself() {
        User reporter = UnitTestDataFactory.user(UnitFixtureIds.VERIFIED_USER_ID, "reporter@example.com", "reporter_one");
        ReportCreateDTO dto = UnitTestDataFactory.reportCreateDto(TargetType.USER);

        when(userRepository.findById(reporter.getId())).thenReturn(Optional.of(reporter));

        Result<Void> result = reportService.createReport(reporter.getId(), reporter.getId(), dto);

        assertFailure(result, REPORT_CANNOT_REPORT_YOURSELF, HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldReturnBadRequest_whenCreateReportTargetsOwnBook() {
        User reporter = UnitTestDataFactory.user(UnitFixtureIds.VERIFIED_USER_ID, "reporter@example.com", "reporter_one");
        ReportCreateDTO dto = UnitTestDataFactory.reportCreateDto(TargetType.BOOK);
        var ownBook = UnitTestDataFactory.book(UnitFixtureIds.SENDER_BOOK_ID, "Own book", reporter);

        when(userRepository.findById(reporter.getId())).thenReturn(Optional.of(reporter));
        when(bookRepository.findByIdAndUserId(ownBook.getId(), reporter.getId())).thenReturn(Optional.of(ownBook));

        Result<Void> result = reportService.createReport(reporter.getId(), ownBook.getId(), dto);

        assertFailure(result, REPORT_CANNOT_REPORT_YOUR_BOOK, HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldReturnBadRequest_whenCreateReportTargetTypeIsMissing() {
        User reporter = UnitTestDataFactory.user(UnitFixtureIds.VERIFIED_USER_ID, "reporter@example.com", "reporter_one");
        ReportCreateDTO dto = UnitTestDataFactory.reportCreateDto(null);

        when(userRepository.findById(reporter.getId())).thenReturn(Optional.of(reporter));

        Result<Void> result = reportService.createReport(reporter.getId(), UnitFixtureIds.TARGET_USER_ID, dto);

        assertFailure(result, SYSTEM_INVALID_DATA, HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldReturnNotFound_whenCreateReportBookTargetDoesNotExist() {
        User reporter = UnitTestDataFactory.user(UnitFixtureIds.VERIFIED_USER_ID, "reporter@example.com", "reporter_one");
        ReportCreateDTO dto = UnitTestDataFactory.reportCreateDto(TargetType.BOOK);

        when(userRepository.findById(reporter.getId())).thenReturn(Optional.of(reporter));
        when(bookRepository.findByIdAndUserId(UnitFixtureIds.RECEIVER_BOOK_ID, reporter.getId())).thenReturn(Optional.empty());
        when(bookRepository.findById(UnitFixtureIds.RECEIVER_BOOK_ID)).thenReturn(Optional.empty());

        Result<Void> result = reportService.createReport(reporter.getId(), UnitFixtureIds.RECEIVER_BOOK_ID, dto);

        assertFailure(result, BOOK_NOT_FOUND, HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldPersistReport_whenCreateReportTargetIsValid() {
        User reporter = UnitTestDataFactory.user(UnitFixtureIds.VERIFIED_USER_ID, "reporter@example.com", "reporter_one");
        User target = UnitTestDataFactory.user(UnitFixtureIds.TARGET_USER_ID, "target@example.com", "target_one");
        ReportCreateDTO dto = UnitTestDataFactory.reportCreateDto(TargetType.USER);

        when(userRepository.findById(reporter.getId())).thenReturn(Optional.of(reporter));
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));

        Result<Void> result = reportService.createReport(reporter.getId(), target.getId(), dto);

        assertSuccess(result, HttpStatus.OK, REPORT_SENT);
        verify(reportRepository).save(any());
        verify(auditService).log(any());
    }
}
