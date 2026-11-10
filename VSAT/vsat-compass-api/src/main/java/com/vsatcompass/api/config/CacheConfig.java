package com.vsatcompass.api.config;

import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.LoggingCacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    public static final String CACHE_LEADERBOARD = "leaderboard";
    public static final String CACHE_BLUEPRINTS = "blueprints";
    public static final String CACHE_BLUEPRINT_DETAIL = "blueprintDetail";
    public static final String CACHE_PUBLIC_EXAMS = "publicExams";
    public static final String CACHE_PUBLIC_EXAM_DETAIL = "publicExamDetail";
    public static final String CACHE_PUBLIC_EXAM_BUNDLE = "publicExamBundle";

    @Bean
    @ConditionalOnClass(RedisCacheManagerBuilderCustomizer.class)
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {
        return builder -> {
            RedisCacheConfiguration baseConfig = RedisCacheConfiguration
                    .defaultCacheConfig(getClass().getClassLoader());
            RedisCacheConfiguration defaultTtl60s = baseConfig
                    .entryTtl(Duration.ofSeconds(60));
            RedisCacheConfiguration ttl1h = baseConfig
                    .entryTtl(Duration.ofHours(1));

            builder
                    .withCacheConfiguration(CACHE_LEADERBOARD, defaultTtl60s)
                    .withCacheConfiguration(CACHE_BLUEPRINTS, ttl1h)
                    .withCacheConfiguration(CACHE_BLUEPRINT_DETAIL, ttl1h)
                    .withCacheConfiguration(CACHE_PUBLIC_EXAMS, defaultTtl60s)
                    .withCacheConfiguration(CACHE_PUBLIC_EXAM_DETAIL, defaultTtl60s)
                    .withCacheConfiguration(CACHE_PUBLIC_EXAM_BUNDLE, defaultTtl60s);
        };
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new LoggingCacheErrorHandler();
    }
}
