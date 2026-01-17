package com.github.myrrhax.shared.payload;

import com.github.myrrhax.diploma_project.model.enums.MailType;

public record SchemeInvitationMailPayload(
    String schemeName,
    String inviterEmail,
    String[] authorities,
    String url
) implements MailPayload {
    @Override
    public MailType getType() {
        return MailType.SCHEME_INVITATION;
    }
}
