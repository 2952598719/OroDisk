package top.orosirian.orodisk.model.response;

import lombok.Data;
import top.orosirian.orodisk.model.entity.User;

@Data
public class UserInfoResponse {

    private Long userId;

    private String username;

    private Long totalQuota;

    private Long usedQuota;

    private Integer status;

    private Integer type;

    public UserInfoResponse (User user) {
        this.userId = user.getUserId();
        this.username = user.getUsername();
        this.totalQuota = user.getTotalQuota();
        this.usedQuota = user.getUsedQuota();
        this.status = user.getStatus();
        this.type = user.getType();
    }

}
