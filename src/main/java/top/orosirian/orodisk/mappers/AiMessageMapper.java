package top.orosirian.orodisk.mappers;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import top.orosirian.orodisk.model.entity.AiMessage;

import java.util.List;

@Mapper
public interface AiMessageMapper {

    int insert(AiMessage message);

    int deleteByConversationId(Long conversationId);

    List<AiMessage> selectByConversationId(@Param("conversationId") Long conversationId, @Param("limit") Integer limit);

    AiMessage selectLastByConversationId(Long conversationId);

}
