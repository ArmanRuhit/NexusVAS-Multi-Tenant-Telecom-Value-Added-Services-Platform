package dev.armanruhit.nexusvas.notification.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Queues consumed by notification-service
    public static final String NOTIFICATION_QUEUE     = "notification.dispatch";
    public static final String NOTIFICATION_DLQ       = "notification.dispatch.dlq";
    public static final String NOTIFICATION_EXCHANGE  = "notification.exchange";
    public static final String NOTIFICATION_DLX       = "notification.dlx";

    // Campaign dispatch queue (produced by campaign-service, consumed here)
    public static final String CAMPAIGN_DISPATCH_QUEUE = "campaign.dispatch";
    public static final String CAMPAIGN_EXCHANGE       = "campaign.exchange";
    public static final String CAMPAIGN_DISPATCH_RK    = "campaign.dispatch";

    @Bean
    public DirectExchange notificationExchange() {
        return new DirectExchange(NOTIFICATION_EXCHANGE);
    }

    @Bean
    public DirectExchange notificationDeadLetterExchange() {
        return new DirectExchange(NOTIFICATION_DLX);
    }

    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(NOTIFICATION_QUEUE)
            .withArgument("x-dead-letter-exchange", NOTIFICATION_DLX)
            .withArgument("x-dead-letter-routing-key", NOTIFICATION_DLQ)
            .build();
    }

    @Bean
    public Queue notificationDlq() {
        return QueueBuilder.durable(NOTIFICATION_DLQ).build();
    }

    @Bean
    public Binding notificationBinding(Queue notificationQueue, DirectExchange notificationExchange) {
        return BindingBuilder.bind(notificationQueue).to(notificationExchange).with(NOTIFICATION_QUEUE);
    }

    @Bean
    public Binding notificationDlqBinding(Queue notificationDlq, DirectExchange notificationDeadLetterExchange) {
        return BindingBuilder.bind(notificationDlq).to(notificationDeadLetterExchange).with(NOTIFICATION_DLQ);
    }

    // Campaign dispatch queue — declared here so notification-service can consume it
    @Bean
    public DirectExchange campaignExchange() {
        return new DirectExchange(CAMPAIGN_EXCHANGE);
    }

    @Bean
    public Queue campaignDispatchQueue() {
        return QueueBuilder.durable(CAMPAIGN_DISPATCH_QUEUE)
            .withArgument("x-dead-letter-exchange", NOTIFICATION_DLX)
            .withArgument("x-dead-letter-routing-key", NOTIFICATION_DLQ)
            .build();
    }

    @Bean
    public Binding campaignDispatchBinding(Queue campaignDispatchQueue, DirectExchange campaignExchange) {
        return BindingBuilder.bind(campaignDispatchQueue).to(campaignExchange).with(CAMPAIGN_DISPATCH_RK);
    }

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         Jackson2JsonMessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        return template;
    }
}
