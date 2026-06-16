package com.expensetracker.metadataservice.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {
    // RedisCacheManager auto-configured from spring.cache.redis.time-to-live in config-repo
}
