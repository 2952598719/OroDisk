package top.orosirian.orodisk.mappers;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import top.orosirian.orodisk.model.entity.User;

import java.util.List;

@Mapper
public interface UserMapper {

    int insert(User user);

    int update(User user);

    int deleteById(Long userId);

    User selectById(@Param("userId") Long userId, @Param("status") Integer status);

    User selectByUsername(@Param("username") String username, @Param("status") Integer status);

    List<User> selectAll();

    int updateQuota(@Param("userId") Long userId, @Param("usedQuota") Long usedQuota);

    int incrementQuota(@Param("userId") Long userId, @Param("fileSize") Long fileSize);

    int decrementQuota(@Param("userId") Long userId, @Param("fileSize") Long fileSize);

    int updateStatus(@Param("userId") Long userId, @Param("status") Integer status);

}
