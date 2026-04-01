package dev.armanruhit.nexusvas.campaign.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String CAMPAIGN_DISPATCH_QUEUE   = "campaign.dispatch";
    public static final String CAMPAIGN_DISPATCH_DLQ     = "campaign.dispatch.dlq";
    public static final String CAMPAIGN_EXCHANGE         = "campaign.exchange";
    public static final String CAMPAIGN_DLX              = "campaign.dlx";
    public static final String CAMPAIGN_DISPATCH_RK      = "campaign.dispatch";

    @Bean
    public DirectExchange campaignExchange() {
        return new DirectExchange(CAMPAIGN_EXCHANGE);
    }

    @Bean
    public DirectExchange campaignDeadLetterExchange() {
        return new DirectExchange(CAMPAIGN_DLX);
    }

    @Bean
    public Queue campaignDispatchQueue() {
        return QueueBuilder.durable(CAMPAIGN_DISPATCH_QUEUE)
            .withArgument("x-dead-letter-exchange", CAMPAIGN_DLX)
            .withArgument("x-dead-letter-routing-key", CAMPAIGN_DISPATCH_DLQ)
            .withArgument("x-message-ttl", 3_600_000) // 1 hour TTL
            .build();
    }

    @Bean
    public Queue campaignDispatchDlq() {
        return QueueBuilder.durable(CAMPAIGN_DISPATCH_DLQ).build();
    }

    @Bean
    public Binding campaignDispatchBinding(Queue campaignDispatchQueue, DirectExchange campaignExchange) {
        return BindingBuilder.bind(campaignDispatchQueue).to(campaignExchange).with(CAMPAIGN_DISPATCH_RK);
    }

    @Bean
    public Binding campaignDlqBinding(Queue campaignDispatchDlq, DirectExchange campaignDeadLetterExchange) {
        return BindingBuilder.bind(campaignDispatchDlq).to(campaignDeadLetterExchange).with(CAMPAIGN_DISPATCH_DLQ);
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
