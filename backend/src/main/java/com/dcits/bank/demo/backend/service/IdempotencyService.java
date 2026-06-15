package com.dcits.bank.demo.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Redis 幂等服务
 * 10 分钟 TTL，过期的幂等号自动清理。
 */
@Service
public class IdempotencyService {

    private static final String PREFIX = "idem:";
    private static final Duration TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;

    public IdempotencyService(StringRedisTemplate redis, ObjectMapper mapper) {
        this.redis = redis;
        this.mapper = mapper;
    }

    /** 查询幂等结果，不存在返回 null */
    public <T> T check(String outTradeNo, Class<T> clazz) {
        try {
            String json = redis.opsForValue().get(PREFIX + outTradeNo);
            if (json == null) return null;
            return mapper.readValue(json, clazz);
        } catch (Exception e) {
            return null; // JSON 解析失败当缓存未命中
        }
    }

    /** 保存幂等结果，10 分钟过期 */
    public void save(String outTradeNo, Object response) {
        try {
            String json = mapper.writeValueAsString(response);
            redis.opsForValue().set(PREFIX + outTradeNo, json, TTL);
        } catch (Exception ignored) {
            // Redis 不可用时不影响业务
        }
    }
}
