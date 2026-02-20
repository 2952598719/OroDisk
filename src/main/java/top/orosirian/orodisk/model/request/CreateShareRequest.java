package top.orosirian.orodisk.model.request;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateShareRequest {

    private Long fileId;

    private String password;

    private LocalDateTime expireTime;

    private Integer maxDownloads;

    private Boolean allowDownload;

}
