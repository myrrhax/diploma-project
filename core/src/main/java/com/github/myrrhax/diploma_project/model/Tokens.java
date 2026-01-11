package com.github.myrrhax.diploma_project.model;

import com.github.myrrhax.diploma_project.security.Token;

public record Tokens(
        Token accessToken,
        String signedAccessToken,
        Token refreshToken,
        String signedRefreshToken
) { }