package com.github.myrrhax.diploma_project.security.jwt;

import com.github.myrrhax.diploma_project.security.jwt.converter.TokenAuthenticationConverter;
import com.github.myrrhax.diploma_project.service.TokenAuthenticationDetailsService;
import lombok.Setter;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.access.ExceptionTranslationFilter;
import org.springframework.security.web.authentication.AuthenticationFilter;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;

@Setter
public class JwtSecurityConfigurer extends AbstractHttpConfigurer<JwtSecurityConfigurer, HttpSecurity> {
    private JwsTokenProvider jwsTokenProvider;

    @Override
    public void configure(HttpSecurity builder) throws Exception {
        var jwtFilter = new AuthenticationFilter(builder.getSharedObject(AuthenticationManager.class),
                new TokenAuthenticationConverter(jwsTokenProvider));

        var provider = new PreAuthenticatedAuthenticationProvider();
        provider.setPreAuthenticatedUserDetailsService(new TokenAuthenticationDetailsService());

        builder.addFilterAfter(jwtFilter, ExceptionTranslationFilter.class)
                .authenticationProvider(provider);
    }
}
