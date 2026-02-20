package top.orosirian.orodisk.model.response;

import lombok.Data;

import java.util.List;

@Data
public class FileListResponse {

    private List<FileResponse> files;

    private Long totalSize;

    private String totalSizeFormat;

    private Long lastFileId;

    private Boolean hasMore;

}
