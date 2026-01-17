package com.github.myrrhax.diploma_project.event.listener;

import com.github.myrrhax.diploma_project.event.SendMailEvent;
import com.github.myrrhax.diploma_project.model.dto.SendMailDto;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SendMailEventListener implements ApplicationListener<SendMailEvent<?>> {
    private final RabbitTemplate rabbitTemplate;

    @Override
    public void onApplicationEvent(SendMailEvent<?> event) {
        SendMailDto dto = new SendMailDto(event.getReceiverEmail(), event.getMailType(), event.getPayload());
        rabbitTemplate.convertAndSend(dto);
    }
}
