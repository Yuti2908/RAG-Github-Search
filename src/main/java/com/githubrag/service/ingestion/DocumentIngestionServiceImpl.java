package com.githubrag.service.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.githubrag.config.AppProperties;
import com.githubrag.model.dto.FetchedFile;
import com.githubrag.model.dto.IngestRequest;
import com.githubrag.model.dto.IngestResponse;
import com.githubrag.model.dto.TextChunk;
import com.githubrag.model.entity.DocumentChunk;
import com.githubrag.model.entity.RepoDocument;
import com.githubrag.repository.DocumentChunkRepository;
import com.githubrag.repository.RepoDocumentRepository;
import com.githubrag.service.embedding.EmbeddingService;
import com.githubrag.service.github.GitHubFetchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class DocumentIngestionServiceImpl implements DocumentIngestionService {

    private final GitHubFetchService gitHubFetchService;
    private final ChunkingService chunkingService;
    private final EmbeddingService embeddingService;
    private final RepoDocumentRepository repoDocumentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;

    public DocumentIngestionServiceImpl(
            GitHubFetchService gitHubFetchService,
            ChunkingService chunkingService,
            EmbeddingService embeddingService,
            RepoDocumentRepository repoDocumentRepository,
            DocumentChunkRepository documentChunkRepository,
            AppProperties appProperties,
            ObjectMapper objectMapper) {
        this.gitHubFetchService = gitHubFetchService;
        this.chunkingService = chunkingService;
        this.embeddingService = embeddingService;
        this.repoDocumentRepository = repoDocumentRepository;
        this.documentChunkRepository = documentChunkRepository;
        this.appProperties = appProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public IngestResponse ingest(IngestRequest request) {
        long startTime = System.currentTimeMillis();

        String username = (request.getGithubUsername() != null && !request.getGithubUsername().isBlank())
                ? request.getGithubUsername()
                : appProperties.getDefaultUsername();

        List<String> reposToIngest = (request.getRepoName() != null && !request.getRepoName().isBlank())
                ? List.of(request.getRepoName())
                : gitHubFetchService.listPublicRepos(username);

        int filesIngested = 0;
        int chunksCreated = 0;

        for (String repoName : reposToIngest) {
            log.info("Ingesting repo {}/{}", username, repoName);
            List<FetchedFile> files = gitHubFetchService.fetchRepoFiles(username, repoName);

            for (FetchedFile file : files) {
                IngestFileResult result = ingestFile(username, repoName, file, request.isForceRefresh());
                if (result.wasNew()) {
                    filesIngested++;
                    chunksCreated += result.chunkCount();
                }
            }
        }

        long timeTaken = System.currentTimeMillis() - startTime;

        return IngestResponse.builder()
                .reposIngested(reposToIngest.size())
                .filesIngested(filesIngested)
                .chunksCreated(chunksCreated)
                .timeTakenMs(timeTaken)
                .repoNames(reposToIngest)
                .build();
    }

    private IngestFileResult ingestFile(String owner, String repoName, FetchedFile file, boolean forceRefresh) {
        Optional<RepoDocument> existing = repoDocumentRepository
                .findByRepoOwnerAndRepoNameAndFilePath(owner, repoName, file.getFilePath());

        if (existing.isPresent() && !forceRefresh) {
            log.debug("Skipping already-ingested file {} (forceRefresh=false)", file.getFilePath());
            return new IngestFileResult(false, 0);
        }

        RepoDocument repoDocument = existing.orElseGet(RepoDocument::new);
        repoDocument.setRepoOwner(owner);
        repoDocument.setRepoName(repoName);
        repoDocument.setFilePath(file.getFilePath());
        repoDocument.setFileType(file.getFileType());
        repoDocument.setRawContent(file.getRawContent());
        repoDocument.setLastIngestedAt(Instant.now());

        // Clear old chunks before re-adding, relevant when forceRefresh=true on an existing file
        repoDocument.getChunks().clear();

        RepoDocument savedDocument = repoDocumentRepository.save(repoDocument);

        List<TextChunk> textChunks = chunkingService.chunk(file);
        List<String> contents = textChunks.stream().map(TextChunk::getContent).toList();
        List<List<Float>> embeddings = embeddingService.embedBatch(contents);

        List<DocumentChunk> chunkEntities = new ArrayList<>();
        for (int i = 0; i < textChunks.size(); i++) {
            TextChunk textChunk = textChunks.get(i);
            String embeddingJson = serializeEmbedding(embeddings.get(i));

            chunkEntities.add(DocumentChunk.builder()
                    .repoDocument(savedDocument)
                    .chunkIndex(textChunk.getChunkIndex())
                    .chunkType(textChunk.getChunkType())
                    .content(textChunk.getContent())
                    .embedding(embeddingJson)
                    .build());
        }
        documentChunkRepository.saveAll(chunkEntities);

        return new IngestFileResult(true, chunkEntities.size());
    }

    private String serializeEmbedding(List<Float> embedding) {
        try {
            return objectMapper.writeValueAsString(embedding);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize embedding", e);
        }
    }

    /** Result of ingesting a single file: whether it was newly (re)ingested, and how many chunks it produced. */
    private record IngestFileResult(boolean wasNew, int chunkCount) {
    }
}
