package top.orosirian.orodisk.model.request;

import lombok.Data;

@Data
public class VerifyShareRequest {

    private String shareCode;

    private String password;

}
