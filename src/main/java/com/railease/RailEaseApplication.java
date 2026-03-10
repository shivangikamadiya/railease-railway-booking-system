package com.railease;  // Must be com.railease, not com.railasee

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
public class RailEaseApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(RailEaseApplication.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(RailEaseApplication.class, args);
    }
}