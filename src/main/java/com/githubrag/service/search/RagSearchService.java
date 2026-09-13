package com.githubrag.service.search;

import com.githubrag.model.dto.SearchRequest;
import com.githubrag.model.dto.SearchResponse;

public interface RagSearchService {

    /** Orchestrates vector retrieval and, optionally, LLM answer synthesis. */
    SearchResponse search(SearchRequest request);
}
