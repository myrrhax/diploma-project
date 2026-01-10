package com.github.myrrhax.diploma_project.security;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {
    String secret;
    String issuer;
    Duration accessTokenTtl;
    Duration refreshTokenTtl;
}
