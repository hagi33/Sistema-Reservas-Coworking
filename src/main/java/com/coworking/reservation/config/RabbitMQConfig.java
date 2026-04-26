package com.coworking.reservation.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "reservations.exchange";
    public static final String NOTIFICATION_QUEUE = "notification.queue";
    public static final String ROUTING_KEY = "reservation.*";

    @Bean
    public TopicExchange reservationsExchange(){

        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue notificationQueue(){
        return new Queue(NOTIFICATION_QUEUE, true);
    }

    @Bean
    public Binding notificationBinding (Queue notificationQueue, TopicExchange reservationExchange){
        return BindingBuilder
                .bind(notificationQueue)
                .to(reservationExchange)
                .with(ROUTING_KEY);

    }

    public JacksonJsonMessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }




}
