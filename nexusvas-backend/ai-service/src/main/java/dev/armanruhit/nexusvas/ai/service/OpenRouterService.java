package dev.armanruhit.nexusvas.ai.service;

import dev.armanruhit.nexusvas.ai.domain.entity.PromptLog;
import dev.armanruhit.nexusvas.ai.domain.repository.PromptLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * OpenRouter API integration for accessing multiple LLM providers.
 * Supports models like Claude, GPT-4, Llama, Mistral through a unified API.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OpenRouterService {

    @Value("${openrouter.api-key:}")
    private String apiKey;

    @Value("${openrouter.base-url:https://openrouter.ai/api/v1}")
    private String baseUrl;

    @Value("${openrouter.default-model:anthropic/claude-3-haiku}")
    private String defaultModel;

    @Value("${openrouter.enabled:false}")
    private boolean enabled;

    private final PromptLogRepository promptLogRepository;
    private WebClient webClient;

    private WebClient getWebClient() {
        if (webClient == null) {
            webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader("HTTP-Referer", "https://nexusvas.armanruhit.dev")
                .defaultHeader("X-Title", "NexusVAS AI Service")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        }
        return webClient;
    }

    /**
     * Sends a chat completion request to OpenRouter.
     */
    public String chat(String tenantId, UUID userId, String systemPrompt, String userMessage) {
        if (!enabled || apiKey == null || apiKey.isBlank()) {
            log.warn("OpenRouter not configured, falling back to default response");
            return "OpenRouter integration not configured. Please set openrouter.api-key in configuration.";
        }

        long startMs = System.currentTimeMillis();

        PromptLog logEntry = PromptLog.builder()
            .tenantId(tenantId)
            .userId(userId)
            .promptType(PromptLog.PromptType.OPENROUTER_QUERY)
            .promptText(userMessage)
            .modelName(defaultModel)
            .build();

        try {
            Map<String, Object> requestBody = Map.of(
                "model", defaultModel,
                "messages", List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", userMessage)
                ),
                "max_tokens", 1024,
                "temperature", 0.7
            );

            String response = getWebClient().post()
                .uri("/chat/completions")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(OpenRouterResponse.class)
                .map(r -> r.choices().get(0).message().content())
                .block();

            logEntry.setResponseText(response);
            logEntry.setStatus(PromptLog.PromptStatus.SUCCESS);
            logEntry.setLatencyMs((int)(System.currentTimeMillis() - startMs));
            promptLogRepository.save(logEntry);

            log.info("OpenRouter chat completed for tenant={} model={} latency={}ms",
                tenantId, defaultModel, logEntry.getLatencyMs());
            return response;
        } catch (Exception e) {
            log.error("OpenRouter chat failed for tenant {}: {}", tenantId, e.getMessage());
            logEntry.setStatus(PromptLog.PromptStatus.FAILED);
            logEntry.setErrorMessage(e.getMessage());
            logEntry.setLatencyMs((int)(System.currentTimeMillis() - startMs));
            promptLogRepository.save(logEntry);
            throw new RuntimeException("OpenRouter request failed: " + e.getMessage(), e);
        }
    }

    /**
     * Lists available models from OpenRouter.
     */
    public Mono<List<ModelInfo>> listModels() {
        if (!enabled) {
            return Mono.just(List.of());
        }

        return getWebClient().get()
            .uri("/models")
            .retrieve()
            .bodyToMono(ModelsResponse.class)
            .map(ModelsResponse::data);
    }

    // Response DTOs
    public record OpenRouterResponse(List<Choice> choices, Usage usage) {}
    public record Choice(int index, Message message, String finish_reason) {}
    public record Message(String role, String content) {}
    public record Usage(int prompt_tokens, int completion_tokens, int total_tokens) {}

    public record ModelsResponse(List<ModelInfo> data) {}
    public record ModelInfo(String id, String name, String context_length, String pricing) {}
}
