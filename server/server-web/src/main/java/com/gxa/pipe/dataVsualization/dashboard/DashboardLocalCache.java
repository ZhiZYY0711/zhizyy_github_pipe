package com.gxa.pipe.dataVsualization.dashboard;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Component
public class DashboardLocalCache {

    private final Map<String, Entry<?>> entries = new ConcurrentHashMap<>();
    private final Clock clock;

    public DashboardLocalCache() {
        this(Clock.systemDefaultZone());
    }

    DashboardLocalCache(Clock clock) {
        this.clock = clock;
    }

    public <T> T get(String key, long ttlMillis, Supplier<T> supplier) {
        long now = clock.millis();
        Entry<?> existing = entries.get(key);
        if (existing != null && existing.expiresAt() > now) {
            @SuppressWarnings("unchecked")
            T value = (T) existing.value();
            return value;
        }

        T value = supplier.get();
        entries.put(key, new Entry<>(value, now + ttlMillis));
        return value;
    }

    public Optional<?> readRaw(String key) {
        Entry<?> entry = entries.get(key);
        return entry == null ? Optional.empty() : Optional.of(entry.value());
    }

    private record Entry<T>(T value, long expiresAt) {
    }
}
