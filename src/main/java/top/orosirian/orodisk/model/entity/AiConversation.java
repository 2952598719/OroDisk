package top.orosirian.orodisk.model.entity;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class AiConversation implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long conversationId;

    private Long userId;

    private String title;

    private Integer contextType;

    private Long contextId;

    private String modelName;

    private Integer status;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;

}
