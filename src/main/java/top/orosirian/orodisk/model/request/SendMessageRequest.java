package top.orosirian.orodisk.model.request;

import lombok.Data;

@Data
public class SendMessageRequest {

    private Long conversationId;

    private String content;

}
