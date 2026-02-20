package top.orosirian.orodisk.model.request;

import lombok.Data;

@Data
public class MoveFileRequest {

    private Long fileId;

    private Long targetParentId;

}
