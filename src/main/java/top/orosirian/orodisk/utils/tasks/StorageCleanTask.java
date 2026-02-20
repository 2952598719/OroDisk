package top.orosirian.orodisk.utils.tasks;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import top.orosirian.orodisk.mappers.StorageMapper;
import top.orosirian.orodisk.model.entity.Storage;
import top.orosirian.orodisk.utils.Funcs;

import java.io.File;
import java.util.List;

@Slf4j
@Component
public class StorageCleanTask {

    @Value("${disk.storage.base-path}")
    private String basePath;

    private final StorageMapper storageMapper;
    public StorageCleanTask(StorageMapper storageMapper) {
        this.storageMapper = storageMapper;
    }

    @Scheduled(cron = "${disk.clean.cron}")
    public void cleanOrphanStorage() {
        log.info("开始清理孤立存储文件...");

        try {
            List<Storage> orphanStorages = storageMapper.selectByRefCountZero();
            int cleanedCount = 0;

            for (Storage storage : orphanStorages) {
                try {
                    File physicalFile = new File(basePath, storage.getStoragePath());
                    if (physicalFile.exists()) {
                        Funcs.deleteFile(physicalFile);
                        log.info("删除物理文件: {}", storage.getStoragePath());
                    }

                    storageMapper.deleteById(storage.getStorageId());
                    cleanedCount++;

                } catch (Exception e) {
                    log.error("清理存储失败: storageId={}", storage.getStorageId(), e);
                }
            }

            log.info("清理孤立存储文件完成，共清理 {} 个", cleanedCount);

        } catch (Exception e) {
            log.error("清理孤立存储文件异常", e);
        }
    }

}
