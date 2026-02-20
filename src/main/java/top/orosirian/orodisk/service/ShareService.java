package top.orosirian.orodisk.service;

import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.orosirian.orodisk.mappers.FileMapper;
import top.orosirian.orodisk.mappers.FileShareMapper;
import top.orosirian.orodisk.mappers.StorageMapper;
import top.orosirian.orodisk.model.entity.FileEntity;
import top.orosirian.orodisk.model.entity.FileShare;
import top.orosirian.orodisk.model.entity.Storage;
import top.orosirian.orodisk.model.request.CreateShareRequest;
import top.orosirian.orodisk.model.request.VerifyShareRequest;
import top.orosirian.orodisk.model.response.ShareInfoResponse;
import top.orosirian.orodisk.model.response.ShareResponse;
import top.orosirian.orodisk.utils.Funcs;
import top.orosirian.orodisk.utils.exceptions.BusinessException;
import top.orosirian.orodisk.utils.enums.FileStatus;
import top.orosirian.orodisk.utils.enums.FileType;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Slf4j
@Service
public class ShareService {

    @Value("${disk.share.base-url:http://www.orosirian.top/s/}")
    private String shareBaseUrl;

    @Value("${disk.storage.base-path}")
    private String basePath;

    private final FileShareMapper fileShareMapper;
    private final FileMapper fileMapper;
    private final StorageMapper storageMapper;

    public ShareService(FileShareMapper fileShareMapper, FileMapper fileMapper, StorageMapper storageMapper) {
        this.fileShareMapper = fileShareMapper;
        this.fileMapper = fileMapper;
        this.storageMapper = storageMapper;
    }

    @Transactional
    public ShareResponse createShare(CreateShareRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();

        FileEntity file = fileMapper.selectById(request.getFileId(), FileStatus.NORMAL.getCode());
        if (file == null) {
            throw new BusinessException("File not found");
        }

        if (!file.getUserId().equals(userId)) {
            throw new BusinessException("No permission to share this file");
        }

        if (file.getFileType().equals(FileType.FOLDER.getCode())) {
            throw new BusinessException("Cannot share a folder");
        }

        String shareCode = generateShareCode();

        FileShare share = new FileShare();
        share.setShareCode(shareCode);
        share.setFileId(request.getFileId());
        share.setUserId(userId);
        share.setPassword(request.getPassword());
        share.setExpireTime(request.getExpireTime());
        share.setMaxDownloads(request.getMaxDownloads() != null ? request.getMaxDownloads() : -1);
        share.setCurrentDownloads(0);
        share.setAllowDownload(request.getAllowDownload() != null ? request.getAllowDownload() : true);
        share.setStatus(0);

        fileShareMapper.insert(share);

        log.info("Share created: userId={}, fileId={}, shareCode={}", userId, request.getFileId(), shareCode);

        return convertToShareResponse(share, file.getFileName());
    }

    public ShareInfoResponse getShareInfo(String shareCode) {
        FileShare share = fileShareMapper.selectByShareCode(shareCode);
        if (share == null) {
            throw new BusinessException("Share not found or has been cancelled");
        }

        validateShare(share);

        FileEntity file = fileMapper.selectById(share.getFileId(), FileStatus.NORMAL.getCode());
        if (file == null) {
            throw new BusinessException("The shared file has been deleted");
        }

        ShareInfoResponse response = new ShareInfoResponse();
        response.setShareCode(share.getShareCode());
        response.setFileName(file.getFileName());
        response.setFileSize(file.getFileSize());
        response.setFileSizeFormat(Funcs.formatFileSize(file.getFileSize()));
        response.setFileType(file.getFileType());
        response.setHasPassword(share.getPassword() != null && !share.getPassword().isEmpty());
        response.setAllowDownload(share.getAllowDownload());
        response.setExpireTime(share.getExpireTime());
        response.setMaxDownloads(share.getMaxDownloads());
        response.setCurrentDownloads(share.getCurrentDownloads());

        return response;
    }

    public boolean verifyPassword(VerifyShareRequest request) {
        FileShare share = fileShareMapper.selectByShareCode(request.getShareCode());
        if (share == null) {
            throw new BusinessException("Share not found");
        }

        if (share.getPassword() == null || share.getPassword().isEmpty()) {
            return true;
        }

        return share.getPassword().equals(request.getPassword());
    }

