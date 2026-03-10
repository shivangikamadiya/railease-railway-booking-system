package com.railease.util;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class TicketIdGenerator {

    private static final AtomicInteger counter = new AtomicInteger(1000);
    private static final Random random = new Random();

    public String generateTicketId() {
        LocalDateTime now = LocalDateTime.now();
        String datePart = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomPart = String.format("%04d", random.nextInt(10000));
        String sequencePart = String.format("%04d", counter.getAndIncrement() % 10000);

        return "TK" + datePart + randomPart + sequencePart;
    }
}