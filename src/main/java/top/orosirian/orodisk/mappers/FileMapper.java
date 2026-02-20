package top.orosirian.orodisk.mappers;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import top.orosirian.orodisk.model.entity.FileEntity;

import java.util.List;

@Mapper
public interface FileMapper {

    int insert(FileEntity file);

    int update(FileEntity file);

    int deleteById(Long fileId);

    FileEntity selectById(@Param("fileId") Long fileId, @Param("status") Integer status);

    List<FileEntity> selectByUserIdAndParentId(@Param("userId") Long userId, @Param("parentId") Long parentId, @Param("status") Integer status);

    List<FileEntity> selectByUserIdAndParentIdWithCursor(@Param("userId") Long userId,
                                                          @Param("parentId") Long parentId,
                                                          @Param("status") Integer status,
                                                          @Param("lastFileId") Long lastFileId,
                                                          @Param("pageSize") int pageSize);

    List<FileEntity> selectByUserId(@Param("userId") Long userId, @Param("status") Integer status);

    FileEntity selectByUserIdAndParentIdAndFileName(@Param("userId") Long userId, @Param("parentId") Long parentId, @Param("fileName") String fileName, @Param("status") Integer status);

    int updateStatus(@Param("fileId") Long fileId, @Param("status") Integer status);

    int updateStatusByParentId(@Param("parentId") Long parentId, @Param("status") Integer status);

    int updateStorageId(@Param("fileId") Long fileId, @Param("storageId") Long storageId);

    int updateParentId(@Param("fileId") Long fileId, @Param("parentId") Long parentId);

    int updateFileName(@Param("fileId") Long fileId, @Param("fileName") String fileName);

    List<FileEntity> selectByStorageId(Long storageId);

    List<FileEntity> selectByStorageIdCommon(@Param("storageId") Long storageId);

    int deleteByStorageId(Long storageId);

    Long sumFileSizeByUserId(@Param("userId") Long userId, @Param("status") Integer status);

    List<FileEntity> selectRecycleByUserId(Long userId);

    List<FileEntity> selectRecycleByUserIdWithCursor(@Param("userId") Long userId,
                                                      @Param("lastFileId") Long lastFileId,
                                                      @Param("pageSize") int pageSize);

    int restoreByFileId(Long fileId);

    int deleteRecycleByUserId(Long userId);

    Long selectStorageId(Long fileId);

    List<Long> selectStorageIdsByUserId(@Param("userId") Long userId, @Param("status") Integer status);

}
