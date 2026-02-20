package top.orosirian.orodisk.mappers;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import top.orosirian.orodisk.model.entity.AiConversation;

import java.util.List;

@Mapper
public interface AiConversationMapper {

    int insert(AiConversation conversation);

    int update(AiConversation conversation);

    int deleteById(Long conversationId);

    AiConversation selectById(Long conversationId);

    List<AiConversation> selectByUserId(@Param("userId") Long userId, @Param("status") Integer status);

    int updateStatus(@Param("conversationId") Long conversationId, @Param("status") Integer status);

    int updateTitle(@Param("conversationId") Long conversationId, @Param("title") String title);

}
