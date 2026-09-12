package com.githubrag.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchResponse {

    private String query;
    private List<ChunkResult> relevantChunks;

    /** Populated only when generateAnswer=true on the request. */
    private String synthesizedAnswer;

    private long retrievalTimeMs;
}
