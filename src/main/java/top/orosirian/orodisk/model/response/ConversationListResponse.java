package top.orosirian.orodisk.model.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ConversationListResponse {

    private Long conversationId;

    private String title;

    private Integer contextType;

    private Long contextId;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;

}
