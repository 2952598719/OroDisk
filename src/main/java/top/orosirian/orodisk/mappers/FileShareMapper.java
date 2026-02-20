package top.orosirian.orodisk.mappers;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import top.orosirian.orodisk.model.entity.FileShare;

import java.util.List;

@Mapper
public interface FileShareMapper {

    int insert(FileShare share);

    int update(FileShare share);

    int deleteById(Long shareId);

    FileShare selectById(Long shareId);

    FileShare selectByShareCode(@Param("shareCode") String shareCode);

    List<FileShare> selectByUserId(Long userId);

    int updateStatus(@Param("shareId") Long shareId, @Param("status") Integer status);

    int incrementDownloads(Long shareId);

    int deleteByFileId(Long fileId);

}
