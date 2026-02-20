package top.orosirian.orodisk.model.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ConversationResponse {

    private Long conversationId;

    private String title;

    private Integer contextType;

    private Long contextId;

    private String modelName;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;

    private List<MessageResponse> messages;

}
