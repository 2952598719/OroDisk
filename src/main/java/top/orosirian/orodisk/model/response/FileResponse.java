package top.orosirian.orodisk.model.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FileResponse {

    private Long fileId;

    private Long parentId;

    private String fileName;

    private Integer fileType;

    private Long fileSize;

    private String fileSizeFormat;

    private Integer status;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;

}
