package top.orosirian.orodisk.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import top.orosirian.orodisk.mappers.FileMapper;
import top.orosirian.orodisk.mappers.StorageMapper;
import top.orosirian.orodisk.model.entity.Storage;
import top.orosirian.orodisk.utils.Funcs;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Slf4j
@Service
public class DocumentVectorService {

    private final FileMapper fileMapper;
    @Value("${disk.storage.base-path}")
    private String basePath;

    private final VectorStore vectorStore;
    private final StorageMapper storageMapper;

    private static final List<String> SUPPORTED_EXTENSIONS = List.of(
            "txt", "md", "doc", "docx", "xls", "xlsx",
            "ppt", "pptx", "csv", "json", "xml", "html", "htm"
    );

    public DocumentVectorService(VectorStore vectorStore, StorageMapper storageMapper, FileMapper fileMapper) {
        this.vectorStore = vectorStore;
        this.storageMapper = storageMapper;
        this.fileMapper = fileMapper;
    }

    public boolean isSupportedFileType(String fileName) {
        String extension = Funcs.getExtension(fileName).toLowerCase();
        return SUPPORTED_EXTENSIONS.contains(extension);
    }

    public void vectorizeFile(Long storageId) {
        Storage storage = storageMapper.selectById(storageId);
        if (storage == null) {
            log.warn("File not found for vectorization: storageId={}", storageId);
            return;
        }

        Path path = Paths.get(storage.getStoragePath());
        String fileName = path.getFileName().toString();
        String extension = Funcs.getExtension(fileName).toLowerCase();
        boolean isSupportedFileType = SUPPORTED_EXTENSIONS.contains(extension);
        if (!isSupportedFileType) {
            log.info("File type not supported for vectorization: fileName={}", fileName);
            return;
        }

        File physicalFile = new File(basePath, storage.getStoragePath());
        if (!physicalFile.exists()) {
            log.warn("Physical file not found: {}", physicalFile.getAbsolutePath());
            return;
        }

        try {
            FileSystemResource resource = new FileSystemResource(physicalFile);
            List<Document> documents = switch (extension) {
                case "txt", "md", "csv", "json", "xml", "html", "htm" -> {
                    TextReader reader = new TextReader(resource);
                    reader.getCustomMetadata().put("source", fileName);
                    yield reader.get();
                }
                default -> {
                    TikaDocumentReader reader = new TikaDocumentReader(resource);
                    yield reader.get();
                }
            };
            if (documents.isEmpty()) {
                log.info("No content extracted from file: fileName={}", fileName);
                return;
            }

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("storageId", storageId);
            for (Document doc : documents) {
                doc.getMetadata().putAll(metadata);
            }

            try {
                vectorStore.delete("storageId == " + storageId);
            } catch (Exception e) {
                log.warn("Failed to delete vectors for storageId={}: {}", storageId, e.getMessage());
            }

            vectorStore.add(documents);
            
            log.info("File vectorized successfully: storageId={}, chunks={}", storageId, documents.size());
                    
        } catch (Exception e) {
            log.error("Failed to vectorize file: storageId={}, error={}", storageId, e.getMessage(), e);
        }
    }

    public List<Document> searchByFileIds(String query, List<Long> fileIds, int topK) {
        try {
            List<Long> storageIds = new ArrayList<>();
            for (Long fileId : fileIds) {
                storageIds.add(fileMapper.selectStorageId(fileId));
            }

            StringBuilder builder = new StringBuilder();
            for (int i = 0; i <= storageIds.size() - 1; i++) {
                if (i != 0) builder.append(" || ");
                builder.append("storageId == ").append("\"").append(storageIds.get(i)).append("\"");
            }

            return vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(query)
                            .filterExpression(builder.toString())
                            .topK(topK)
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to search by storage ids: {}", e.getMessage());
            return List.of();
        }
    }

}
