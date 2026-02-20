package top.orosirian.orodisk.model.request;

import lombok.Data;

@Data
public class RenameFileRequest {

    private Long fileId;

    private String newName;

}
