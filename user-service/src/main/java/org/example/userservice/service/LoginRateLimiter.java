package org.example.userservice.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginRateLimiter {

    static final int MAX_FAILURES = 5;
    static final long WINDOW_MS = 60_000;

    private final ConcurrentHashMap<String, Deque<Long>> failures = new ConcurrentHashMap<>();

    public void checkNotBlocked(String email) {
        Deque<Long> timestamps = getEvicted(email);
        synchronized (timestamps) {
            if (timestamps.size() >= MAX_FAILURES) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        "Too many failed login attempts. Try again in a minute.");
            }
        }
    }

    public void recordFailure(String email) {
        Deque<Long> timestamps = getEvicted(email);
        synchronized (timestamps) {
            timestamps.addLast(System.currentTimeMillis());
        }
    }

    private Deque<Long> getEvicted(String email) {
        long now = System.currentTimeMillis();
        failures.putIfAbsent(email, new ArrayDeque<>());
        Deque<Long> timestamps = failures.get(email);
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && now - timestamps.peekFirst() > WINDOW_MS) {
                timestamps.pollFirst();
            }
        }
        return timestamps;
    }
}
