package com.github.myrrhax.diploma_project.event;

import com.github.myrrhax.shared.model.MailType;
import com.github.myrrhax.shared.payload.MailPayload;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class SendMailEvent<T extends MailPayload> extends ApplicationEvent {
    private final String receiverEmail;
    private final MailType mailType;
    private final T payload;

    public SendMailEvent(Object source, String receiverEmail, MailType mailType, T payload) {
        super(source);
        this.receiverEmail = receiverEmail;
        this.mailType = mailType;
        this.payload = payload;
    }
}