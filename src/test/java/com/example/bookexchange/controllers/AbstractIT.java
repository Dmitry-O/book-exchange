package com.example.bookexchange.controllers;

import com.example.bookexchange.config.TestUserConfig;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;

@SpringBootTest
@Import(TestUserConfig.class)
@ActiveProfiles("localmysql")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractIT {

    static MySQLContainer<?> mySQLContainer = new MySQLContainer<>("mysql:9").withReuse(true);

    static {
        mySQLContainer.start();
    }

    @DynamicPropertySource
    static void mysqlProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mySQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", mySQLContainer::getUsername);
        registry.add("spring.datasource.password", mySQLContainer::getPassword);
    }
}
