package top.orosirian.orodisk.model.response;

import lombok.Data;

@Data
public class LoginResponse {

    private String token;

    private UserInfoResponse userInfo;

}
