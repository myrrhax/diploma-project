package com.github.myrrhax.diploma_project.config.props;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "app.cache")
public class AppCacheProperties {
    private Duration schemaTtl;
    private Map<String, CacheProperty> caches = new HashMap<>();

    @Data
    public static class CacheProperty {
        private Duration ttl = Duration.ZERO;
    }
}