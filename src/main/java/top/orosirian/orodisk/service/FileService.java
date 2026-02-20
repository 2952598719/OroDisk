package top.orosirian.orodisk.service;

import cn.dev33.satoken.stp.StpUtil;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import top.orosirian.orodisk.mappers.FileChunkMapper;
import top.orosirian.orodisk.mappers.FileMapper;
import top.orosirian.orodisk.mappers.FileShareMapper;
import top.orosirian.orodisk.mappers.StorageMapper;
import top.orosirian.orodisk.model.dto.FileData;
import top.orosirian.orodisk.model.entity.FileChunk;
import top.orosirian.orodisk.model.entity.FileEntity;
import top.orosirian.orodisk.model.entity.Storage;
import top.orosirian.orodisk.model.request.CreateFolderRequest;
import top.orosirian.orodisk.model.request.MergeFileRequest;
import top.orosirian.orodisk.model.request.MoveFileRequest;
import top.orosirian.orodisk.model.request.RenameFileRequest;
import top.orosirian.orodisk.model.response.CheckFileResponse;
import top.orosirian.orodisk.model.response.FileListResponse;
import top.orosirian.orodisk.model.response.FileResponse;
import top.orosirian.orodisk.utils.exceptions.BusinessException;
import top.orosirian.orodisk.utils.Constant;
import top.orosirian.orodisk.utils.DistributedLock;
import top.orosirian.orodisk.utils.Funcs;
import top.orosirian.orodisk.utils.enums.FileStatus;
import top.orosirian.orodisk.utils.enums.FileType;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class FileService {

    @Value("${disk.storage.base-path}")
    private String basePath;

    @Value("${disk.storage.temp-path}")
    private String tempPath;

    private final FileMapper fileMapper;
    private final StorageMapper storageMapper;
    private final FileChunkMapper fileChunkMapper;
    private final FileShareMapper fileShareMapper;
    private final UserService userService;
    private final StringRedisTemplate redisTemplate;


    public FileService(FileMapper fileMapper, StorageMapper storageMapper, FileChunkMapper fileChunkMapper, 
                       FileShareMapper fileShareMapper, UserService userService, StringRedisTemplate redisTemplate) {
        this.fileMapper = fileMapper;
        this.storageMapper = storageMapper;
        this.fileChunkMapper = fileChunkMapper;
        this.fileShareMapper = fileShareMapper;
        this.userService = userService;
        this.redisTemplate = redisTemplate;
    }

    private static final int DEFAULT_PAGE_SIZE = 50;

    public FileListResponse listFiles(Long parentId, Long lastFileId, Integer pageSize) {
        Long userId = StpUtil.getLoginIdAsLong();
        int size = pageSize != null && pageSize > 0 ? pageSize : DEFAULT_PAGE_SIZE;
        
        List<FileEntity> files = fileMapper.selectByUserIdAndParentIdWithCursor(
                userId, parentId, FileStatus.NORMAL.getCode(), lastFileId, size + 1);

        FileListResponse response = new FileListResponse();
        response.setFiles(new ArrayList<>());

        Long totalSize = 0L;
        int count = 0;
        Long lastId = null;
        
        for (FileEntity file : files) {
            if (count >= size) {
                break;
            }
            FileResponse fileResponse = convertToFileResponse(file);
            response.getFiles().add(fileResponse);
            totalSize += file.getFileSize();
            lastId = file.getFileId();
            count++;
        }

        response.setTotalSize(totalSize);
        response.setTotalSizeFormat(Funcs.formatFileSize(totalSize));
        response.setLastFileId(lastId);
        response.setHasMore(files.size() > size);
        return response;
    }

    @Transactional
    public FileResponse createFolder(CreateFolderRequest request) {
        // 并发问题：事务A和B分别SELECT，都发现existFile==null，都执行插入，于是多个同名文件夹
        // 不过sql加了(user_id, parent_id, file_name, status)的唯一索引，这里其实原先就可以解决，不过还是加上分布式锁，在业务层面解决
        Long userId = StpUtil.getLoginIdAsLong();
        String lockKey = Constant.FOLDER_LOCK_PREFIX + userId + ":" + request.getParentId() + ":" + request.getFolderName();
        try (DistributedLock _ = new DistributedLock(redisTemplate, lockKey, Constant.LOCK_LEASE_TIME).lock()) {
            FileEntity existFile = fileMapper.selectByUserIdAndParentIdAndFileName(userId, request.getParentId(), request.getFolderName(), FileStatus.NORMAL.getCode());
            if (existFile != null) {
                throw new BusinessException("Folder already exists");
            }

            FileEntity folder = new FileEntity();
            folder.setUserId(userId);
            folder.setParentId(request.getParentId());
            folder.setStorageId(0L);
            folder.setFileName(request.getFolderName());
            folder.setFileType(FileType.FOLDER.getCode());
            folder.setFileSize(0L);
            folder.setStatus(FileStatus.NORMAL.getCode());

            fileMapper.insert(folder);
            log.info("创建文件夹成功: userId={}, folderName={}", userId, request.getFolderName());

            return convertToFileResponse(folder);
        }
    }


    @Transactional
    public FileResponse uploadFile(MultipartFile multipartFile, Long parentId, String md5) throws IOException {
        Long userId = StpUtil.getLoginIdAsLong();
        
        // ========== 第一步：前置校验 ==========
        UserService.UserQuota quota = userService.getUserQuota(userId);
        
        String originalFilename = multipartFile.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            originalFilename = "unnamed";
        }
        
        File tempFile = saveToTempFile(multipartFile);
        long fileSize = tempFile.length();
        
        if (quota.getUsedQuota() + fileSize > quota.getTotalQuota()) {
            boolean _ = tempFile.delete();
            throw new BusinessException("Insufficient storage space");
        }
        
        // ========== 第二步：获取分布式锁 ==========
        // 文件名锁：防止同名文件并发上传
        String fileLockKey = Constant.FILE_LOCK_PREFIX + userId + ":" + parentId + ":" + originalFilename;
        // MD5锁：防止相同内容文件并发上传（秒传场景）
        String md5LockKey = Constant.UPLOAD_LOCK_PREFIX + md5;
        
        DistributedLock fileLock = new DistributedLock(redisTemplate, fileLockKey, Constant.LOCK_LEASE_TIME);
        DistributedLock md5Lock = new DistributedLock(redisTemplate, md5LockKey, Constant.LOCK_LEASE_TIME);
        
        // 快速失败策略：尝试获取锁，不等待
        if (!fileLock.tryLock()) {
            boolean _ = tempFile.delete();
            throw new BusinessException("File with the same name is being uploaded, please retry later");
        }
        try {
            if (!md5Lock.tryLock()) {
                throw new BusinessException("File with the same content is being uploaded, please retry later");
            }
            try {
                // ========== 第三步：文件名冲突检查 ==========
                FileEntity existFile = fileMapper.selectByUserIdAndParentIdAndFileName(userId, parentId, originalFilename, FileStatus.NORMAL.getCode());
                if (existFile != null) {
                    throw new BusinessException("File already exists");
                }
                
                // ========== 第四步：处理物理文件（秒传或新建） ==========
                Storage storage = storageMapper.selectByMd5(md5);
                boolean isStorageExist = storage != null;
                
                if (isStorageExist) {
                    storageMapper.incrementRefCount(storage.getStorageId());
                    boolean _ = tempFile.delete();
                    log.info("Instant upload: userId={}, fileName={}, md5={}", userId, originalFilename, md5);
                } else {
                    String extension = Funcs.getExtension(originalFilename);
                    String storagePath = Funcs.generateStoragePath(md5, extension);
                    Path targetPath = Paths.get(basePath, storagePath);
                    Files.createDirectories(targetPath.getParent());
                    Files.move(tempFile.toPath(), targetPath);
                    boolean _ = tempFile.delete();
                    
                    storage = new Storage();
                    storage.setStorageSize(fileSize);
                    storage.setStoragePath(storagePath);
                    storage.setMd5(md5);
                    storage.setRefCount(1);
                    storageMapper.insert(storage);
                }
                
                FileEntity file = createFileRecord(userId, parentId, storage.getStorageId(), originalFilename, FileType.FILE.getCode(), fileSize);
                userService.incrementUsedQuota(userId, fileSize);

                log.info("Upload file success: userId={}, fileName={}, md5={}", userId, originalFilename, md5);
                return convertToFileResponse(file);
                
            } finally {
                md5Lock.unlock();
            }
        } finally {
            fileLock.unlock();
            // 确保临时文件被删除（异常情况下）
            if (tempFile.exists()) {
                boolean _ = tempFile.delete();
            }
        }
    }

    @Transactional
    public void deleteFile(Long fileId) {
        Long userId = StpUtil.getLoginIdAsLong();

        FileEntity file = fileMapper.selectById(fileId, FileStatus.NORMAL.getCode());
        if (file == null) {
            throw new BusinessException("File not found");
        }

        if (!file.getUserId().equals(userId)) {
            throw new BusinessException("No permission to delete this file");
        }

        if (Objects.equals(file.getFileType(), FileType.FOLDER.getCode())) {
            throw new BusinessException("Deleting folders is not allowed here");
        }

        fileMapper.updateStatus(fileId, FileStatus.RECYCLE.getCode());
        fileShareMapper.deleteByFileId(fileId);

        log.info("文件已移至回收站: fileId={}", fileId);
    }

    @Transactional
    public void deletePermanently(Long fileId) {
        // 会操作refCount与usedQuota，如果出现并发冲突，不像deleteFile那样结果一样，而会减掉两次导致错误
        Long userId = StpUtil.getLoginIdAsLong();

        FileEntity file = fileMapper.selectById(fileId, FileStatus.RECYCLE.getCode());
        if (file == null) {
            throw new BusinessException("File not found");
        }

        if (!file.getUserId().equals(userId)) {
            throw new BusinessException("No permission to delete this file");
        }

        if (Objects.equals(file.getFileType(), FileType.FOLDER.getCode())) {
            throw new BusinessException("Deleting folders is not allowed here");
        }

        String lockKey = Constant.FILE_LOCK_PREFIX + userId + ":" + fileId;
        try (DistributedLock _ = new DistributedLock(redisTemplate, lockKey, Constant.LOCK_LEASE_TIME).lock()) {
            // 再次检查文件是否仍存在（可能已被其他请求删除）
            FileEntity fileAgain = fileMapper.selectById(fileId, FileStatus.RECYCLE.getCode());
            if (fileAgain == null) {
                return;
            }

            Long fileSize = fileAgain.getFileSize();

            if (fileAgain.getStorageId() != null && fileAgain.getStorageId() > 0) {
                storageMapper.decrementRefCount(fileAgain.getStorageId());
            }

            fileMapper.deleteById(fileId);
            fileShareMapper.deleteByFileId(fileId);

            if (fileSize > 0) {
                userService.decrementUsedQuota(userId, fileSize);
            }

            log.info("文件已永久删除: fileId={}, fileSize={}", fileId, fileSize);
        }
    }

    public FileListResponse listRecycle(Long lastFileId, Integer pageSize) {
        Long userId = StpUtil.getLoginIdAsLong();
        int size = pageSize != null && pageSize > 0 ? pageSize : DEFAULT_PAGE_SIZE;
        
        List<FileEntity> files = fileMapper.selectRecycleByUserIdWithCursor(userId, lastFileId, size + 1);

        FileListResponse response = new FileListResponse();
        response.setFiles(new ArrayList<>());

        Long totalSize = 0L;
        int count = 0;
        Long lastId = null;
        
        for (FileEntity file : files) {
            if (count >= size) {
                break;
            }
            FileResponse fileResponse = convertToFileResponse(file);
            response.getFiles().add(fileResponse);
            totalSize += file.getFileSize();
            lastId = file.getFileId();
            count++;
        }

        response.setTotalSize(totalSize);
        response.setTotalSizeFormat(Funcs.formatFileSize(totalSize));
        response.setLastFileId(lastId);
        response.setHasMore(files.size() > size);
        return response;
    }

    @Transactional
    public void restoreFile(Long fileId) {
        // 并发冲突：同时恢复回收站中两个重名文件，各自SELECT后发现不冲突就都恢复为正常文件，导致重名
        Long userId = StpUtil.getLoginIdAsLong();

        FileEntity file = fileMapper.selectById(fileId, FileStatus.RECYCLE.getCode());
        if (file == null) {
            throw new BusinessException("File not found");
        }

        if (!file.getUserId().equals(userId)) {
            throw new BusinessException("No permission to restore this file");
        }

        if (Objects.equals(file.getFileType(), FileType.FOLDER.getCode())) {
            throw new BusinessException("Folder operations are not allowed here");
        }

        String lockKey = Constant.FILE_LOCK_PREFIX + userId + ":" + file.getParentId() + ":" + file.getFileName();
        try (DistributedLock _ = new DistributedLock(redisTemplate, lockKey, Constant.LOCK_LEASE_TIME).lock()) {
            FileEntity existFile = fileMapper.selectByUserIdAndParentIdAndFileName(userId, file.getParentId(), file.getFileName(), FileStatus.NORMAL.getCode());
            if (existFile != null) {
                throw new BusinessException("A file with the same name already exists in the original location");
            }

            fileMapper.restoreByFileId(fileId);

            log.info("文件已恢复: fileId={}", fileId);
        }
    }

    @Transactional
    public void clearRecycle() {
        Long userId = StpUtil.getLoginIdAsLong();

        String lockKey = Constant.LOCK_PREFIX + "recycle:" + userId;
        try (DistributedLock _ = new DistributedLock(redisTemplate, lockKey, Constant.LOCK_LEASE_TIME).lock()) {
            List<FileEntity> files = fileMapper.selectRecycleByUserId(userId);

            for (FileEntity file : files) {
                if (Objects.equals(file.getFileType(), FileType.FOLDER.getCode())) {
                    throw new BusinessException("Recycle bin contains folders, cannot clear");
                }
            }

            Long totalFileSize = 0L;
            for (FileEntity file : files) {
                if (Objects.equals(file.getFileType(), FileType.FILE.getCode())) {
                    totalFileSize += file.getFileSize();
                }
            }

            for (FileEntity file : files) {
                if (file.getStorageId() != null && file.getStorageId() > 0) {
                    storageMapper.decrementRefCount(file.getStorageId());
                }
            }

            fileMapper.deleteRecycleByUserId(userId);

            if (totalFileSize > 0) {
                userService.decrementUsedQuota(userId, totalFileSize);
            }

            log.info("回收站已清空: userId={}, totalFileSize={}", userId, totalFileSize);
        }
    }

    @Transactional
    public FileResponse renameFile(RenameFileRequest request) {
        // 并发冲突：同样文件名
        Long userId = StpUtil.getLoginIdAsLong();

        FileEntity file = fileMapper.selectById(request.getFileId(), FileStatus.NORMAL.getCode());
        if (file == null) {
            throw new BusinessException("File not found");
        }

        if (!file.getUserId().equals(userId)) {
            throw new BusinessException("No permission to rename this file");
        }

        String lockKey = Constant.FILE_LOCK_PREFIX + userId + ":" + file.getParentId() + ":" + request.getNewName();
        try (DistributedLock _ = new DistributedLock(redisTemplate, lockKey, Constant.LOCK_LEASE_TIME).lock()) {
            FileEntity existFile = fileMapper.selectByUserIdAndParentIdAndFileName(userId, file.getParentId(), request.getNewName(), FileStatus.NORMAL.getCode());
            if (existFile != null) {
                throw new BusinessException("File name already exists");
            }

            fileMapper.updateFileName(request.getFileId(), request.getNewName());
            file.setFileName(request.getNewName());

            log.info("文件重命名成功: fileId={}, newName={}", request.getFileId(), request.getNewName());
            return convertToFileResponse(file);
        }
    }

    @Transactional
    public FileResponse moveFile(MoveFileRequest request) {
        // 并发冲突：同样文件名
        Long userId = StpUtil.getLoginIdAsLong();

        FileEntity file = fileMapper.selectById(request.getFileId(), FileStatus.NORMAL.getCode());
        if (file == null) {
            throw new BusinessException("File not found");
        }

        if (!file.getUserId().equals(userId)) {
            throw new BusinessException("No permission to move this file");
        }

        if (request.getTargetParentId() != 0) {
            FileEntity targetFolder = fileMapper.selectById(request.getTargetParentId(), FileStatus.NORMAL.getCode());
            if (targetFolder == null || !Objects.equals(targetFolder.getFileType(), FileType.FOLDER.getCode())) {
                throw new BusinessException("Target folder not found");
            }
        }

        String lockKey = Constant.FILE_LOCK_PREFIX + userId + ":" + request.getTargetParentId() + ":" + file.getFileName();
        try (DistributedLock _ = new DistributedLock(redisTemplate, lockKey, Constant.LOCK_LEASE_TIME).lock()) {
            FileEntity existFile = fileMapper.selectByUserIdAndParentIdAndFileName(userId, request.getTargetParentId(), file.getFileName(), FileStatus.NORMAL.getCode());
            if (existFile != null) {
                throw new BusinessException("A file with the same name already exists in the target folder");
            }

            fileMapper.updateParentId(request.getFileId(), request.getTargetParentId());
            file.setParentId(request.getTargetParentId());

            log.info("文件移动成功: fileId={}, targetParentId={}", request.getFileId(), request.getTargetParentId());
            return convertToFileResponse(file);
        }
    }

    public File downloadFile(Long fileId) {
        Long userId = StpUtil.getLoginIdAsLong();

        FileEntity file = fileMapper.selectById(fileId, FileStatus.NORMAL.getCode());
        if (file == null) {
            throw new BusinessException("File not found");
        }

        if (!file.getUserId().equals(userId)) {
            throw new BusinessException("No permission to download this file");
        }

        if (Objects.equals(file.getFileType(), FileType.FOLDER.getCode())) {
            throw new BusinessException("Cannot download a folder");
        }

        Storage storage = storageMapper.selectById(file.getStorageId());
        if (storage == null) {
            throw new BusinessException("File storage info not found");
        }

        File physicalFile = new File(basePath, storage.getStoragePath());
        if (!physicalFile.exists()) {
            throw new BusinessException("Physical file not found");
        }

        log.info("文件下载: userId={}, fileName={}", userId, file.getFileName());
        return physicalFile;
    }

    public FileData getFileForPreview(Long fileId) {
        Long userId = StpUtil.getLoginIdAsLong();

        FileEntity file = fileMapper.selectById(fileId, FileStatus.NORMAL.getCode());
        if (file == null) {
            throw new BusinessException("File not found");
        }

        if (!file.getUserId().equals(userId)) {
            throw new BusinessException("No permission to preview this file");
        }

        if (Objects.equals(file.getFileType(), FileType.FOLDER.getCode())) {
            throw new BusinessException("Cannot preview a folder");
        }

        Storage storage = storageMapper.selectById(file.getStorageId());
        if (storage == null) {
            throw new BusinessException("File storage info not found");
        }

        File physicalFile = new File(basePath, storage.getStoragePath());
        if (!physicalFile.exists()) {
            throw new BusinessException("Physical file not found");
        }

        log.info("文件预览: userId={}, fileName={}", userId, file.getFileName());
        return new FileData(physicalFile, file.getFileName());
    }

    public FileResponse getFileInfo(Long fileId) {
        Long userId = StpUtil.getLoginIdAsLong();

        FileEntity file = fileMapper.selectById(fileId, FileStatus.NORMAL.getCode());
        if (file == null) {
            throw new BusinessException("File not found");
        }

        if (!file.getUserId().equals(userId)) {
            throw new BusinessException("No permission to access this file");
        }

        return convertToFileResponse(file);
    }

    @Transactional
    public void deleteFolder(Long folderId) {
        Long userId = StpUtil.getLoginIdAsLong();

        FileEntity folder = fileMapper.selectById(folderId, FileStatus.NORMAL.getCode());
        if (folder == null) {
            throw new BusinessException("Folder not found");
        }

        if (!folder.getUserId().equals(userId)) {
            throw new BusinessException("No permission to delete this folder");
        }

        if (!Objects.equals(folder.getFileType(), FileType.FOLDER.getCode())) {
            throw new BusinessException("Only folders can be deleted here");
        }

        String lockKey = Constant.FOLDER_LOCK_PREFIX + userId + ":" + folderId;
        try (DistributedLock _ = new DistributedLock(redisTemplate, lockKey, Constant.LOCK_LEASE_TIME).lock()) {
            if (isFolderNotEmptyRecursive(userId, folderId)) {
                throw new BusinessException("Can only delete empty folders or folders containing only empty folders");
            }

            deleteEmptyFoldersRecursive(userId, folderId);

            log.info("文件夹删除成功: folderId={}, folderName={}", folderId, folder.getFileName());
        }
    }

    private boolean isFolderNotEmptyRecursive(Long userId, Long folderId) {
        List<FileEntity> children = fileMapper.selectByUserIdAndParentId(userId, folderId, FileStatus.NORMAL.getCode());
        if (children.isEmpty()) {
            return false;
        }

        for (FileEntity child : children) {
            if (Objects.equals(child.getFileType(), FileType.FILE.getCode())) {
                // 包含文件，不是空文件夹
                return true;
            } else if (Objects.equals(child.getFileType(), FileType.FOLDER.getCode())) {
                // 递归检查子文件夹
                if (isFolderNotEmptyRecursive(userId, child.getFileId())) {
                    return true;
                }
            }
        }

        return false;
    }

    private void deleteEmptyFoldersRecursive(Long userId, Long folderId) {
        List<FileEntity> children = fileMapper.selectByUserIdAndParentId(userId, folderId, FileStatus.NORMAL.getCode());
        
        // 先递归删除子文件夹
        for (FileEntity child : children) {
            if (Objects.equals(child.getFileType(), FileType.FOLDER.getCode())) {
                deleteEmptyFoldersRecursive(userId, child.getFileId());
            }
        }

        // 删除当前文件夹
        fileMapper.deleteById(folderId);
    }

    private File saveToTempFile(MultipartFile multipartFile) throws IOException {
        Path tempDir = Paths.get(tempPath);
        if (!Files.exists(tempDir)) {
            Files.createDirectories(tempDir);
        }

        String tempFileName = System.currentTimeMillis() + "_" + multipartFile.getOriginalFilename();
        File tempFile = tempDir.resolve(tempFileName).toFile();
        multipartFile.transferTo(tempFile);
        return tempFile;
    }

    private FileEntity createFileRecord(Long userId, Long parentId, Long storageId, String fileName, Integer fileType, Long fileSize) {
        FileEntity file = new FileEntity();
        file.setUserId(userId);
        file.setParentId(parentId);
        file.setStorageId(storageId);
        file.setFileName(fileName);
        file.setFileType(fileType);
        file.setFileSize(fileSize);
        file.setStatus(FileStatus.NORMAL.getCode());
        fileMapper.insert(file);
        return file;
    }

    public CheckFileResponse checkFile(String identifier, String fileName, Long totalSize, Long parentId) {
        Long userId = StpUtil.getLoginIdAsLong();
        CheckFileResponse response = new CheckFileResponse();
        
        Storage existingStorage = storageMapper.selectByMd5(identifier);
        if (existingStorage != null) {
            response.setSkipUpload(true);
            response.setStorageId(existingStorage.getStorageId());
            response.setUploadedChunks(new ArrayList<>());
            return response;
        }
        
        response.setSkipUpload(false);
        response.setStorageId(null);
        List<Integer> uploadedChunks = fileChunkMapper.selectUploadedChunkNumbers(identifier);
        response.setUploadedChunks(uploadedChunks);
        
        return response;
    }

    @Transactional
    public void uploadChunk(MultipartFile file, String identifier, Integer chunkNumber, 
            Integer totalChunks, Long totalSize, Long parentId) throws IOException {
        Long userId = StpUtil.getLoginIdAsLong();
        
        FileChunk existingChunk = fileChunkMapper.selectByIdentifierAndChunkNumber(identifier, chunkNumber);
        if (existingChunk != null) {
            return;
        }
        
        UserService.UserQuota quota = userService.getUserQuota(userId);
        if (quota.getUsedQuota() + totalSize > quota.getTotalQuota()) {
            throw new BusinessException("Insufficient storage space");
        }
        
        Path chunkDir = Paths.get(tempPath, "chunks", identifier);
        Files.createDirectories(chunkDir);
        
        String chunkPath = chunkDir.resolve(String.valueOf(chunkNumber)).toString();
        file.transferTo(new File(chunkPath));
        
        FileChunk chunk = new FileChunk();
        chunk.setFileIdentifier(identifier);
        chunk.setChunkNumber(chunkNumber);
        chunk.setChunkSize(file.getSize());
        chunk.setTotalChunks(totalChunks);
        chunk.setTotalSize(totalSize);
        chunk.setStoragePath(chunkPath);
        chunk.setUserId(userId);
        fileChunkMapper.insert(chunk);
        
        log.info("Chunk uploaded: userId={}, identifier={}, chunkNumber={}", userId, identifier, chunkNumber);
    }

    @Transactional
    public FileResponse mergeFile(MergeFileRequest request) throws IOException {
        Long userId = StpUtil.getLoginIdAsLong();
        String identifier = request.getIdentifier();
        String fileName = request.getFileName();
        Long parentId = request.getParentId();
        
        FileEntity existFile = fileMapper.selectByUserIdAndParentIdAndFileName(userId, parentId, fileName, FileStatus.NORMAL.getCode());
        if (existFile != null) {
            throw new BusinessException("File already exists");
        }
        
        List<FileChunk> chunks = fileChunkMapper.selectByIdentifier(identifier);
        if (chunks.size() != request.getTotalChunks()) {
            throw new BusinessException("Not all chunks uploaded");
        }
        
        chunks.sort((a, b) -> a.getChunkNumber() - b.getChunkNumber());
        
        String extension = Funcs.getExtension(fileName);
        String storagePath = Funcs.generateStoragePath(identifier, extension);
        Path targetPath = Paths.get(basePath, storagePath);
        Files.createDirectories(targetPath.getParent());
        
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(targetPath.toFile())) {
            for (FileChunk chunk : chunks) {
                Files.copy(Paths.get(chunk.getStoragePath()), fos);
            }
        }
        
        Storage storage = new Storage();
        storage.setStorageSize(request.getTotalSize());
        storage.setStoragePath(storagePath);
        storage.setMd5(identifier);
        storage.setRefCount(1);
        storageMapper.insert(storage);
        
        FileEntity file = createFileRecord(userId, parentId, storage.getStorageId(), fileName, FileType.FILE.getCode(), request.getTotalSize());
        userService.incrementUsedQuota(userId, request.getTotalSize());
        
        cleanupChunks(identifier);
        
        log.info("File merged: userId={}, fileName={}, identifier={}", userId, fileName, identifier);
        return convertToFileResponse(file);
    }

    @Transactional
    public FileResponse instantUpload(MergeFileRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        String identifier = request.getIdentifier();
        String fileName = request.getFileName();
        Long parentId = request.getParentId();
        Long storageId = request.getStorageId();
        
        FileEntity existFile = fileMapper.selectByUserIdAndParentIdAndFileName(userId, parentId, fileName, FileStatus.NORMAL.getCode());
        if (existFile != null) {
            throw new BusinessException("File already exists");
        }
        
        Storage storage = storageMapper.selectById(storageId);
        if (storage == null) {
            throw new BusinessException("Storage not found");
        }
        
        storageMapper.incrementRefCount(storageId);
        
        FileEntity file = createFileRecord(userId, parentId, storageId, fileName, FileType.FILE.getCode(), request.getTotalSize());
        userService.incrementUsedQuota(userId, request.getTotalSize());
        
        log.info("Instant upload: userId={}, fileName={}, storageId={}", userId, fileName, storageId);
        return convertToFileResponse(file);
    }

    public Long getStorageId(Long fileId) {
        return fileMapper.selectStorageId(fileId);
    }

    private void cleanupChunks(String identifier) {
        List<FileChunk> chunks = fileChunkMapper.selectByIdentifier(identifier);
        for (FileChunk chunk : chunks) {
            try {
                Files.deleteIfExists(Paths.get(chunk.getStoragePath()));
            } catch (IOException e) {
                log.warn("Failed to delete chunk: {}", chunk.getStoragePath());
            }
        }
        fileChunkMapper.deleteByIdentifier(identifier);
    }

    private FileResponse convertToFileResponse(FileEntity file) {
        FileResponse response = new FileResponse();
        response.setFileId(file.getFileId());
        response.setParentId(file.getParentId());
        response.setFileName(file.getFileName());
        response.setFileType(file.getFileType());
        response.setFileSize(file.getFileSize());
        response.setFileSizeFormat(Funcs.formatFileSize(file.getFileSize()));
        response.setStatus(file.getStatus());
        response.setCreatedTime(file.getCreatedTime());
        response.setUpdatedTime(file.getUpdatedTime());
        return response;
    }

    public FilePathResponse getFilePath(Long fileId) {
        Long userId = StpUtil.getLoginIdAsLong();

        FileEntity file = fileMapper.selectById(fileId, FileStatus.NORMAL.getCode());
        if (file == null) {
            throw new BusinessException("File not found");
        }

        if (!file.getUserId().equals(userId)) {
            throw new BusinessException("No permission to access this file");
        }

        List<Long> pathIds = new ArrayList<>();
        pathIds.add(fileId);

        Long parentId = file.getParentId();
        while (parentId != null && parentId != 0) {
            pathIds.add(0, parentId);
            FileEntity parent = fileMapper.selectById(parentId, FileStatus.NORMAL.getCode());
            if (parent == null) break;
            parentId = parent.getParentId();
        }

        return new FilePathResponse(fileId, pathIds);
    }

    @Transactional
    public FileResponse createTextFile(Long userId, Long parentId, String fileName, String content) {
        FileEntity existFile = fileMapper.selectByUserIdAndParentIdAndFileName(userId, parentId, fileName, FileStatus.NORMAL.getCode());
        if (existFile != null) {
            throw new BusinessException("File already exists");
        }

        UserService.UserQuota quota = userService.getUserQuota(userId);
        long fileSize = content.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
        if (quota.getUsedQuota() + fileSize > quota.getTotalQuota()) {
            throw new BusinessException("Insufficient storage space");
        }

        String md5 = org.apache.commons.codec.digest.DigestUtils.md5Hex(content);
        Storage storage = storageMapper.selectByMd5(md5);
        
        if (storage == null) {
            String extension = Funcs.getExtension(fileName);
            String storagePath = Funcs.generateStoragePath(md5, extension);
            Path targetPath = Paths.get(basePath, storagePath);
            
            try {
                Files.createDirectories(targetPath.getParent());
                Files.writeString(targetPath, content, java.nio.charset.StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new BusinessException("Failed to write file: " + e.getMessage());
            }

            storage = new Storage();
            storage.setStorageSize(fileSize);
            storage.setStoragePath(storagePath);
            storage.setMd5(md5);
            storage.setRefCount(1);
            storageMapper.insert(storage);
        } else {
            storageMapper.incrementRefCount(storage.getStorageId());
        }

        FileEntity file = new FileEntity();
        file.setUserId(userId);
        file.setParentId(parentId);
        file.setStorageId(storage.getStorageId());
        file.setFileName(fileName);
        file.setFileType(FileType.FILE.getCode());
        file.setFileSize(fileSize);
        file.setStatus(FileStatus.NORMAL.getCode());
        fileMapper.insert(file);

        userService.incrementUsedQuota(userId, fileSize);

        log.info("Text file created: userId={}, fileName={}, size={}", userId, fileName, fileSize);
        return convertToFileResponse(file);
    }

    public List<FileResponse> listFilesInFolder(Long userId, Long parentId) {
        List<FileEntity> files = fileMapper.selectByUserIdAndParentId(userId, parentId, FileStatus.NORMAL.getCode());
        return files.stream()
                .map(this::convertToFileResponse)
                .toList();
    }

    public String readFileContent(Long userId, Long fileId) {
        FileEntity file = fileMapper.selectById(fileId, FileStatus.NORMAL.getCode());
        if (file == null) {
            throw new BusinessException("File not found");
        }
        if (!file.getUserId().equals(userId)) {
            throw new BusinessException("No permission to access this file");
        }
        if (Objects.equals(file.getFileType(), FileType.FOLDER.getCode())) {
            throw new BusinessException("Cannot read a folder");
        }

        Storage storage = storageMapper.selectById(file.getStorageId());
        if (storage == null) {
            throw new BusinessException("File storage info not found");
        }

        File physicalFile = new File(basePath, storage.getStoragePath());
        if (!physicalFile.exists()) {
            throw new BusinessException("Physical file not found");
        }

        String fileName = file.getFileName().toLowerCase();
        if (!isTextFile(fileName)) {
            throw new BusinessException("Only text files can be read");
        }

        try {
            return Files.readString(physicalFile.toPath(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new BusinessException("Failed to read file: " + e.getMessage());
        }
    }

    private boolean isTextFile(String fileName) {
        String ext = fileName.contains(".") ? fileName.substring(fileName.lastIndexOf(".") + 1) : "";
        return List.of("txt", "md", "json", "xml", "csv", "html", "htm", "css", "js", "ts", 
                "java", "py", "go", "rs", "c", "cpp", "h", "cs", "php", "rb", "swift", "kt",
                "scala", "sh", "bat", "sql", "yaml", "yml", "ini", "conf", "cfg", "properties",
                "vue", "jsx", "tsx", "scss", "sass", "less", "log").contains(ext);
    }

    public record FilePathResponse(Long fileId, List<Long> pathIds) {}
}

