package com.github.myrrhax.shared.payload;

import com.github.myrrhax.shared.model.MailType;

public record ConfirmationCodeEmailPayload(
        String code
) implements MailPayload {
    @Override
    public MailType getType() {
        return MailType.ACCOUNT_CONFIRMATION;
    }
}