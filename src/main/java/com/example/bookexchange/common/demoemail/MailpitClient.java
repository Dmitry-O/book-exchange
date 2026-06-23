package com.example.bookexchange.common.demoemail;

import java.util.List;
import java.util.Map;

public interface MailpitClient {

    List<MailpitMessageSummary> listMessages(int limit);

    Map<String, List<String>> getHeaders(String messageId);

    MailpitMessageDetail getMessage(String messageId);

    void deleteMessages(List<String> messageIds);

    void deleteAllMessages(int batchSize, int maxBatches);
}
