package com.example.bookexchange.report.api;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class ReportPaths {

    public static final String REPORT_PATH = "/report";
    public static final String REPORT_PATH_TARGET_ID = REPORT_PATH + "/{targetId}";
}
