package dev.armanruhit.nexusvas.notification;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import dev.armanruhit.nexusvas.notification.messaging.CampaignBatchConsumer;
import dev.armanruhit.nexusvas.notification.service.SmsDispatchService;

@SpringBootApplication
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);

        SmsDispatchService smsDispatchService = new SmsDispatchService(null, null);
        RabbitTemplate rabbitTemplate = new RabbitTemplate();
        CampaignBatchConsumer campaignBatchConsumer = new CampaignBatchConsumer(smsDispatchService, rabbitTemplate);
    }
}
