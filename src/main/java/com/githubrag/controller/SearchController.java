package com.githubrag.controller;

import com.githubrag.model.dto.SearchRequest;
import com.githubrag.model.dto.SearchResponse;
import com.githubrag.service.search.RagSearchService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/search")
public class SearchController {

    private final RagSearchService ragSearchService;

    public SearchController(RagSearchService ragSearchService) {
        this.ragSearchService = ragSearchService;
    }

    @PostMapping
    public SearchResponse search(@Valid @RequestBody SearchRequest request) {
        log.info("Search request received: query={}, topK={}", request.getQuery(), request.getTopK());
        return ragSearchService.search(request);
    }

    @GetMapping
    public SearchResponse quickSearch(
            @RequestParam("q") String query,
            @RequestParam(value = "topK", defaultValue = "5") int topK,
            @RequestParam(value = "repoName", required = false) String repoName,
            @RequestParam(value = "chunkType", required = false) String chunkType,
            @RequestParam(value = "generateAnswer", defaultValue = "false") boolean generateAnswer) {

        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .repoName(repoName)
                .chunkType(chunkType)
                .generateAnswer(generateAnswer)
                .build();

        log.info("Quick search request received: query={}, topK={}", query, topK);
        return ragSearchService.search(request);
    }
}
