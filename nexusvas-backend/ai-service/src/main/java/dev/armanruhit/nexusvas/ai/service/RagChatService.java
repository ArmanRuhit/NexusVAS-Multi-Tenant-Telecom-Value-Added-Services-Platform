package dev.armanruhit.nexusvas.ai.service;

import dev.armanruhit.nexusvas.ai.domain.entity.PromptLog;
import dev.armanruhit.nexusvas.ai.domain.repository.PromptLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * RAG (Retrieval-Augmented Generation) chat service.
 * Retrieves relevant documents from pgvector and augments the prompt before sending to OpenAI.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RagChatService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final PromptLogRepository promptLogRepository;

    private static final String MODEL_NAME = "gpt-4o-mini";
    private static final int TOP_K = 5;

    public String chat(String tenantId, UUID userId, String userMessage) {
        long startMs = System.currentTimeMillis();

        // Retrieve relevant context from vector store
        List<org.springframework.ai.document.Document> relevantDocs =
            vectorStore.similaritySearch(SearchRequest.query(userMessage).withTopK(TOP_K));

        String context = relevantDocs.stream()
            .map(org.springframework.ai.document.Document::getContent)
            .reduce("", (a, b) -> a + "\n---\n" + b);

        String augmentedPrompt = context.isBlank() ? userMessage :
            "Use the following context to answer the question:\n\n" + context +
            "\n\nQuestion: " + userMessage;

        PromptLog logEntry = PromptLog.builder()
            .tenantId(tenantId)
            .userId(userId)
            .promptType(PromptLog.PromptType.RAG_QUERY)
            .promptText(userMessage)
            .modelName(MODEL_NAME)
            .build();

        try {
            String response = chatClient.prompt()
                .user(augmentedPrompt)
                .call()
                .content();

            logEntry.setResponseText(response);
            logEntry.setStatus(PromptLog.PromptStatus.SUCCESS);
            logEntry.setLatencyMs((int)(System.currentTimeMillis() - startMs));
            promptLogRepository.save(logEntry);
            return response;
        } catch (Exception e) {
            log.error("RAG chat error for tenant {}: {}", tenantId, e.getMessage());
            logEntry.setStatus(PromptLog.PromptStatus.FAILED);
            logEntry.setErrorMessage(e.getMessage());
            logEntry.setLatencyMs((int)(System.currentTimeMillis() - startMs));
            promptLogRepository.save(logEntry);
            throw new RuntimeException("AI chat failed: " + e.getMessage(), e);
        }
    }
}
