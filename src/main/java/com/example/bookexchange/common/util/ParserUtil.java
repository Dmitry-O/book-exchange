package com.example.bookexchange.common.util;

import org.springframework.stereotype.Component;

@Component
public class ParserUtil {

    public Long ifMatchParser(String ifMatch) {
        return Long.parseLong(ifMatch.replace("\"", ""));
    }
}
