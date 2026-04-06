package com.example.bookexchange.common.util;

import com.example.bookexchange.common.web.InvalidIfMatchHeaderException;
import org.springframework.stereotype.Component;

@Component
public class ParserUtil {

    public Long ifMatchParser(String ifMatch) {
        try {
            return Long.parseLong(ifMatch.replace("\"", ""));
        } catch (NumberFormatException ex) {
            throw new InvalidIfMatchHeaderException(ex);
        }
    }
}
