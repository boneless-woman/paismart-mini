package com.yizhaoqi.smartpai.service;

import com.yizhaoqi.smartpai.config.RateLimitProperties;
import com.yizhaoqi.smartpai.exception.RateLimitExceededException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.time.LocalDate;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.beans.factory.annotation.Value;

@Service
public class RateLimitService {

    private final StringRedisTemplate stringRedisTemplate;
    private final RateLimitProperties properties;
    private final RateLimitConfigService rateLimitConfigService;
    private final UsageQuotaService usageQuotaService;
    private final ConcurrentHashMap<String, MemoryWindow> memoryWindows = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> dailyTokenUsage = new ConcurrentHashMap<>();

    @Value("${portfolio.demo.enabled:false}")
    private boolean inMemory;

    @Value("${portfolio.demo.daily-requests:100}")
    private long demoDailyRequests;

    @Value("${portfolio.demo.daily-token-limit:100000}")
    private long demoDailyTokenLimit;

    @Value("${portfolio.demo.per-minute:6}")
    private long demoPerMinute;

    public RateLimitService(
            StringRedisTemplate stringRedisTemplate,
            RateLimitProperties properties,
            RateLimitConfigService rateLimitConfigService,
            UsageQuotaService usageQuotaService
    ) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.properties = properties;
        this.rateLimitConfigService = rateLimitConfigService;
        this.usageQuotaService = usageQuotaService;
    }

    public void checkRegisterByIp(String ip) {
        checkSingleWindow("register:ip:" + ip, properties.getRegister().getMax(), properties.getRegister().getWindowSeconds(), "注册请求过于频繁");
    }

    public void checkLoginByIp(String ip) {
        checkSingleWindow("login:ip:" + ip, properties.getLogin().getMax(), properties.getLogin().getWindowSeconds(), "登录请求过于频繁");
    }

    public void checkChatByUser(String userId) {
        checkChatByUser(userId, null);
    }

    public void checkChatByUser(String userId, String clientAddress) {
        if (inMemory) {
            String key = clientAddress == null || clientAddress.isBlank() ? userId : clientAddress;
            checkSingleWindow("chat:ip:" + key, demoPerMinute, 60, "聊天请求过于频繁");
            checkSingleWindow("chat:global:day:" + LocalDate.now(), demoDailyRequests, 86400,
                    "演示站今日问答额度已用完，请明天再试");
            return;
        }
        RateLimitConfigService.WindowLimitView limit = rateLimitConfigService.getCurrentSettings().chatMessage();
        checkSingleWindow("chat:user:" + userId, limit.max(), limit.windowSeconds(), "聊天请求过于频繁");
        usageQuotaService.recordChatRequest(userId);
    }

    public UsageQuotaService.TokenReservationBundle reserveLlmUsage(
            String userId,
            int estimatedPromptTokens,
            int maxCompletionTokens
    ) {
        if (inMemory) {
            reserveDemoTokens(Math.max(estimatedPromptTokens, 0) + Math.max(maxCompletionTokens, 0));
            return UsageQuotaService.TokenReservationBundle.noop("llm", userId);
        }
        RateLimitConfigService.TokenBudgetView limit = rateLimitConfigService.getCurrentSettings().llmGlobalToken();
        return usageQuotaService.reserveLlmTokensWithGlobalBudget(
                userId,
                estimatedPromptTokens,
                maxCompletionTokens,
                limit.minuteMax(),
                limit.minuteWindowSeconds(),
                limit.dayMax(),
                limit.dayWindowSeconds()
        );
    }

    public void checkEmbeddingQueryByUser(String userId) {
        RateLimitConfigService.DualWindowLimitView limit = rateLimitConfigService.getCurrentSettings().embeddingQueryRequest();
        checkSingleWindow("embedding:query:min:user:" + userId, limit.minuteMax(), limit.minuteWindowSeconds(), "Embedding查询过于频繁");
        checkSingleWindow("embedding:query:day:user:" + userId, limit.dayMax(), limit.dayWindowSeconds(), "Embedding查询当日次数已达上限");
    }

    public UsageQuotaService.TokenReservationBundle reserveEmbeddingUploadUsage(String userId, java.util.List<String> texts) {
        if (inMemory) {
            // The portfolio knowledge base is seeded once during startup and persisted in Neon.
            // It must not require Redis or consume the visitor-facing daily token budget.
            return UsageQuotaService.TokenReservationBundle.noop("embedding-upload", userId);
        }
        RateLimitConfigService.TokenBudgetView limit = rateLimitConfigService.getCurrentSettings().embeddingUploadToken();
        return usageQuotaService.reserveEmbeddingTokensWithGlobalBudget(
                userId,
                texts,
                "embedding-upload",
                "Embedding上传全网分钟Token预算已达上限",
                "Embedding上传全网当日Token预算已达上限",
                limit.minuteMax(),
                limit.minuteWindowSeconds(),
                limit.dayMax(),
                limit.dayWindowSeconds()
        );
    }

    public UsageQuotaService.TokenReservationBundle reserveEmbeddingQueryUsage(String userId, java.util.List<String> texts) {
        if (inMemory) {
            reserveDemoTokens(usageQuotaService.estimateEmbeddingTokens(texts));
            return UsageQuotaService.TokenReservationBundle.noop("embedding-query", userId);
        }
        checkEmbeddingQueryByUser(userId);
        RateLimitConfigService.TokenBudgetView limit = rateLimitConfigService.getCurrentSettings().embeddingQueryGlobalToken();
        return usageQuotaService.reserveEmbeddingTokensWithGlobalBudget(
                userId,
                texts,
                "embedding-query",
                "Embedding查询全网分钟Token预算已达上限",
                "Embedding查询全网当日Token预算已达上限",
                limit.minuteMax(),
                limit.minuteWindowSeconds(),
                limit.dayMax(),
                limit.dayWindowSeconds()
        );
    }

    private void checkSingleWindow(String key, long max, long windowSeconds, String message) {
        if (inMemory) {
            long now = System.currentTimeMillis();
            MemoryWindow window = memoryWindows.compute(key, (ignored, current) -> {
                if (current == null || current.expiresAt <= now) {
                    return new MemoryWindow(new AtomicLong(1), now + windowSeconds * 1000);
                }
                current.count.incrementAndGet();
                return current;
            });
            if (window.count.get() > max) {
                throw new RateLimitExceededException(message, Math.max(1, (window.expiresAt - now) / 1000));
            }
            return;
        }
        Long current = stringRedisTemplate.opsForValue().increment(key);
        if (current == null) {
            return;
        }

        if (current == 1) {
            stringRedisTemplate.expire(key, windowSeconds, TimeUnit.SECONDS);
        }

        if (current > max) {
            Long ttl = stringRedisTemplate.getExpire(key, TimeUnit.SECONDS);
            long retryAfterSeconds = ttl == null || ttl < 0 ? windowSeconds : ttl;
            throw new RateLimitExceededException(message, retryAfterSeconds);
        }
    }

    private void reserveDemoTokens(long tokens) {
        String day = LocalDate.now().toString();
        long total = dailyTokenUsage.computeIfAbsent(day, ignored -> new AtomicLong()).addAndGet(Math.max(tokens, 1));
        if (total > demoDailyTokenLimit) {
            throw new RateLimitExceededException("演示站今日模型额度已用完，请明天再试", 3600);
        }
    }

    private record MemoryWindow(AtomicLong count, long expiresAt) {}
}
