package com.githubrag.exception;

public class RepoNotFoundException extends RuntimeException {

    public RepoNotFoundException(String message) {
        super(message);
    }
}
