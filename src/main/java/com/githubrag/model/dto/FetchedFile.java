package com.githubrag.model.dto;

import com.githubrag.model.entity.RepoDocument;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Internal transfer object between GitHubFetchService and the ingestion
 * pipeline. Not persisted directly - DocumentIngestionService maps this
 * into a RepoDocument entity.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FetchedFile {

    private String repoOwner;
    private String repoName;
    private String filePath;
    private String rawContent;
    private RepoDocument.FileType fileType;
}
