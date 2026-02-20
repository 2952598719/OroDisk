package top.orosirian.orodisk.model.request;

import lombok.Data;

@Data
public class CreateConversationRequest {

    private String title;

    private Integer contextType = 0;

    private Long contextId;

}
