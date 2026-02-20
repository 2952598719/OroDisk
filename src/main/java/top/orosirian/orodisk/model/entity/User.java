package top.orosirian.orodisk.model.entity;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class User implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long userId;

    private String username;

    private String password;

    private Long totalQuota;

    private Long usedQuota;

    private Integer status;

    private Integer type;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;

}
