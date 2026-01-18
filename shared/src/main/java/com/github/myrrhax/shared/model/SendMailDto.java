package com.github.myrrhax.shared.model;

import com.github.myrrhax.shared.payload.MailPayload;

public record SendMailDto(
        String to,
        MailType type,
        MailPayload payload
) { }
