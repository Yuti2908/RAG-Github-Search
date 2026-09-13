package com.githubrag.service.github;

import com.githubrag.model.dto.FetchedFile;

import java.util.List;

public interface GitHubFetchService {

    /** Lists public, non-forked repo names for the given GitHub username. */
    List<String> listPublicRepos(String username);

    /** Recursively fetches all relevant files (README, source) for a given repo. */
    List<FetchedFile> fetchRepoFiles(String owner, String repoName);
}
