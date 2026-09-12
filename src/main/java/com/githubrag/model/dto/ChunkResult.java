package com.githubrag.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChunkResult {

    private String repoName;
    private String filePath;
    private String content;
    private int chunkIndex;
    private double similarityScore;
}
