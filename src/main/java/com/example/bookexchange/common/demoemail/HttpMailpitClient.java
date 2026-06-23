package com.example.bookexchange.common.demoemail;

import com.example.bookexchange.common.config.AppProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.demo-email-sandbox.enabled", havingValue = "true")
public class HttpMailpitClient implements MailpitClient {

    private static final int OK_MIN = 200;
    private static final int OK_MAX = 299;
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);

    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .build();

    @Override
    public List<MailpitMessageSummary> listMessages(int limit) {
        JsonNode root = readJson(get(apiUrl("/api/v1/messages?start=0&limit=" + limit)));
        JsonNode messages = firstArray(root, "messages", "Messages");
        List<MailpitMessageSummary> result = new ArrayList<>();

        if (messages == null) {
            return result;
        }

        for (JsonNode message : messages) {
            result.add(toSummary(message));
        }

        return result;
    }

    @Override
    public Map<String, List<String>> getHeaders(String messageId) {
        JsonNode root = readJson(get(apiUrl("/api/v1/message/" + encode(messageId) + "/headers")));
        return parseHeaders(root);
    }

    @Override
    public MailpitMessageDetail getMessage(String messageId) {
        JsonNode root = readJson(get(apiUrl("/api/v1/message/" + encode(messageId))));
        return toDetail(root);
    }

    @Override
    public void deleteMessages(List<String> messageIds) {
        if (messageIds.isEmpty()) {
            return;
        }

        try {
            String body = objectMapper.writeValueAsString(Map.of("IDs", messageIds));
            send(HttpRequest.newBuilder(URI.create(apiUrl("/api/v1/messages")))
                    .header("Content-Type", "application/json")
                    .method("DELETE", HttpRequest.BodyPublishers.ofString(body))
                    .build());
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to serialize Mailpit delete request", ex);
        }
    }

    @Override
    public void deleteAllMessages(int batchSize, int maxBatches) {
        for (int batchIndex = 0; batchIndex < maxBatches; batchIndex++) {
            List<String> messageIds = listMessages(batchSize).stream()
                    .map(MailpitMessageSummary::id)
                    .filter(messageId -> messageId != null && !messageId.isBlank())
                    .toList();

            if (messageIds.isEmpty()) {
                return;
            }

            deleteMessages(messageIds);
        }

        throw new IllegalStateException("Mailpit cleanup did not finish after " + maxBatches + " batches");
    }

    private HttpRequest get(String url) {
        return requestBuilder(url)
                .GET()
                .build();
    }

    private HttpRequest.Builder requestBuilder(String url) {
        return HttpRequest.newBuilder(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json");
    }

    private String send(HttpRequest request) {
        try {
            HttpRequest authenticatedRequest = withAuth(request);
            HttpResponse<String> response = httpClient.send(authenticatedRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < OK_MIN || response.statusCode() > OK_MAX) {
                throw new IllegalStateException("Mailpit API returned HTTP " + response.statusCode());
            }

            return response.body();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to call Mailpit API", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Mailpit API call was interrupted", ex);
        }
    }

    private HttpRequest withAuth(HttpRequest request) {
        String username = appProperties.getDemoEmailSandbox().getMailpitUsername();

        if (username == null || username.isBlank()) {
            return request;
        }

        HttpRequest.Builder builder = HttpRequest.newBuilder(request.uri())
                .method(request.method(), request.bodyPublisher().orElse(HttpRequest.BodyPublishers.noBody()));

        request.headers().map().forEach((name, values) -> values.forEach(value -> builder.header(name, value)));

        String password = appProperties.getDemoEmailSandbox().getMailpitPassword();
        String rawCredentials = username + ":" + (password == null ? "" : password);
        String credentials = Base64.getEncoder().encodeToString(rawCredentials.getBytes(StandardCharsets.UTF_8));
        builder.header("Authorization", "Basic " + credentials);

        return builder.build();
    }

    private JsonNode readJson(HttpRequest request) {
        try {
            return objectMapper.readTree(send(request));
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to parse Mailpit API response", ex);
        }
    }

    private MailpitMessageSummary toSummary(JsonNode node) {
        return new MailpitMessageSummary(
                text(node, "ID", "id"),
                addressText(first(node, "From", "from")),
                parseAddresses(firstArray(node, "To", "to")),
                text(node, "Subject", "subject"),
                text(node, "Snippet", "snippet"),
                instant(node, "Created", "created")
        );
    }

    private MailpitMessageDetail toDetail(JsonNode node) {
        return new MailpitMessageDetail(
                text(node, "ID", "id"),
                addressText(first(node, "From", "from")),
                parseAddresses(firstArray(node, "To", "to")),
                text(node, "Subject", "subject"),
                text(node, "Snippet", "snippet"),
                instant(node, "Created", "created"),
                text(node, "HTML", "Html", "html"),
                text(node, "Text", "text")
        );
    }

    private List<String> parseAddresses(JsonNode addresses) {
        List<String> result = new ArrayList<>();

        if (addresses == null || !addresses.isArray()) {
            return result;
        }

        for (JsonNode address : addresses) {
            String value = addressText(address);

            if (value != null && !value.isBlank()) {
                result.add(value);
            }
        }

        return result;
    }

    private String addressText(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }

        if (node.isTextual()) {
            return node.asText();
        }

        return text(node, "Address", "address", "Email", "email");
    }

    private Map<String, List<String>> parseHeaders(JsonNode root) {
        JsonNode headers = first(root, "Headers", "headers");

        if (headers == null || headers.isMissingNode() || headers.isNull()) {
            headers = root;
        }

        Map<String, List<String>> result = new HashMap<>();

        if (headers.isArray()) {
            for (JsonNode header : headers) {
                String key = text(header, "Key", "key", "Name", "name");
                JsonNode value = first(header, "Value", "value", "Values", "values");
                addHeader(result, key, value);
            }
        } else if (headers.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = headers.fields();

            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                addHeader(result, field.getKey(), field.getValue());
            }
        }

        return result;
    }

    private void addHeader(Map<String, List<String>> result, String key, JsonNode value) {
        if (key == null || key.isBlank() || value == null || value.isMissingNode() || value.isNull()) {
            return;
        }

        List<String> values = result.computeIfAbsent(key, ignored -> new ArrayList<>());

        if (value.isArray()) {
            for (JsonNode item : value) {
                if (!item.isNull()) {
                    values.add(item.asText());
                }
            }
        } else {
            values.add(value.asText());
        }
    }

    private JsonNode first(JsonNode node, String... fieldNames) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }

        for (String fieldName : fieldNames) {
            JsonNode child = node.get(fieldName);

            if (child != null && !child.isMissingNode() && !child.isNull()) {
                return child;
            }
        }

        return null;
    }

    private JsonNode firstArray(JsonNode node, String... fieldNames) {
        JsonNode child = first(node, fieldNames);
        return child != null && child.isArray() ? child : null;
    }

    private String text(JsonNode node, String... fieldNames) {
        JsonNode child = first(node, fieldNames);
        return child == null || child.isNull() ? null : child.asText();
    }

    private Instant instant(JsonNode node, String... fieldNames) {
        String value = text(node, fieldNames);

        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Instant.parse(value);
        } catch (DateTimeParseException ex) {
            try {
                return OffsetDateTime.parse(value).toInstant();
            } catch (DateTimeParseException ignored) {
                return null;
            }
        }
    }

    private String apiUrl(String path) {
        String baseUrl = appProperties.getDemoEmailSandbox().getMailpitApiBaseUrl();

        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        return baseUrl + path;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
