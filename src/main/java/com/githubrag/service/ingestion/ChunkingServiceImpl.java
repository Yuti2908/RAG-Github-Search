package com.githubrag.service.ingestion;

import com.githubrag.model.dto.FetchedFile;
import com.githubrag.model.dto.TextChunk;
import com.githubrag.model.entity.DocumentChunk;
import com.githubrag.model.entity.RepoDocument.FileType;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ChunkingServiceImpl implements ChunkingService {

    private static final int CHUNK_SIZE = 500;
    private static final int CHUNK_OVERLAP = 50;

    private static final Set<FileType> PROSE_TYPES = Set.of(FileType.README, FileType.MARKDOWN);

    /**
     * Approximate Java method/class boundary matcher. Looks for lines with
     * typical modifiers followed by a class/interface/enum declaration, or
     * a method signature ending in "{". Deliberately simple regex, not a
     * real parser - good enough to keep a method/class body together.
     */
    private static final Pattern JAVA_BOUNDARY_PATTERN = Pattern.compile(
            "^\\s*(public|private|protected|static|final|abstract)?\\s*" +
            "(class|interface|enum|record)\\s+\\w+" +
            "|^\\s*(public|private|protected|static|final|synchronized)+\\s+[\\w<>\\[\\],\\s]+\\s+\\w+\\s*\\([^)]*\\)\\s*\\{?\\s*$"
    );

    @Override
    public List<TextChunk> chunk(FetchedFile file) {
        if (file.getRawContent() == null || file.getRawContent().isBlank()) {
            return List.of();
        }

        if (PROSE_TYPES.contains(file.getFileType())) {
            return chunkFixedSize(file.getRawContent(), DocumentChunk.ChunkType.PROSE);
        }

        if (file.getFileType() == FileType.JAVA) {
            List<TextChunk> boundaryChunks = chunkByJavaBoundaries(file.getRawContent());
            if (!boundaryChunks.isEmpty()) {
                return boundaryChunks;
            }
            // Fallback if no boundaries matched (e.g. interface with no methods)
        }

        return chunkFixedSize(file.getRawContent(), DocumentChunk.ChunkType.CODE);
    }

    private List<TextChunk> chunkFixedSize(String content, DocumentChunk.ChunkType chunkType) {
        List<TextChunk> chunks = new ArrayList<>();
        int index = 0;
        int position = 0;
        int length = content.length();

        while (position < length) {
            int end = Math.min(position + CHUNK_SIZE, length);
            String piece = content.substring(position, end);
            chunks.add(TextChunk.builder()
                    .chunkIndex(index++)
                    .content(piece)
                    .chunkType(chunkType)
                    .build());

            if (end == length) {
                break;
            }
            position = end - CHUNK_OVERLAP;
        }
        return chunks;
    }

    private List<TextChunk> chunkByJavaBoundaries(String content) {
        String[] lines = content.split("\n", -1);
        List<Integer> boundaryLineIndices = new ArrayList<>();

        for (int i = 0; i < lines.length; i++) {
            Matcher matcher = JAVA_BOUNDARY_PATTERN.matcher(lines[i]);
            if (matcher.find()) {
                boundaryLineIndices.add(i);
            }
        }

        if (boundaryLineIndices.isEmpty()) {
            return List.of();
        }

        List<TextChunk> chunks = new ArrayList<>();
        int chunkIndex = 0;

        // Everything before the first boundary (package/imports/fields) becomes chunk 0
        if (boundaryLineIndices.get(0) > 0) {
            String header = String.join("\n", java.util.Arrays.copyOfRange(lines, 0, boundaryLineIndices.get(0)));
            if (!header.isBlank()) {
                chunks.add(buildCodeChunk(chunkIndex++, header));
            }
        }

        for (int b = 0; b < boundaryLineIndices.size(); b++) {
            int start = boundaryLineIndices.get(b);
            int end = (b + 1 < boundaryLineIndices.size()) ? boundaryLineIndices.get(b + 1) : lines.length;
            String piece = String.join("\n", java.util.Arrays.copyOfRange(lines, start, end));
            if (!piece.isBlank()) {
                chunks.add(buildCodeChunk(chunkIndex++, piece));
            }
        }

        return chunks;
    }

    private TextChunk buildCodeChunk(int index, String content) {
        return TextChunk.builder()
                .chunkIndex(index)
                .content(content)
                .chunkType(DocumentChunk.ChunkType.CODE)
                .build();
    }
}
