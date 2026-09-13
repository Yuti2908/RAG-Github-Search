package com.githubrag.service.embedding;

import java.util.List;

public interface EmbeddingService {

    /** Generates an embedding vector for a single piece of text. */
    List<Float> embed(String text);

    /** Batch variant - embeds multiple texts, preserving order. */
    List<List<Float>> embedBatch(List<String> texts);
}
