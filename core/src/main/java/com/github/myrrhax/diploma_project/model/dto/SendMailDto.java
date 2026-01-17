package com.github.myrrhax.diploma_project.model.dto;

import com.github.myrrhax.diploma_project.model.enums.MailType;
import com.github.myrrhax.diploma_project.model.payload.MailPayload;

public record SendMailDto(
        String to,
        MailType type,
        MailPayload payload
) { }