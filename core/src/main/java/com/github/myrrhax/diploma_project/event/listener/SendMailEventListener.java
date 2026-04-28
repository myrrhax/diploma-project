package com.github.myrrhax.diploma_project.event.listener;

import com.github.myrrhax.diploma_project.event.SendMailEvent;
import com.github.myrrhax.shared.model.SendMailDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SendMailEventListener implements ApplicationListener<SendMailEvent<?>> {
    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbit.notification-exchange}")
    private String exchangeName;
    @Value("${app.rabbit.send-mail-key}")
    private String sendMailRouterKey;

    @Override
    public void onApplicationEvent(SendMailEvent<?> event) {
        SendMailDto dto = new SendMailDto(event.getReceiverEmail(), event.getMailType(), event.getPayload());
        log.info("Sending email to notification service for user {}", event.getReceiverEmail());
        rabbitTemplate.convertAndSend(exchangeName, sendMailRouterKey, dto);
    }
}
