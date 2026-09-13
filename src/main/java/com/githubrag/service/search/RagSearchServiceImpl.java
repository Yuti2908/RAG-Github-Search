package com.githubrag.service.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.githubrag.config.OpenAiProperties;
import com.githubrag.exception.ExternalApiException;
import com.githubrag.model.dto.ChunkResult;
import com.githubrag.model.dto.SearchRequest;
import com.githubrag.model.dto.SearchResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class RagSearchServiceImpl implements RagSearchService {

    private static final String SYSTEM_PROMPT = """
            You are a helpful assistant answering questions about the user's own \
            GitHub repositories, based only on the provided context chunks. \
            For every claim you make, cite which repo and file it came from, \
            in the form (repo: <name>, file: <path>). If the context doesn't \
            contain enough information to answer, say so plainly instead of \
            guessing.
            """;

    private final VectorSearchService vectorSearchService;
    private final WebClient openAiWebClient;
    private final OpenAiProperties openAiProperties;

    public RagSearchServiceImpl(
            VectorSearchService vectorSearchService,
            WebClient openAiWebClient,
            OpenAiProperties openAiProperties) {
        this.vectorSearchService = vectorSearchService;
        this.openAiWebClient = openAiWebClient;
        this.openAiProperties = openAiProperties;
    }

    @Override
    public SearchResponse search(SearchRequest request) {
        long startTime = System.currentTimeMillis();

        List<ChunkResult> chunks = vectorSearchService.search(
                request.getQuery(), request.getTopK(), request.getRepoName(), request.getChunkType());

        String synthesizedAnswer = null;
        if (request.isGenerateAnswer()) {
            synthesizedAnswer = synthesizeAnswer(request.getQuery(), chunks);
        }

        long retrievalTime = System.currentTimeMillis() - startTime;

        return SearchResponse.builder()
                .query(request.getQuery())
                .relevantChunks(chunks)
                .synthesizedAnswer(synthesizedAnswer)
                .retrievalTimeMs(retrievalTime)
                .build();
    }

    private String synthesizeAnswer(String query, List<ChunkResult> chunks) {
        if (chunks.isEmpty()) {
            return "No relevant context found in your ingested repositories to answer this question.";
        }
        if (!openAiProperties.hasApiKey()) {
            return "[Mock answer - no OpenAI key configured] Based on "
                    + chunks.size() + " retrieved chunks, the top match is from "
                    + chunks.get(0).getRepoName() + "/" + chunks.get(0).getFilePath() + ".";
        }

        String context = buildContextBlock(chunks);
        String userPrompt = "Context:\n" + context + "\n\nQuestion: " + query;

        try {
            JsonNode response = openAiWebClient.post()
                    .uri("/chat/completions")
                    .bodyValue(Map.of(
                            "model", openAiProperties.getChatModel(),
                            "messages", List.of(
                                    Map.of("role", "system", "content", SYSTEM_PROMPT),
                                    Map.of("role", "user", "content", userPrompt)
                            )
                    ))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (response != null && response.has("choices") && response.get("choices").size() > 0) {
                return response.get("choices").get(0).path("message").path("content").asText();
            }
            return "No answer generated.";
        } catch (WebClientResponseException ex) {
            throw new ExternalApiException("OpenAI chat completion call failed: " + ex.getStatusCode(), ex);
        }
    }

    private String buildContextBlock(List<ChunkResult> chunks) {
        StringBuilder builder = new StringBuilder();
        for (ChunkResult chunk : chunks) {
            builder.append("(repo: ").append(chunk.getRepoName())
                    .append(", file: ").append(chunk.getFilePath())
                    .append(")\n")
                    .append(chunk.getContent())
                    .append("\n\n");
        }
        return builder.toString();
    }
}
