package top.orosirian.orodisk.model.response;

import lombok.Data;

import java.util.List;

@Data
public class CheckFileResponse {

    private Boolean skipUpload;

    private Long storageId;

    private List<Integer> uploadedChunks;

}
