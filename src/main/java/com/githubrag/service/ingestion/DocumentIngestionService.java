package com.githubrag.service.ingestion;

import com.githubrag.model.dto.IngestRequest;
import com.githubrag.model.dto.IngestResponse;

public interface DocumentIngestionService {

    /** Orchestrates fetch -> chunk -> embed -> persist for the requested scope. */
    IngestResponse ingest(IngestRequest request);
}
