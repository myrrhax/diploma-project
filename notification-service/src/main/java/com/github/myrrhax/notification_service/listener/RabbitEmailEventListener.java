package com.github.myrrhax.notification_service.listener;

import com.github.myrrhax.notification_service.strategy.MailTemplateStrategy;
import com.github.myrrhax.shared.model.MailType;
import com.github.myrrhax.shared.model.SendMailDto;
import jakarta.annotation.PostConstruct;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

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

    @Value("${spring.mail.username}")
    private String fromUser;

    @Async("emailTaskExecutor")
    @RabbitListener(queues = {"${app.rabbitmq.send-mail-queue}"})
    public void processEmail(@Payload SendMailDto dto) {
        log.info("Processing new email update from queue: {}", dto);
        MailTemplateStrategy messageStrategy = strategies.get(dto.type());
        String message = messageStrategy.buildMessage(dto.to(), dto.payload());
        MimeMessage mailMessage = mailSender.createMimeMessage();
        try {
            MimeMessageHelper messageHelper = new MimeMessageHelper(mailMessage, true, "UTF-8");
            messageHelper.setFrom(fromUser);
            messageHelper.setTo(dto.to());
            messageHelper.setSubject(messageStrategy.getSubject());
            messageHelper.setText(message, true);
            messageHelper.setReplyTo("no-reply@myrrermvdev.space");

            log.info("Sending new email...");
            mailSender.send(mailMessage);
            log.info("Email was sent successfully!");
        } catch (MessagingException e) {
            log.error("Unable to send message", e);
            throw new RuntimeException(e);
        }
    }

    @PostConstruct
    public void init() {
        strategies = mailTemplateStrategies.stream()
                .collect(Collectors.toMap(MailTemplateStrategy::getSupportedType,
                        Function.identity()));
    }
}
