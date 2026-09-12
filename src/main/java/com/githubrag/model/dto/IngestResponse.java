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
public class IngestResponse {

    private int reposIngested;
    private int filesIngested;
    private int chunksCreated;
    private long timeTakenMs;
    private List<String> repoNames;
}
