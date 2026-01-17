package com.github.myrrhax.diploma_project.model.payload;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.github.myrrhax.diploma_project.model.enums.MailType;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = SchemeInvitationMailPayload.class, name = "INVITATION")
})
public interface MailPayload {
    MailType getType();
}
