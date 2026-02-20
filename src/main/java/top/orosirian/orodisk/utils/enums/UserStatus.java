package top.orosirian.orodisk.utils.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserStatus {

    FORBIDDEN(0),
    NORMAL(1),
    UNREGISTER(2),
    ;

    private final Integer code;

}
