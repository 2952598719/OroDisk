package top.orosirian.orodisk.model.request;

import lombok.Data;

@Data
public class MergeFileRequest {

    private String identifier;

    private String fileName;

    private Integer totalChunks;

    private Long totalSize;

    private Long parentId;

    private Long storageId;

}
