package com.github.myrrhax.diploma_project.config;

import com.github.myrrhax.diploma_project.config.props.AppCacheProperties;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration(proxyBeanMethods = false)
@EnableCaching
@EnableConfigurationProperties({CacheProperties.class, AppCacheProperties.class})
@RequiredArgsConstructor
public class CacheConfiguration {
    private final AppCacheProperties cacheProperties;

    @Bean
    public CacheManager redisCacheManager(JedisConnectionFactory jedisConnectionFactory) {
        var defaultConfig = RedisCacheConfiguration.defaultCacheConfig();
        Map<String, RedisCacheConfiguration> config = new HashMap<>();
        cacheProperties.getCaches().forEach((key,prop) -> {
            var cacheConfig = RedisCacheConfiguration.defaultCacheConfig();
            var ttl = prop.getTtl();
            if (ttl != null && ttl != Duration.ZERO) {
                cacheConfig = cacheConfig.entryTtl(ttl);
            }
            config.put(key, cacheConfig);
        });

        return RedisCacheManager.builder(jedisConnectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(config)
                .build();
    }
}
