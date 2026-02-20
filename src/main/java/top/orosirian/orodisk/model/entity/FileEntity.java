package top.orosirian.orodisk.model.entity;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class FileEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long fileId;

    private Long userId;

    private Long parentId;

    private Long storageId;

    private String fileName;

    private Integer fileType;

    private Long fileSize;

    private Integer status;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;

}
