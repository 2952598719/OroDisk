package top.orosirian.orodisk.model.entity;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class FileChunk implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long chunkId;

    private String fileIdentifier;

    private Integer chunkNumber;

    private Long chunkSize;

    private Integer totalChunks;

    private Long totalSize;

    private String storagePath;

    private Long userId;

    private LocalDateTime createdTime;

}
