package com.githubrag.service.github;

import com.fasterxml.jackson.databind.JsonNode;
import com.githubrag.exception.ExternalApiException;
import com.githubrag.model.dto.FetchedFile;
import com.githubrag.model.entity.RepoDocument;
import com.githubrag.util.FileTypeResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Fetches repo data from GitHub using unauthenticated REST calls.
 * Note: unauthenticated requests are capped at 60/hour by GitHub -
 * fine for occasional ingestion of a handful of repos, but swap in a
 * Personal Access Token (Authorization header) if you hit that limit.
 */
@Slf4j
@Service
public class GitHubFetchServiceImpl implements GitHubFetchService {

    private static final Set<String> IGNORED_DIRS = Set.of(
            "node_modules", ".git", "target", "build", "dist", "venv", "__pycache__", ".idea"
    );

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "md", "java", "py", "js", "ts", "yml", "yaml"
    );

    private final WebClient githubApiWebClient;
    private final WebClient githubRawWebClient;

    public GitHubFetchServiceImpl(
            @Qualifier("githubApiWebClient") WebClient githubApiWebClient,
            @Qualifier("githubRawWebClient") WebClient githubRawWebClient) {
        this.githubApiWebClient = githubApiWebClient;
        this.githubRawWebClient = githubRawWebClient;
    }

    @Override
    public List<String> listPublicRepos(String username) {
        try {
            JsonNode repos = githubApiWebClient.get()
                    .uri("/users/{username}/repos?per_page=100&type=owner", username)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            List<String> repoNames = new ArrayList<>();
            if (repos != null) {
                repos.forEach(repo -> {
                    boolean isFork = repo.path("fork").asBoolean(false);
                    if (!isFork) {
                        repoNames.add(repo.path("name").asText());
                    }
                });
            }
            return repoNames;
        } catch (WebClientResponseException ex) {
            throw new ExternalApiException(
                    "Failed to list repos for user " + username + ": " + ex.getStatusCode(), ex);
        }
    }

    @Override
    public List<FetchedFile> fetchRepoFiles(String owner, String repoName) {
        String defaultBranch = fetchDefaultBranch(owner, repoName);
        List<String> filePaths = fetchFileTree(owner, repoName, defaultBranch);

        List<FetchedFile> files = new ArrayList<>();
        for (String path : filePaths) {
            String content = fetchRawContent(owner, repoName, defaultBranch, path);
            if (content == null) {
                continue;
            }
            RepoDocument.FileType fileType = FileTypeResolver.resolve(path);
            files.add(FetchedFile.builder()
                    .repoOwner(owner)
                    .repoName(repoName)
                    .filePath(path)
                    .rawContent(content)
                    .fileType(fileType)
                    .build());
        }
        return files;
    }

    private String fetchDefaultBranch(String owner, String repoName) {
        try {
            JsonNode repo = githubApiWebClient.get()
                    .uri("/repos/{owner}/{repo}", owner, repoName)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
            return repo != null ? repo.path("default_branch").asText("main") : "main";
        } catch (WebClientResponseException ex) {
            throw new ExternalApiException(
                    "Failed to fetch repo metadata for " + owner + "/" + repoName, ex);
        }
    }

    private List<String> fetchFileTree(String owner, String repoName, String branch) {
        try {
            JsonNode tree = githubApiWebClient.get()
                    .uri("/repos/{owner}/{repo}/git/trees/{branch}?recursive=1", owner, repoName, branch)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            List<String> paths = new ArrayList<>();
            if (tree != null && tree.has("tree")) {
                tree.get("tree").forEach(entry -> {
                    String type = entry.path("type").asText();
                    String path = entry.path("path").asText();
                    if ("blob".equals(type) && isIncluded(path)) {
                        paths.add(path);
                    }
                });
            }
            return paths;
        } catch (WebClientResponseException ex) {
            throw new ExternalApiException(
                    "Failed to fetch file tree for " + owner + "/" + repoName, ex);
        }
    }

    private String fetchRawContent(String owner, String repoName, String branch, String path) {
        // Built as a plain string (not a URI template var) since paths
        // contain slashes that must stay unencoded, e.g. src/main/Foo.java
        String uri = "/" + owner + "/" + repoName + "/" + branch + "/" + path;
        try {
            return githubRawWebClient.get()
                    .uri(uri)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (WebClientResponseException ex) {
            log.warn("Skipping file {} in {}/{} - {}", path, owner, repoName, ex.getStatusCode());
            return null;
        }
    }

    private boolean isIncluded(String path) {
        for (String ignored : IGNORED_DIRS) {
            if (path.contains(ignored + "/")) {
                return false;
            }
        }
        int lastDot = path.lastIndexOf('.');
        if (lastDot == -1) {
            return false;
        }
        String extension = path.substring(lastDot + 1).toLowerCase();
        return ALLOWED_EXTENSIONS.contains(extension);
    }
}
