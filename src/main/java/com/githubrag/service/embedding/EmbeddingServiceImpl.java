package com.githubrag.service.embedding;

import com.fasterxml.jackson.databind.JsonNode;
import com.githubrag.config.OpenAiProperties;
import com.githubrag.exception.ExternalApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Generates text embeddings via OpenAI's embeddings endpoint.
 * <p>
 * Falls back to deterministic mock embeddings when no API key is configured
 * (app.openai.api-key unset), so the app is fully runnable/testable without
 * incurring API costs. Mock vectors are seeded from a hash of the input text,
 * so the same text always produces the same mock embedding - similarity
 * comparisons stay stable across runs even though they're not semantically
 * meaningful.
 */
@Slf4j
@Service
public class EmbeddingServiceImpl implements EmbeddingService {

    private final WebClient openAiWebClient;
    private final OpenAiProperties properties;

    public EmbeddingServiceImpl(WebClient openAiWebClient, OpenAiProperties properties) {
        this.openAiWebClient = openAiWebClient;
        this.properties = properties;
    }

    @Override
    public List<Float> embed(String text) {
        if (!properties.hasApiKey()) {
            return mockEmbedding(text);
        }
        return callOpenAiEmbedding(List.of(text)).get(0);
    }

    @Override
    public List<List<Float>> embedBatch(List<String> texts) {
        if (texts.isEmpty()) {
            return List.of();
        }
        if (!properties.hasApiKey()) {
            List<List<Float>> results = new ArrayList<>();
            for (String text : texts) {
                results.add(mockEmbedding(text));
            }
            return results;
        }
        return callOpenAiEmbedding(texts);
    }

    private List<List<Float>> callOpenAiEmbedding(List<String> texts) {
        try {
            JsonNode response = openAiWebClient.post()
                    .uri("/embeddings")
                    .bodyValue(Map.of(
                            "model", properties.getEmbeddingModel(),
                            "input", texts
                    ))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            List<List<Float>> embeddings = new ArrayList<>();
            if (response != null && response.has("data")) {
                for (JsonNode item : response.get("data")) {
                    List<Float> vector = new ArrayList<>();
                    for (JsonNode value : item.get("embedding")) {
                        vector.add((float) value.asDouble());
                    }
                    embeddings.add(vector);
                }
            }
            return embeddings;
        } catch (WebClientResponseException ex) {
            throw new ExternalApiException("OpenAI embeddings call failed: " + ex.getStatusCode(), ex);
        }
    }

    private List<Float> mockEmbedding(String text) {
        long seed = hashToLong(text);
        Random random = new Random(seed);
        int dimension = properties.getEmbeddingDimension() > 0 ? properties.getEmbeddingDimension() : 1536;

        List<Float> vector = new ArrayList<>(dimension);
        for (int i = 0; i < dimension; i++) {
            vector.add((float) (random.nextGaussian() * 0.1));
        }
        return vector;
    }

    private long hashToLong(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            long result = 0;
            for (int i = 0; i < 8; i++) {
                result = (result << 8) | (hash[i] & 0xff);
            }
            return result;
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is always available on the JVM; this is unreachable in practice
            return text.hashCode();
        }
    }
}
