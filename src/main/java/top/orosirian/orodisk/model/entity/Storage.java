package top.orosirian.orodisk.model.entity;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class Storage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long storageId;

    private Long storageSize;

    private String storagePath;

    private String md5;

    private Integer refCount;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;

}
