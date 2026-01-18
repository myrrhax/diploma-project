package com.github.myrrhax.notification_service.strategy;

import com.github.myrrhax.shared.model.MailType;
import com.github.myrrhax.shared.payload.MailPayload;

public interface MailTemplateStrategy {
    MailType getSupportedType();
    String buildMessage(String to, MailPayload payload);
    String getSubject();
}
