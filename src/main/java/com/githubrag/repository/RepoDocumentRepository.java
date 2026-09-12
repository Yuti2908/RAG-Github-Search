package com.githubrag.repository;

import com.githubrag.model.entity.RepoDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RepoDocumentRepository extends JpaRepository<RepoDocument, Long> {

    Optional<RepoDocument> findByRepoOwnerAndRepoNameAndFilePath(
            String repoOwner, String repoName, String filePath);

    List<RepoDocument> findByRepoOwnerAndRepoName(String repoOwner, String repoName);

    List<RepoDocument> findByRepoOwner(String repoOwner);
}
