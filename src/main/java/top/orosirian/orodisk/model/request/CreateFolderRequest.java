package top.orosirian.orodisk.model.request;

import lombok.Data;

@Data
public class CreateFolderRequest {

    private Long parentId = 0L;

    private String folderName;

}
