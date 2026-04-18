package com.example.orderservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${app.rabbitmq.exchange}")
    private String exchange;

    @Value("${app.rabbitmq.queue}")
    private String queue;

    @Value("${app.rabbitmq.routing-key-prefix}")
    private String routingKeyPrefix;

    @Bean
    TopicExchange ordersExchange() {
        return new TopicExchange(exchange);
    }

    @Bean
    Queue ordersQueue() {
        return QueueBuilder.durable(queue).build();
    }

    @Bean
    Binding ordersBinding(Queue ordersQueue, TopicExchange ordersExchange) {
        return BindingBuilder.bind(ordersQueue).to(ordersExchange).with(routingKeyPrefix + ".*");
    }

    @Bean
    Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    @Bean
    RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                  Jackson2JsonMessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}
