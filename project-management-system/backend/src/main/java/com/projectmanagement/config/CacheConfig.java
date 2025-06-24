package com.projectmanagement.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 缓存配置类
 * 优化用户搜索等高频查询的性能
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * 缓存管理器
     * 使用内存缓存，适合中小型应用
     * 如果需要分布式缓存，可以替换为Redis
     */
    @Bean
    public CacheManager cacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();

        // 定义缓存名称
        cacheManager.setCacheNames(java.util.Arrays.asList(
                "userSearch", // 用户搜索缓存
                "userById", // 用户ID查询缓存
                "userByUsername", // 用户名查询缓存
                "dashboardUsers" // 仪表板用户缓存
        ));

        // 设置允许空值缓存
        cacheManager.setAllowNullValues(true);

        return cacheManager;
    }
}