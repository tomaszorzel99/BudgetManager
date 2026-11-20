package com.personalfinance.BudgetManager.Services;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Prosty limit nieudanych logowań trzymany w pamięci.
 * Klucz to np. "email:jan@x.pl" albo "ip:203.0.113.5".
 * Po przekroczeniu limitu klucz jest blokowany do końca 15-minutowego okna.
 * Uwaga: licznik znika po restarcie i nie jest współdzielony między instancjami.
 */
@Component
public class LoginAttemptService {

    private static final Duration WINDOW = Duration.ofMinutes(15);
    private static final int CLEANUP_THRESHOLD = 10_000;

    private record Attempts(int count, Instant windowStart) {
        boolean expired(Instant now) {
            return windowStart.plus(WINDOW).isBefore(now);
        }
    }

    private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();

    public boolean isBlocked(String key, int maxFailures) {
        Attempts current = attempts.get(key);
        if (current == null) {
            return false;
        }
        if (current.expired(Instant.now())) {
            attempts.remove(key, current);
            return false;
        }
        return current.count() >= maxFailures;
    }

    public void loginFailed(String key) {
        Instant now = Instant.now();
        attempts.compute(key, (k, old) ->
                old == null || old.expired(now)
                        ? new Attempts(1, now)
                        : new Attempts(old.count() + 1, old.windowStart()));
        if (attempts.size() > CLEANUP_THRESHOLD) {
            attempts.values().removeIf(a -> a.expired(now));
        }
    }

    public void loginSucceeded(String key) {
        attempts.remove(key);
    }
}
