package hdu.ljq.common;

import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class RateLimiter {
  private final Map<String, Deque<Long>> events = new HashMap<>();

  public synchronized void check(String key, int max, long window, boolean add) {
    long now = System.currentTimeMillis();
    events
        .values()
        .forEach(
            q -> {
              while (!q.isEmpty() && q.peekFirst() < now - 900000) q.removeFirst();
            });
    events.entrySet().removeIf(e -> e.getValue().isEmpty());
    Deque<Long> q = events.computeIfAbsent(key, k -> new ArrayDeque<>());
    while (!q.isEmpty() && q.peekFirst() < now - window) q.removeFirst();
    if (q.size() >= max)
      throw new ApiException(429, "RATE_LIMITED", "Too many attempts. Please try again later.");
    if (add) q.addLast(now);
  }

  public synchronized void clear(String key) {
    events.remove(key);
  }
}
