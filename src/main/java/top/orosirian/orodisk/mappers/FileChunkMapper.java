package top.orosirian.orodisk.mappers;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import top.orosirian.orodisk.model.entity.FileChunk;

import java.util.List;

@Mapper
public interface FileChunkMapper {

    int insert(FileChunk chunk);

    int deleteByIdentifier(@Param("fileIdentifier") String fileIdentifier);

    int deleteByIdentifierAndChunkNumber(@Param("fileIdentifier") String fileIdentifier, @Param("chunkNumber") Integer chunkNumber);

    List<FileChunk> selectByIdentifier(@Param("fileIdentifier") String fileIdentifier);

    List<Integer> selectUploadedChunkNumbers(@Param("fileIdentifier") String fileIdentifier);

    FileChunk selectByIdentifierAndChunkNumber(@Param("fileIdentifier") String fileIdentifier, @Param("chunkNumber") Integer chunkNumber);

    int countByIdentifier(@Param("fileIdentifier") String fileIdentifier);

}
