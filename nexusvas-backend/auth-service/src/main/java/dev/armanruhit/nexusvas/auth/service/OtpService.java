package dev.armanruhit.nexusvas.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class OtpService {

    private final RedisTemplate<String, String> redisTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${auth.otp.ttl-minutes:5}")
    private int otpTtlMinutes;

    @Value("${auth.otp.max-attempts:3}")
    private int maxAttempts;

    private static final String OTP_PREFIX = "otp:";
    private static final String OTP_ATTEMPTS_PREFIX = "otp_attempts:";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public void sendOtp(String msisdn, String tenantId) {
        String otp = generateOtp();
        String redisKey = OTP_PREFIX + tenantId + ":" + msisdn;

        redisTemplate.opsForValue().set(redisKey, otp, otpTtlMinutes, TimeUnit.MINUTES);

        // Publish OTP send event to Kafka → Notification Service will dispatch SMS
        Map<String, String> otpEvent = Map.of(
            "msisdn", msisdn,
            "tenantId", tenantId,
            "otp", otp,
            "channel", "SMS"
        );
        kafkaTemplate.send("notification-events", tenantId + ":" + msisdn, otpEvent);

        log.info("OTP sent for msisdn {} on tenant {}", maskMsisdn(msisdn), tenantId);
    }

    public boolean verifyOtp(String msisdn, String tenantId, String submittedOtp) {
        String otpKey = OTP_PREFIX + tenantId + ":" + msisdn;
        String attemptsKey = OTP_ATTEMPTS_PREFIX + tenantId + ":" + msisdn;

        String storedOtp = redisTemplate.opsForValue().get(otpKey);
        if (storedOtp == null) {
            log.warn("OTP not found or expired for msisdn {} tenant {}", maskMsisdn(msisdn), tenantId);
            return false;
        }

        // Check attempt limit
        String attemptsStr = redisTemplate.opsForValue().get(attemptsKey);
        int attempts = attemptsStr != null ? Integer.parseInt(attemptsStr) : 0;
        if (attempts >= maxAttempts) {
            redisTemplate.delete(otpKey);
            log.warn("OTP max attempts exceeded for msisdn {} tenant {}", maskMsisdn(msisdn), tenantId);
            return false;
        }

        if (!storedOtp.equals(submittedOtp)) {
            redisTemplate.opsForValue().increment(attemptsKey);
            redisTemplate.expire(attemptsKey, otpTtlMinutes, TimeUnit.MINUTES);
            return false;
        }

        // OTP verified — clean up
        redisTemplate.delete(otpKey);
        redisTemplate.delete(attemptsKey);
        return true;
    }

    private String generateOtp() {
        return String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
    }

    private String maskMsisdn(String msisdn) {
        if (msisdn == null || msisdn.length() < 4) return "***";
        return msisdn.substring(0, msisdn.length() - 4) + "****";
    }
}