    public void downloadShare(String shareCode, String password, HttpServletResponse response) throws Exception {
        FileShare share = fileShareMapper.selectByShareCode(shareCode);
        if (share == null) {
            throw new BusinessException("Share not found");
        }

        validateShare(share);

        if (share.getPassword() != null && !share.getPassword().isEmpty()) {
            if (!share.getPassword().equals(password)) {
                throw new BusinessException("Incorrect password");
            }
        }

        if (!share.getAllowDownload()) {
            throw new BusinessException("Download not allowed");
        }

        FileEntity file = fileMapper.selectById(share.getFileId(), FileStatus.NORMAL.getCode());
        if (file == null) {
            throw new BusinessException("File not found");
        }

        Storage storage = storageMapper.selectById(file.getStorageId());
        if (storage == null) {
            throw new BusinessException("Storage not found");
        }

        File physicalFile = new File(basePath, storage.getStoragePath());
        if (!physicalFile.exists()) {
            throw new BusinessException("Physical file not found");
        }

        String contentType = getContentType(file.getFileName());
        response.setContentType(contentType);
        response.setHeader("Content-Disposition", "attachment; filename=" +
                URLEncoder.encode(file.getFileName(), StandardCharsets.UTF_8));
        response.setHeader("Content-Length", String.valueOf(physicalFile.length()));

        try (InputStream is = new FileInputStream(physicalFile);
             OutputStream os = response.getOutputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            os.flush();
        }

        fileShareMapper.incrementDownloads(share.getShareId());
        log.info("Share downloaded: shareCode={}, fileId={}", shareCode, file.getFileId());
    }

    public void previewShare(String shareCode, String password, HttpServletResponse response) throws Exception {
        FileShare share = fileShareMapper.selectByShareCode(shareCode);
        if (share == null) {
            throw new BusinessException("Share not found");
        }

        validateShare(share);

        if (share.getPassword() != null && !share.getPassword().isEmpty()) {
            if (!share.getPassword().equals(password)) {
                throw new BusinessException("Incorrect password");
            }
        }

        FileEntity file = fileMapper.selectById(share.getFileId(), FileStatus.NORMAL.getCode());
        if (file == null) {
            throw new BusinessException("File not found");
        }

        Storage storage = storageMapper.selectById(file.getStorageId());
        if (storage == null) {
            throw new BusinessException("Storage not found");
        }

        File physicalFile = new File(basePath, storage.getStoragePath());
        if (!physicalFile.exists()) {
            throw new BusinessException("Physical file not found");
        }

        String contentType = getContentType(file.getFileName());
        response.setContentType(contentType);
        response.setHeader("Content-Disposition", "inline; filename=" +
                URLEncoder.encode(file.getFileName(), StandardCharsets.UTF_8));
        response.setHeader("Content-Length", String.valueOf(physicalFile.length()));

        try (InputStream is = new FileInputStream(physicalFile);
             OutputStream os = response.getOutputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            os.flush();
        }
    }

    public List<ShareResponse> listMyShares() {
        Long userId = StpUtil.getLoginIdAsLong();
        List<FileShare> shares = fileShareMapper.selectByUserId(userId);

        return shares.stream().map(share -> {
            FileEntity file = fileMapper.selectById(share.getFileId(), FileStatus.NORMAL.getCode());
            String fileName = file != null ? file.getFileName() : "Unknown";
            return convertToShareResponse(share, fileName);
        }).toList();
    }

    @Transactional
    public void cancelShare(Long shareId) {
        Long userId = StpUtil.getLoginIdAsLong();

        FileShare share = fileShareMapper.selectById(shareId);
        if (share == null) {
            throw new BusinessException("Share not found");
        }

        if (!share.getUserId().equals(userId)) {
            throw new BusinessException("No permission to cancel this share");
        }

        fileShareMapper.updateStatus(shareId, 1);
        log.info("Share cancelled: userId={}, shareId={}", userId, shareId);
    }

    private void validateShare(FileShare share) {
        if (share.getStatus() == 1) {
            throw new BusinessException("Share has been cancelled");
        }

        if (share.getExpireTime() != null && share.getExpireTime().isBefore(LocalDateTime.now())) {
            fileShareMapper.updateStatus(share.getShareId(), 2);
            throw new BusinessException("Share has expired");
        }

        if (share.getMaxDownloads() > 0 && share.getCurrentDownloads() >= share.getMaxDownloads()) {
            throw new BusinessException("Download limit reached");
        }
    }

    private String generateShareCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private ShareResponse convertToShareResponse(FileShare share, String fileName) {
        ShareResponse response = new ShareResponse();
        response.setShareId(share.getShareId());
        response.setShareCode(share.getShareCode());
        response.setShareUrl(shareBaseUrl + share.getShareCode());
        response.setFileId(share.getFileId());
        response.setFileName(fileName);
        response.setHasPassword(share.getPassword() != null && !share.getPassword().isEmpty());
        response.setExpireTime(share.getExpireTime());
        response.setMaxDownloads(share.getMaxDownloads());
        response.setCurrentDownloads(share.getCurrentDownloads());
        response.setAllowDownload(share.getAllowDownload());
        response.setStatus(share.getStatus());
        response.setCreatedTime(share.getCreatedTime());
        return response;
    }

    private String getContentType(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "application/octet-stream";
        }
        String ext = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        return switch (ext) {
            case "pdf" -> "application/pdf";
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "bmp" -> "image/bmp";
            case "webp" -> "image/webp";
            case "svg" -> "image/svg+xml";
            case "txt", "log", "md", "csv" -> "text/plain";
            case "html", "htm" -> "text/html";
            case "css" -> "text/css";
            case "js" -> "application/javascript";
            case "json" -> "application/json";
            case "xml" -> "application/xml";
            default -> "application/octet-stream";
        };
    }

}
