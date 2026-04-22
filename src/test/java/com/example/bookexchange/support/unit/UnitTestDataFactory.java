package com.example.bookexchange.support.unit;

import com.example.bookexchange.admin.dto.BanUserDTO;
import com.example.bookexchange.auth.dto.AuthLoginRequestDTO;
import com.example.bookexchange.book.dto.BookCategoryDTO;
import com.example.bookexchange.book.dto.BookCreateDTO;
import com.example.bookexchange.book.dto.BookUpdateDTO;
import com.example.bookexchange.book.model.Book;
import com.example.bookexchange.common.dto.PageQueryDTO;
import com.example.bookexchange.exchange.dto.RequestCreateDTO;
import com.example.bookexchange.exchange.model.Exchange;
import com.example.bookexchange.exchange.model.ExchangeStatus;
import com.example.bookexchange.report.dto.ReportCreateDTO;
import com.example.bookexchange.report.model.Report;
import com.example.bookexchange.report.model.ReportReason;
import com.example.bookexchange.report.model.ReportStatus;
import com.example.bookexchange.report.model.TargetType;
import com.example.bookexchange.support.TestReportStrings;
import com.example.bookexchange.user.dto.UserCreateDTO;
import com.example.bookexchange.user.dto.UserForgotPasswordDTO;
import com.example.bookexchange.user.dto.UserInitiateDeleteAccountDTO;
import com.example.bookexchange.user.dto.UserResetPasswordDTO;
import com.example.bookexchange.user.dto.UserResendEmailConfirmationDTO;
import com.example.bookexchange.user.dto.UserUpdateDTO;
import com.example.bookexchange.user.model.User;
import com.example.bookexchange.user.model.UserRole;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Base64;

import static org.springframework.security.core.userdetails.User.withUsername;

public final class UnitTestDataFactory {

    private static final String BASE64_PHOTO = Base64.getEncoder().encodeToString("photo".getBytes());
    private static final String PHOTO_URL = "https://test-bucket.s3.eu-central-1.amazonaws.com/test-image.jpg";

    private UnitTestDataFactory() {
    }

    public static User user(long id, String email, String nickname) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setNickname(nickname);
        user.setPassword("encoded-password");
        user.setEmailVerified(true);
        user.setLocale("en");
        user.setVersion(1L);
        user.addRole(UserRole.USER);
        return user;
    }

    public static User unverifiedUser(long id, String email, String nickname) {
        User user = user(id, email, nickname);
        user.setEmailVerified(false);
        return user;
    }

    public static Book book(long id, String name, User owner) {
        Book book = new Book();
        book.setId(id);
        book.setName(name);
        book.setDescription(name + " description");
        book.setAuthor("Author");
        book.setCategory(BookCategoryDTO.DRAMA.getProperty());
        book.setPublicationYear(2020);
        book.setPhotoUrl(PHOTO_URL);
        book.setCity("Berlin");
        book.setContactDetails("Telegram: reader");
        book.setIsGift(false);
        book.setIsExchanged(false);
        book.setUser(owner);
        book.setVersion(1L);
        return book;
    }

    public static Exchange exchange(
            long id,
            User sender,
            User receiver,
            Book senderBook,
            Book receiverBook,
            ExchangeStatus status
    ) {
        Exchange exchange = new Exchange();
        exchange.setId(id);
        exchange.setSenderUser(sender);
        exchange.setReceiverUser(receiver);
        exchange.setSenderBook(senderBook);
        exchange.setReceiverBook(receiverBook);
        exchange.setStatus(status);
        exchange.setVersion(1L);
        exchange.setIsReadBySender(false);
        exchange.setIsReadByReceiver(false);
        return exchange;
    }

    public static Report report(long id, User reporter, TargetType targetType, long targetId, ReportStatus status) {
        Report report = new Report();
        report.setId(id);
        report.setReporter(reporter);
        report.setTargetType(targetType);
        report.setTargetId(targetId);
        report.setReason(ReportReason.SPAM);
        report.setComment(TestReportStrings.comment("Looks suspicious"));
        report.setStatus(status);
        report.setVersion(1L);
        return report;
    }

    public static AuthLoginRequestDTO loginRequest(String email, String password) {
        return AuthLoginRequestDTO.builder()
                .email(email)
                .password(password)
                .build();
    }

    public static UserCreateDTO userCreateDto(String email, String nickname) {
        return UserCreateDTO.builder()
                .email(email)
                .password("Password-123")
                .nickname(nickname)
                .locale("en")
                .build();
    }

    public static UserForgotPasswordDTO forgotPasswordDto(String email) {
        return UserForgotPasswordDTO.builder()
                .email(email)
                .build();
    }

    public static UserResendEmailConfirmationDTO resendEmailConfirmationDto(String email) {
        return UserResendEmailConfirmationDTO.builder()
                .email(email)
                .build();
    }

    public static UserInitiateDeleteAccountDTO initiateDeleteAccountDto(String email) {
        return UserInitiateDeleteAccountDTO.builder()
                .email(email)
                .build();
    }

    public static UserUpdateDTO userUpdateDto(String nickname) {
        return UserUpdateDTO.builder()
                .nickname(nickname)
                .photoBase64(BASE64_PHOTO)
                .locale("en")
                .build();
    }

    public static UserResetPasswordDTO resetPasswordDto(String currentPassword, String newPassword) {
        return UserResetPasswordDTO.builder()
                .currentPassword(currentPassword)
                .newPassword(newPassword)
                .build();
    }

    public static BookCreateDTO bookCreateDto() {
        return BookCreateDTO.builder()
                .name("Book unit")
                .description("Book unit description")
                .author("Author unit")
                .category(BookCategoryDTO.DRAMA)
                .publicationYear(2021)
                .photoBase64(BASE64_PHOTO)
                .city("Berlin")
                .contactDetails("Telegram: book-owner")
                .isGift(false)
                .build();
    }

    public static BookUpdateDTO bookUpdateDto() {
        return BookUpdateDTO.builder()
                .name("Updated unit book")
                .description("Updated unit description")
                .author("Updated author")
                .category(BookCategoryDTO.NOVEL)
                .publicationYear(2022)
                .photoBase64(BASE64_PHOTO)
                .city("Hamburg")
                .contactDetails("Signal: updated")
                .isGift(true)
                .build();
    }

    public static RequestCreateDTO requestCreateDto(long receiverUserId, Long senderBookId, long receiverBookId) {
        return RequestCreateDTO.builder()
                .receiverUserId(receiverUserId)
                .senderBookId(senderBookId)
                .receiverBookId(receiverBookId)
                .build();
    }

    public static ReportCreateDTO reportCreateDto(TargetType targetType) {
        return ReportCreateDTO.builder()
                .targetType(targetType)
                .reason(ReportReason.SPAM)
                .comment(TestReportStrings.comment("Looks suspicious"))
                .build();
    }

    public static BanUserDTO permanentBanDto() {
        return BanUserDTO.builder()
                .bannedPermanently(true)
                .banReason(TestReportStrings.banReason("Fraud"))
                .build();
    }

    public static PageQueryDTO pageQuery(int pageIndex, int pageSize) {
        PageQueryDTO queryDTO = new PageQueryDTO();
        queryDTO.setPageIndex(pageIndex);
        queryDTO.setPageSize(pageSize);
        return queryDTO;
    }

    public static UserDetails adminPrincipal(String email) {
        return withUsername(email)
                .password("secret")
                .authorities("ROLE_ADMIN")
                .build();
    }

    public static Instant futureInstant() {
        return Instant.now().plusSeconds(3600);
    }
}
