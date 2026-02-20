package top.orosirian.orodisk.model.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ShareInfoResponse {

    private String shareCode;

    private String fileName;

    private Long fileSize;

    private String fileSizeFormat;

    private Integer fileType;

    private Boolean hasPassword;

    private Boolean allowDownload;

    private LocalDateTime expireTime;

    private Integer maxDownloads;

    private Integer currentDownloads;

}
