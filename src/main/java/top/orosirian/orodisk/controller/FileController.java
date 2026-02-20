package top.orosirian.orodisk.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import top.orosirian.orodisk.model.Result;
import top.orosirian.orodisk.model.request.CreateFolderRequest;
import top.orosirian.orodisk.model.request.MergeFileRequest;
import top.orosirian.orodisk.model.request.MoveFileRequest;
import top.orosirian.orodisk.model.request.RenameFileRequest;
import top.orosirian.orodisk.model.response.CheckFileResponse;
import top.orosirian.orodisk.model.response.FileListResponse;
import top.orosirian.orodisk.model.response.FileResponse;
import top.orosirian.orodisk.service.FileService;
import top.orosirian.orodisk.service.VectorQueueService;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/file")
public class FileController {

    private final FileService fileService;
    private final VectorQueueService vectorQueueService;
    public FileController(FileService fileService, VectorQueueService vectorQueueService) {
        this.fileService = fileService;
        this.vectorQueueService = vectorQueueService;
    }

    @SaCheckLogin
    @GetMapping("/list")
    public Result<FileListResponse> listFiles(
            @RequestParam(defaultValue = "0") Long parentId,
            @RequestParam(required = false) Long lastFileId,
            @RequestParam(required = false) Integer pageSize) {
        return Result.success(fileService.listFiles(parentId, lastFileId, pageSize));
    }

    @SaCheckLogin
    @PostMapping("/folder")
    public Result<FileResponse> createFolder(@RequestBody CreateFolderRequest request) {
        return Result.success(fileService.createFolder(request));
    }

    @SaCheckLogin
    @GetMapping("/check")
    public Result<CheckFileResponse> checkFile(
            @RequestParam String identifier,
            @RequestParam String fileName,
            @RequestParam Long totalSize,
            @RequestParam(defaultValue = "0") Long parentId) {
        return Result.success(fileService.checkFile(identifier, fileName, totalSize, parentId));
    }

    @SaCheckLogin
    @PostMapping("/chunk")
    public Result<Void> uploadChunk(
            @RequestParam("file") MultipartFile file,
            @RequestParam String identifier,
            @RequestParam Integer chunkNumber,
            @RequestParam Integer totalChunks,
            @RequestParam Long totalSize,
            @RequestParam(defaultValue = "0") Long parentId) throws IOException {
        fileService.uploadChunk(file, identifier, chunkNumber, totalChunks, totalSize, parentId);
        return Result.success();
    }

    @SaCheckLogin
    @PostMapping("/merge")
    public Result<FileResponse> mergeFile(@RequestBody MergeFileRequest request) throws IOException {
        FileResponse response = fileService.mergeFile(request);
        Long storageId = fileService.getStorageId(response.getFileId());
        vectorQueueService.sendVectorizeMessage(storageId);
        return Result.success(response);
    }

    @SaCheckLogin
    @PostMapping("/instant")
    public Result<FileResponse> instantUpload(@RequestBody MergeFileRequest request) {
        return Result.success(fileService.instantUpload(request));
    }

    @SaCheckLogin
    @PostMapping("/upload")
    public Result<FileResponse> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "0") Long parentId,
            @RequestParam("md5") String md5) throws IOException {
        FileResponse response = fileService.uploadFile(file, parentId, md5);
        Long storageId = fileService.getStorageId(response.getFileId());
        vectorQueueService.sendVectorizeMessage(storageId);
        return Result.success(response);
    }

    @SaCheckLogin
    @DeleteMapping("/{fileId}")
    public Result<Void> deleteFile(@PathVariable Long fileId) {
        fileService.deleteFile(fileId);
        return Result.success();
    }

    @SaCheckLogin
    @DeleteMapping("/permanent/{fileId}")
    public Result<Void> deletePermanently(@PathVariable Long fileId) {
        fileService.deletePermanently(fileId);
        return Result.success();
    }

    @SaCheckLogin
    @GetMapping("/recycle")
    public Result<FileListResponse> listRecycle(
            @RequestParam(required = false) Long lastFileId,
            @RequestParam(required = false) Integer pageSize) {
        return Result.success(fileService.listRecycle(lastFileId, pageSize));
    }

    @SaCheckLogin
    @PutMapping("/restore/{fileId}")
    public Result<Void> restoreFile(@PathVariable Long fileId) {
        fileService.restoreFile(fileId);
        return Result.success();
    }

    @SaCheckLogin
    @DeleteMapping("/recycle/clear")
    public Result<Void> clearRecycle() {
        fileService.clearRecycle();
        return Result.success();
    }

    @SaCheckLogin
    @PutMapping("/rename")
    public Result<FileResponse> renameFile(@RequestBody RenameFileRequest request) {
        return Result.success(fileService.renameFile(request));
    }

    @SaCheckLogin
    @PutMapping("/move")
    public Result<FileResponse> moveFile(@RequestBody MoveFileRequest request) {
        return Result.success(fileService.moveFile(request));
    }

    @SaCheckLogin
    @GetMapping("/download/{fileId}")
    public void downloadFile(@PathVariable Long fileId, HttpServletResponse response) throws IOException {
        java.io.File file = fileService.downloadFile(fileId);
        String fileName = file.getName();

        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=" +
                URLEncoder.encode(fileName, StandardCharsets.UTF_8));
        response.setHeader("Content-Length", String.valueOf(file.length()));

        try (InputStream is = new FileInputStream(file);
             OutputStream os = response.getOutputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            os.flush();
        }
    }

    @SaCheckLogin
    @GetMapping("/preview/{fileId}")
    public void previewFile(@PathVariable Long fileId, HttpServletResponse response) throws IOException {
        var fileData = fileService.getFileForPreview(fileId);
        java.io.File file = fileData.getFile();
        String fileName = fileData.getFileName();
        String contentType = getContentType(fileName);
        
        response.setContentType(contentType);
        response.setHeader("Content-Disposition", "inline; filename=" +
                URLEncoder.encode(fileName, StandardCharsets.UTF_8));
        response.setHeader("Content-Length", String.valueOf(file.length()));

        try (InputStream is = new FileInputStream(file);
             OutputStream os = response.getOutputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            os.flush();
        }
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
            case "ico" -> "image/x-icon";
            case "txt", "log", "md", "csv", "ini", "conf", "cfg", "properties" -> "text/plain";
            case "html", "htm" -> "text/html";
            case "css" -> "text/css";
            case "js" -> "application/javascript";
            case "json" -> "application/json";
            case "xml" -> "application/xml";
            case "yaml", "yml" -> "application/x-yaml";
            default -> "application/octet-stream";
        };
    }

    @SaCheckLogin
    @GetMapping("/info/{fileId}")
    public Result<FileResponse> getFileInfo(@PathVariable Long fileId) {
        return Result.success(fileService.getFileInfo(fileId));
    }

    @SaCheckLogin
    @DeleteMapping("/folder/{folderId}")
    public Result<Void> deleteFolder(@PathVariable Long folderId) {
        fileService.deleteFolder(folderId);
        return Result.success();
    }

    @SaCheckLogin
    @GetMapping("/path/{fileId}")
    public Result<FileService.FilePathResponse> getFilePath(@PathVariable Long fileId) {
        return Result.success(fileService.getFilePath(fileId));
    }
}