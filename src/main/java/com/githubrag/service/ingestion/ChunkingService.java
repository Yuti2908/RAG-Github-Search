package com.githubrag.service.ingestion;

import com.githubrag.model.dto.FetchedFile;
import com.githubrag.model.dto.TextChunk;

import java.util.List;

public interface ChunkingService {

    /**
     * Chunks a fetched file's raw content, dispatching to a prose or
     * code strategy based on the file's type.
     */
    List<TextChunk> chunk(FetchedFile file);
}
