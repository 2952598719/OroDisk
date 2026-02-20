package top.orosirian.orodisk.model.entity;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class AiMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long messageId;

    private Long conversationId;

    private String role;

    private String content;

    private Integer tokenCount;

    private LocalDateTime createdTime;

}
