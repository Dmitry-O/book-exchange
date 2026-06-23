package com.example.bookexchange.common.demoemail;

import com.example.bookexchange.common.config.AppProperties;
import com.example.bookexchange.common.i18n.MessageKey;
import com.example.bookexchange.common.result.Result;
import com.example.bookexchange.common.result.ResultFactory;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.demo-email-sandbox.enabled", havingValue = "true")
public class MailpitDemoEmailSandboxService implements DemoEmailSandboxService {

    private static final int SESSION_ID_BYTES = 32;
    private static final int MIN_LIMIT = 1;
    private static final int MAX_LIMIT_FLOOR = 1;

    private final AppProperties appProperties;
    private final MailpitClient mailpitClient;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, SandboxSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, String> sandboxByEmail = new ConcurrentHashMap<>();
    private final Map<String, String> emailBySandbox = new ConcurrentHashMap<>();

    @Override
    public void attachSandboxHeader(String emailTo, MimeMessage message) {
        resolveSandboxForOutgoingEmail(emailTo)
                .ifPresent(sandboxId -> {
                    try {
                        message.setHeader(MAIL_HEADER_NAME, sandboxId);
                    } catch (MessagingException ex) {
                        log.warn("Failed to attach demo email sandbox header. emailTo={}, reason={}", emailTo, ex.getMessage(), ex);
                    }
                });
    }

    @Override
    public Result<DemoEmailSandboxSessionDTO> createSession(String requestedSandboxId, String email) {
        cleanupExpiredSessions();
        String normalizedEmail = normalizeEmail(email);
        SandboxSession session = normalizedEmail == null
                ? refreshOrCreateSession(requestedSandboxId)
                : getOrCreateSessionForEmail(normalizedEmail, requestedSandboxId);
        exposeSandboxId(session.id());

        return ResultFactory.ok(new DemoEmailSandboxSessionDTO(session.id(), session.expiresAt(), true));
    }

