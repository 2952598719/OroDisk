package top.orosirian.orodisk.model.entity;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class FileShare implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long shareId;

    private String shareCode;

    private Long fileId;

    private Long userId;

    private String password;

    private LocalDateTime expireTime;

    private Integer maxDownloads;

    private Integer currentDownloads;

    private Boolean allowDownload;

    private Integer status;

    private LocalDateTime createdTime;

}
