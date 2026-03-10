package com.railease;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@SpringBootTest
public class DatabaseConnectionTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void testConnection() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            System.out.println("✅ Database connected: " + conn.getMetaData().getURL());

            // Check if tables exist
            Integer userCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
            System.out.println("✅ Users in database: " + userCount);

            Integer trainCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM trains", Integer.class);
            System.out.println("✅ Trains in database: " + trainCount);
        }
    }
}