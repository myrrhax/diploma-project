package com.github.myrrhax.notification_service.listener;

import com.github.myrrhax.notification_service.strategy.MailTemplateStrategy;
import com.github.myrrhax.shared.model.MailType;
import com.github.myrrhax.shared.model.SendMailDto;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitEmailEventListener {
    private final JavaMailSender mailSender;
    private final List<MailTemplateStrategy> mailTemplateStrategies;
    private Map<MailType, MailTemplateStrategy> strategies;

    @Async("emailTaskExecutor")
    @RabbitListener(queues = {"${app.rabbitmq.send-mail-queue}"})
    public void processEmail(@Payload SendMailDto dto) {
        log.info("Processing new email update from queue: {}", dto);
        MailTemplateStrategy messageStrategy = strategies.get(MailType.SCHEME_INVITATION);
        String message = messageStrategy.buildMessage(dto.to(), dto.payload());
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setFrom("");
    }

    @PostConstruct
    public void init() {
        strategies = mailTemplateStrategies.stream()
                .collect(Collectors.toMap(MailTemplateStrategy::getSupportedType,
                        Function.identity()));
    }
}