    @Override
    public Result<DemoEmailInboxDTO> getInbox(String requestedSandboxId, Integer limit) {
        try {
            cleanupExpiredSessions();
            SandboxSession session = getOrCreateSessionWithoutRefresh(requestedSandboxId);
            exposeSandboxId(session.id());

            List<DemoEmailMessageDTO> messages = findMessagesForSandbox(session.id(), clampLimit(limit)).stream()
                    .map(this::toDto)
                    .toList();

            return ResultFactory.ok(new DemoEmailInboxDTO(session.id(), session.expiresAt(), messages));
        } catch (RuntimeException ex) {
            log.warn("Failed to load demo Mailpit inbox. reason={}", ex.getMessage(), ex);
            return ResultFactory.error(MessageKey.SYSTEM_UNEXPECTED_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public Result<DemoEmailInboxDTO> clearInbox(String requestedSandboxId) {
        try {
            cleanupExpiredSessions();
            SandboxSession session = refreshOrCreateSession(requestedSandboxId);
            exposeSandboxId(session.id());
            deleteMessagesForSandbox(session.id());

            return ResultFactory.ok(new DemoEmailInboxDTO(session.id(), session.expiresAt(), List.of()));
        } catch (RuntimeException ex) {
            log.warn("Failed to clear demo Mailpit inbox. reason={}", ex.getMessage(), ex);
            return ResultFactory.error(MessageKey.SYSTEM_UNEXPECTED_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void clearAllSandboxState() {
        sessions.clear();
        sandboxByEmail.clear();
        emailBySandbox.clear();
    }

    @Scheduled(fixedDelayString = "${app.demo-email-sandbox.cleanup-interval-millis:300000}")
    void cleanupExpiredSessions() {
        if (!appProperties.getDemoEmailSandbox().isEnabled()) {
            return;
        }

        Instant now = Instant.now();
        List<String> expiredSandboxIds = sessions.values().stream()
                .filter(session -> session.expiresAt().isBefore(now))
                .map(SandboxSession::id)
                .toList();

        for (String sandboxId : expiredSandboxIds) {
            sessions.remove(sandboxId);
            sandboxByEmail.entrySet().removeIf(entry -> sandboxId.equals(entry.getValue()));
            emailBySandbox.remove(sandboxId);

            if (appProperties.getDemoEmailSandbox().isDeleteExpiredMessages()) {
                try {
                    deleteMessagesForSandbox(sandboxId);
                } catch (RuntimeException ex) {
                    log.warn("Failed to delete expired demo Mailpit messages. sandboxId={}, reason={}", sandboxId, ex.getMessage(), ex);
                }
            }
        }
    }

    private Optional<String> resolveSandboxForOutgoingEmail(String emailTo) {
        cleanupExpiredSessions();
        String normalizedEmail = normalizeEmail(emailTo);
        ServletRequestAttributes attributes = currentRequestAttributes();

        if (normalizedEmail != null) {
            SandboxSession session = getOrCreateSessionForEmail(normalizedEmail, null);
            exposeSandboxIdIfRequestMatches(attributes, session.id());
            return Optional.of(session.id());
        }

        if (attributes != null) {
            String requestedSandboxId = readSandboxId(attributes.getRequest());

            if (!isValidSandboxId(requestedSandboxId)) {
                return Optional.empty();
            }

            SandboxSession session = refreshOrCreateSession(requestedSandboxId);
            exposeSandboxId(attributes.getResponse(), session.id());
            return Optional.of(session.id());
        }

        return Optional.empty();
    }

    private List<MailpitMessageDetail> findMessagesForSandbox(String sandboxId, int limit) {
        int scanLimit = Math.max(limit, appProperties.getDemoEmailSandbox().getMaxMessages());
        List<MailpitMessageDetail> result = new ArrayList<>();

        for (MailpitMessageSummary summary : mailpitClient.listMessages(scanLimit)) {
            if (summary.id() == null || summary.id().isBlank()) {
                continue;
            }

            Map<String, List<String>> headers = mailpitClient.getHeaders(summary.id());

            if (containsSandboxHeader(headers, sandboxId)) {
                result.add(mailpitClient.getMessage(summary.id()));
            }

            if (result.size() >= limit) {
                break;
            }
        }

        return result.stream()
                .sorted(Comparator.comparing(
                        MailpitMessageDetail::createdAt,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .toList();
    }

    private void deleteMessagesForSandbox(String sandboxId) {
        List<String> messageIds = mailpitClient.listMessages(appProperties.getDemoEmailSandbox().getMaxMessages()).stream()
                .filter(summary -> summary.id() != null && !summary.id().isBlank())
                .filter(summary -> containsSandboxHeader(mailpitClient.getHeaders(summary.id()), sandboxId))
                .map(MailpitMessageSummary::id)
                .toList();

        mailpitClient.deleteMessages(messageIds);
    }

    private boolean containsSandboxHeader(Map<String, List<String>> headers, String sandboxId) {
        return headers.entrySet().stream()
                .filter(entry -> MAIL_HEADER_NAME.equalsIgnoreCase(entry.getKey()))
                .flatMap(entry -> entry.getValue().stream())
                .anyMatch(sandboxId::equals);
    }

    private DemoEmailMessageDTO toDto(MailpitMessageDetail message) {
        return new DemoEmailMessageDTO(
                message.id(),
                message.from(),
                message.to(),
                message.subject(),
                message.snippet(),
                message.createdAt(),
                message.html(),
                message.text()
        );
    }

    private SandboxSession refreshOrCreateSession(String requestedSandboxId) {
        return getOrCreateSession(requestedSandboxId, true);
    }

    private SandboxSession getOrCreateSessionWithoutRefresh(String requestedSandboxId) {
        return getOrCreateSession(requestedSandboxId, false);
    }

    private SandboxSession getOrCreateSession(String requestedSandboxId, boolean refreshExistingSession) {
        String sandboxId = isValidSandboxId(requestedSandboxId) ? requestedSandboxId : createSandboxId();

        return sessions.compute(sandboxId, (ignored, existingSession) -> {
            if (existingSession != null && existingSession.expiresAt().isAfter(Instant.now())) {
                if (refreshExistingSession) {
                    existingSession.extend(expiresAt());
                }

                return existingSession;
            }

            return new SandboxSession(sandboxId, expiresAt());
        });
    }

    private SandboxSession getOrCreateSessionForEmail(String normalizedEmail, String requestedSandboxId) {
        String existingSandboxId = sandboxByEmail.get(normalizedEmail);
        Optional<SandboxSession> existingSession = getValidSession(existingSandboxId);

        if (existingSession.isPresent()) {
            return refreshOrCreateSession(existingSession.get().id());
        }

        if (existingSandboxId != null) {
            sandboxByEmail.remove(normalizedEmail, existingSandboxId);
            emailBySandbox.remove(existingSandboxId, normalizedEmail);
        }

        String sandboxId = selectSandboxIdForEmail(normalizedEmail, requestedSandboxId);
        SandboxSession session = refreshOrCreateSession(sandboxId);
        bindEmailToSandbox(normalizedEmail, session.id());

        return session;
    }

    private String selectSandboxIdForEmail(String normalizedEmail, String requestedSandboxId) {
        if (!isValidSandboxId(requestedSandboxId)) {
            return createSandboxId();
        }

        String sandboxOwnerEmail = emailBySandbox.get(requestedSandboxId);

        return sandboxOwnerEmail == null || normalizedEmail.equals(sandboxOwnerEmail)
                ? requestedSandboxId
                : createSandboxId();
    }

    private Optional<SandboxSession> getValidSession(String sandboxId) {
        if (!isValidSandboxId(sandboxId)) {
            return Optional.empty();
        }

        SandboxSession session = sessions.get(sandboxId);

        if (session == null || session.expiresAt().isBefore(Instant.now())) {
            return Optional.empty();
        }

        return Optional.of(session);
    }

    private void bindEmailToSandbox(String email, String sandboxId) {
        String normalizedEmail = normalizeEmail(email);

        if (normalizedEmail != null && isValidSandboxId(sandboxId)) {
            String previousSandboxId = sandboxByEmail.put(normalizedEmail, sandboxId);

            if (previousSandboxId != null && !sandboxId.equals(previousSandboxId)) {
                emailBySandbox.remove(previousSandboxId, normalizedEmail);
            }

            String previousEmail = emailBySandbox.put(sandboxId, normalizedEmail);

            if (previousEmail != null && !normalizedEmail.equals(previousEmail)) {
                sandboxByEmail.remove(previousEmail, sandboxId);
            }
        }
    }

    private void exposeSandboxIdIfRequestMatches(ServletRequestAttributes attributes, String sandboxId) {
        if (attributes == null) {
            return;
        }

        String requestedSandboxId = readSandboxId(attributes.getRequest());

        if (sandboxId.equals(requestedSandboxId)) {
            exposeSandboxId(attributes.getResponse(), sandboxId);
        }
    }

    private String readSandboxId(HttpServletRequest request) {
        return request == null ? null : request.getHeader(REQUEST_HEADER_NAME);
    }

    private void exposeSandboxId(String sandboxId) {
        ServletRequestAttributes attributes = currentRequestAttributes();

        if (attributes != null) {
            exposeSandboxId(attributes.getResponse(), sandboxId);
        }
    }

    private void exposeSandboxId(HttpServletResponse response, String sandboxId) {
        if (response != null) {
            response.setHeader(REQUEST_HEADER_NAME, sandboxId);
        }
    }

    private ServletRequestAttributes currentRequestAttributes() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes;
        }

        return null;
    }

    private int clampLimit(Integer requestedLimit) {
        int max = Math.max(appProperties.getDemoEmailSandbox().getMaxMessages(), MAX_LIMIT_FLOOR);
        int value = requestedLimit == null ? max : requestedLimit;

        return Math.max(MIN_LIMIT, Math.min(value, max));
    }

    private Instant expiresAt() {
        return Instant.now().plus(appProperties.getDemoEmailSandbox().getSessionTtlMinutes(), ChronoUnit.MINUTES);
    }

    private String createSandboxId() {
        byte[] bytes = new byte[SESSION_ID_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private boolean isValidSandboxId(String sandboxId) {
        return sandboxId != null && sandboxId.matches("[A-Za-z0-9_-]{32,128}");
    }

    private String normalizeEmail(String email) {
        return email == null || email.isBlank() ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private static final class SandboxSession {

        private final String id;
        private volatile Instant expiresAt;

        private SandboxSession(String id, Instant expiresAt) {
            this.id = id;
            this.expiresAt = expiresAt;
        }

        private String id() {
            return id;
        }

        private Instant expiresAt() {
            return expiresAt;
        }

        private void extend(Instant expiresAt) {
            this.expiresAt = expiresAt;
        }

    }
}
