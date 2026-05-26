package com.github.myrrhax.notification_service.listener;

import com.github.myrrhax.notification_service.service.MailSenderService;
import com.github.myrrhax.shared.model.SendMailDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitEmailEventListener {
    private final MailSenderService mailSenderService;

    @RabbitListener(queues = {"${app.rabbitmq.send-mail-queue}"})
    public void processEmail(@Payload SendMailDto dto) {
        log.info("Processing email notification with type: {}", dto.type());
        mailSenderService.sendMail(dto);
    }
}
