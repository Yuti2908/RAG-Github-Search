package com.githubrag.model.dto;

import jakarta.validation.constraints.NotBlank;
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
public class SearchRequest {

    @NotBlank(message = "query must not be blank")
    private String query;

    @Builder.Default
    private int topK = 5;

    /** Optional filter, e.g. restrict search to a single repo. */
    private String repoName;

    /** Optional filter, e.g. restrict search to PROSE or CODE chunks. */
    private String chunkType;

    @Builder.Default
    private boolean generateAnswer = false;
}
