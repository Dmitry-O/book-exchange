package com.example.bookexchange.common.demoaccess;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DemoAccessPaths {

    public static final String DEMO_ACCESS_PATH = "/demo/access";
    public static final String DEMO_ACCESS_VERIFY_PATH = DEMO_ACCESS_PATH + "/verify";
}
