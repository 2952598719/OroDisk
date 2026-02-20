package top.orosirian.orodisk.mappers;

import org.apache.ibatis.annotations.Mapper;
import top.orosirian.orodisk.model.entity.Storage;

import java.util.List;

@Mapper
public interface StorageMapper {

    int insert(Storage storage);

    int update(Storage storage);

    int deleteById(Long storageId);

    Storage selectById(Long storageId);

    Storage selectByMd5(String md5);

    List<Storage> selectAll();

    int incrementRefCount(Long storageId);

    int decrementRefCount(Long storageId);

    List<Storage> selectByRefCountZero();

}
