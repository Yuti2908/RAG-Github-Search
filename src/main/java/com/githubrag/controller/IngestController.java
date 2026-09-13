package com.githubrag.controller;

import com.githubrag.model.dto.IngestRequest;
import com.githubrag.model.dto.IngestResponse;
import com.githubrag.service.ingestion.DocumentIngestionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/repos")
public class IngestController {

    private final DocumentIngestionService documentIngestionService;

    public IngestController(DocumentIngestionService documentIngestionService) {
        this.documentIngestionService = documentIngestionService;
    }

    @PostMapping("/ingest")
    public IngestResponse ingest(@RequestBody(required = false) IngestRequest request) {
        IngestRequest effectiveRequest = (request != null) ? request : IngestRequest.builder().build();
        log.info("Ingest request received: username={}, repoName={}, forceRefresh={}",
                effectiveRequest.getGithubUsername(), effectiveRequest.getRepoName(), effectiveRequest.isForceRefresh());
        return documentIngestionService.ingest(effectiveRequest);
    }
}
