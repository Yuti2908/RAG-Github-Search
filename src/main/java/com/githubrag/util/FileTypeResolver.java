package com.githubrag.util;

import com.githubrag.model.entity.RepoDocument.FileType;

public final class FileTypeResolver {

    private FileTypeResolver() {
    }

    public static FileType resolve(String filePath) {
        String lowerPath = filePath.toLowerCase();
        String extension = extensionOf(lowerPath);

        return switch (extension) {
            case "md", "markdown" -> lowerPath.contains("readme") ? FileType.README : FileType.MARKDOWN;
            case "java" -> FileType.JAVA;
            case "py" -> FileType.PYTHON;
            case "js" -> FileType.JAVASCRIPT;
            case "ts" -> FileType.TYPESCRIPT;
            case "yml", "yaml" -> FileType.YAML;
            default -> FileType.OTHER;
        };
    }

    private static String extensionOf(String path) {
        int lastDot = path.lastIndexOf('.');
        int lastSlash = path.lastIndexOf('/');
        if (lastDot == -1 || lastDot < lastSlash) {
            return "";
        }
        return path.substring(lastDot + 1);
    }
}
