package com.example.edu.sports_predict_live.global.ratelimit;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

// IP/키 기반 단순 fixed-window 레이트 리밋 (in-memory, 분산환경 미지원)
@Component
public class SimpleRateLimiter {

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    // 윈도우 내 호출 허용 여부
    public boolean tryAcquire(String key, int maxRequests, long windowMillis) {
        long now = System.currentTimeMillis();
        Window updated = windows.compute(key, (k, existing) -> {
            if (existing == null || now - existing.start >= windowMillis) {
                return new Window(now, 1);
            }
            return new Window(existing.start, existing.count + 1);
        });
        return updated.count <= maxRequests;
    }

    private record Window(long start, int count) {}
}
