package com.github.myrrhax.diploma_project.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class RabbitConfiguration {
    @Value("${app.rabbit.send-mail-queue}")
    private String mailQueue;
    @Value("${app.rabbit.notification-exchange}")
    private String exchangeName;
    @Value("${app.rabbit.send-mail-key}")
    private String sendMailRouterKey;

    @Bean
    public Exchange notificationExchange() {
        return new DirectExchange(exchangeName, true, false);
    }

    @Bean
    public Queue mailQueue() {
        return new Queue(mailQueue, true);
    }

    @Bean
    public Binding mailNotificationBinding() {
        return new Binding(mailQueue, Binding.DestinationType.QUEUE, exchangeName, sendMailRouterKey, null);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
