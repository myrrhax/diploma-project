package com.github.myrrhax.diploma_project.security;

import jakarta.servlet.http.HttpServletResponse;
import lombok.Setter;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.AuthenticationFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.CookieClearingLogoutHandler;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;

@Setter
public class JwtSecurityConfigurer extends AbstractHttpConfigurer<JwtSecurityConfigurer, HttpSecurity> {
    private TokenAuthenticationDetailsService tokenAuthenticationDetailsService;
    private String refreshCookieName = "Refresh-Token";

    @Override
    public void init(HttpSecurity builder) throws Exception {
        builder.logout(logout -> logout.addLogoutHandler(
                new CookieClearingLogoutHandler(refreshCookieName)
        ));
    }

    @Override
    public void configure(HttpSecurity builder) throws Exception {
        AuthenticationManager authenticationManager = builder.getSharedObject(AuthenticationManager.class);

        var jwtFilter = new AuthenticationFilter(authenticationManager, new TokenAuthenticationConverter());
        jwtFilter.setSuccessHandler((req, resp, auth) -> {});
        jwtFilter.setFailureHandler((req, resp, e) ->
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED));

        var provider = new PreAuthenticatedAuthenticationProvider();
        provider.setPreAuthenticatedUserDetailsService(tokenAuthenticationDetailsService);

        builder.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .authenticationProvider(provider)
                .sessionManagement(policy ->
                        policy.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    }
}
