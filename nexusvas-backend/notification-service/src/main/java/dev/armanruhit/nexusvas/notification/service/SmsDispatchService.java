package dev.armanruhit.nexusvas.notification.service;

import dev.armanruhit.nexusvas.notification.domain.entity.NotificationLog;
import dev.armanruhit.nexusvas.notification.domain.entity.NotificationLog.NotificationStatus;
import dev.armanruhit.nexusvas.notification.domain.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * SMS dispatch service with exponential backoff retry.
 * Calls configurable SMS gateway (Twilio, Vonage, local operator gateway).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SmsDispatchService {

    private static final int MAX_RETRIES = 3;
    private static final long[] BACKOFF_DELAYS_MS = {1000, 2000, 4000, 8000}; // 1s, 2s, 4s, 8s

    private final NotificationLogRepository repository;
    private final WebClient.Builder webClientBuilder;

    @Value("${sms.gateway.url:}")
    private String gatewayUrl;

    @Value("${sms.gateway.api-key:}")
    private String gatewayApiKey;

    @Value("${sms.gateway.sender-id:NexusVAS}")
    private String senderId;

    @Transactional
    public void dispatch(String tenantId, String msisdn, String content,
                         UUID correlationId, String provider) {
        NotificationLog logEntry = NotificationLog.builder()
            .tenantId(tenantId)
            .correlationId(correlationId)
            .type(NotificationLog.NotificationType.SMS)
            .msisdn(msisdn)
            .content(content)
            .provider(provider)
            .build();

        NotificationLog saved = repository.save(logEntry);
        doSendWithRetry(saved, msisdn, content, 0);
    }

    /**
     * Async dispatch for batch operations — returns immediately, retries in background.
     */
    @Async
    public void dispatchAsync(String tenantId, String msisdn, String content,
                              UUID correlationId, String provider) {
        dispatch(tenantId, msisdn, content, correlationId, provider);
    }

    private void doSendWithRetry(NotificationLog logEntry, String msisdn, String content, int attempt) {
        try {
            String messageId = sendViaSmsGateway(msisdn, content);
            logEntry.setStatus(NotificationStatus.SENT);
            logEntry.setSentAt(Instant.now());
            logEntry.setProviderMessageId(messageId);
            log.info("SMS sent to {} correlationId={} attempt={}", msisdn, logEntry.getCorrelationId(), attempt + 1);
        } catch (Exception e) {
            int retryCount = logEntry.getRetryCount() + 1;
            logEntry.setRetryCount(retryCount);
            
            if (retryCount >= MAX_RETRIES) {
                logEntry.setStatus(NotificationStatus.FAILED);
                logEntry.setErrorMessage(e.getMessage());
                log.error("SMS permanently failed for {} after {} retries: {}", msisdn, retryCount, e.getMessage());
            } else {
                logEntry.setStatus(NotificationStatus.RETRYING);
                logEntry.setErrorMessage(e.getMessage());
                log.warn("SMS dispatch attempt {} failed for {}: {}. Scheduling retry...", 
                    retryCount, msisdn, e.getMessage());
                
                // Schedule retry with exponential backoff
                scheduleRetry(logEntry, msisdn, content, retryCount);
            }
        }
        repository.save(logEntry);
    }

    private void scheduleRetry(NotificationLog logEntry, String msisdn, String content, int retryCount) {
        long delayMs = BACKOFF_DELAYS_MS[Math.min(retryCount, BACKOFF_DELAYS_MS.length - 1)];
        
        try {
            TimeUnit.MILLISECONDS.sleep(delayMs);
            doSendWithRetry(logEntry, msisdn, content, retryCount);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            log.error("Retry interrupted for msisdn={}", msisdn);
            logEntry.setStatus(NotificationStatus.FAILED);
            logEntry.setErrorMessage("Retry interrupted");
            repository.save(logEntry);
        }
    }

    /**
     * Sends SMS via configured gateway.
     * Stub implementation — replace with actual gateway SDK (Twilio, Vonage, etc.)
     */
    private String sendViaSmsGateway(String msisdn, String content) {
        // Real implementation would:
        // 1. Build gateway-specific request payload
        // 2. Call gateway REST API via WebClient
        // 3. Parse response and return message ID
        //
        // Example for Twilio:
        // return webClientBuilder.build()
        //     .post()
        //     .uri(gatewayUrl + "/Messages.json")
        //     .headers(h -> h.setBasicAuth(accountSid, authToken))
        //     .body(BodyInserters.fromFormData("To", msisdn)
        //         .with("From", senderId)
        //         .with("Body", content))
        //     .retrieve()
        //     .bodyToMono(TwilioResponse.class)
        //     .map(TwilioResponse::sid)
        //     .block();
        
        // Stub: simulate success with random message ID
        if (msisdn != null && msisdn.startsWith("+880")) {
            return "MSG-" + UUID.randomUUID().toString().substring(0, 8);
        }
        throw new RuntimeException("Invalid MSISDN format");
    }
}
