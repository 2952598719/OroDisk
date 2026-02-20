package top.orosirian.orodisk.utils.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FileType {

    FOLDER(0),
    FILE(1),
    ;

    private final Integer code;
}
