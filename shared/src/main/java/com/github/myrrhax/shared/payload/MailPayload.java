package com.github.myrrhax.shared.payload;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.github.myrrhax.shared.model.MailType;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = SchemeInvitationMailPayload.class, name = "INVITATION"),
        @JsonSubTypes.Type(value = ConfirmationCodeEmailPayload.class, name = "CONFIRMATION")
})
public interface MailPayload {
    MailType getType();
}
