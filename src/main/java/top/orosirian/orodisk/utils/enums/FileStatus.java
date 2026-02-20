package top.orosirian.orodisk.utils.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FileStatus {

    NORMAL(0),
    DELETED(1),
    RECYCLE(2),
    ;

    private final Integer code;

}
