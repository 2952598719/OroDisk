package top.orosirian.orodisk.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.File;

@Data
@AllArgsConstructor
public class FileData {
    private File file;
    private String fileName;
}
