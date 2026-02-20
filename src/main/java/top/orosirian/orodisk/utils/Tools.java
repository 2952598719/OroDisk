package top.orosirian.orodisk.utils;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;
import top.orosirian.orodisk.mappers.FileMapper;
import top.orosirian.orodisk.model.entity.FileEntity;
import top.orosirian.orodisk.model.response.FileResponse;
import top.orosirian.orodisk.model.response.SearchResult;
import top.orosirian.orodisk.service.FileService;
import top.orosirian.orodisk.utils.enums.FileStatus;

import java.util.ArrayList;
import java.util.List;

@Component
public class Tools {

    private final VectorStore vectorStore;
    private final FileMapper fileMapper;
    private final FileService fileService;

    public Tools(VectorStore vectorStore, FileMapper fileMapper, FileService fileService) {
        this.vectorStore = vectorStore;
        this.fileMapper = fileMapper;
        this.fileService = fileService;
    }

    @Tool(description = "检索具有用户描述内容的文件。返回结果后，请使用 [文件名](file://文件ID) 格式展示每个文件，例如：[报告.docx](file://123)")
    public List<SearchResult> getFiles(
            @ToolParam(description = "用户描述的内容关键词") String query,
            @ToolParam(description = "检索返回的最大记录数") int topK,
            ToolContext toolContext) {

        Long userId = (Long) toolContext.getContext().get("userId");
        
        List<Long> storageIds = fileMapper.selectStorageIdsByUserId(userId, FileStatus.NORMAL.getCode());
        
        if (storageIds.isEmpty()) {
            return new ArrayList<>();
        }

        String filterExpression = buildStorageIdFilter(storageIds);

        List<Document> documents = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(topK)
                        .filterExpression(filterExpression)
                        .build()
        );

        List<SearchResult> results = new ArrayList<>();

        for (Document doc : documents) {
            Object storageIdObj = doc.getMetadata().get("storageId");
            if (storageIdObj == null) continue;
            
            Long storageId = storageIdObj instanceof Long 
                    ? (Long) storageIdObj 
                    : Long.parseLong(storageIdObj.toString());

            List<FileEntity> fileEntities = fileMapper.selectByStorageId(storageId);
            for (FileEntity file : fileEntities) {
                if (!file.getUserId().equals(userId)) {
                    continue;
                }
                
                String content = doc.getText();
                String snippet = content.length() > 200
                        ? content.substring(0, 200) + "..."
                        : content;

                results.add(new SearchResult(
                        file.getFileId(),
                        file.getFileName(),
                        snippet
                ));
            }
        }

        return results;
    }

    @Tool(description = "创建文本文件并写入内容。成功后返回文件信息，使用 [文件名](file://文件ID) 格式展示。")
    public FileResponse writeFile(
            @ToolParam(description = "文件名，需要包含扩展名，如 report.txt 或 notes.md") String fileName,
            @ToolParam(description = "文件内容") String content,
            @ToolParam(description = "父文件夹ID，根目录为0", required = false) Long parentId,
            ToolContext toolContext) {

        Long userId = (Long) toolContext.getContext().get("userId");
        
        if (parentId == null) {
            parentId = 0L;
        }

        return fileService.createTextFile(userId, parentId, fileName, content);
    }

    @Tool(description = "列出指定文件夹下的所有文件和子文件夹。不传folderId时默认列出当前用户所在文件夹。返回文件列表，每个文件包含ID、名称、类型(0=文件夹,1=文件)、大小。")
    public List<FileResponse> listFilesInFolder(
            @ToolParam(description = "文件夹ID，不传则使用当前所在文件夹", required = false) Long folderId,
            ToolContext toolContext) {

        Long userId = (Long) toolContext.getContext().get("userId");
        Long currentFolderId = (Long) toolContext.getContext().get("currentFolderId");
        
        if (folderId == null) {
            folderId = currentFolderId != null ? currentFolderId : 0L;
        }

        return fileService.listFilesInFolder(userId, folderId);
    }

    @Tool(description = "读取文本文件的内容。只能读取文本格式的文件（如txt、md、json、代码文件等）。返回文件内容字符串。")
    public String readFileContent(
            @ToolParam(description = "要读取的文件ID") Long fileId,
            ToolContext toolContext) {

        Long userId = (Long) toolContext.getContext().get("userId");

        return fileService.readFileContent(userId, fileId);
    }

    private String buildStorageIdFilter(List<Long> storageIds) {
        if (storageIds.size() == 1) {
            return "storageId == " + "\"" + storageIds.getFirst() + "\"";
        }
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < storageIds.size(); i++) {
            if (i > 0) {
                sb.append(" || ");
            }
            sb.append("storageId == ").append("\"").append(storageIds.get(i)).append("\"");
        }
        return sb.toString();
    }
}
