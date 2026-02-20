package top.orosirian.orodisk.model.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ShareResponse {

    private Long shareId;

    private String shareCode;

    private String shareUrl;

    private Long fileId;

    private String fileName;

    private Boolean hasPassword;

    private LocalDateTime expireTime;

    private Integer maxDownloads;

    private Integer currentDownloads;

    private Boolean allowDownload;

    private Integer status;

    private LocalDateTime createdTime;

}
