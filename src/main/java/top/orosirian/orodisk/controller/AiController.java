package top.orosirian.orodisk.controller;

import cn.dev33.satoken.stp.StpUtil;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import top.orosirian.orodisk.model.Result;
import top.orosirian.orodisk.model.entity.AiConversation;
import top.orosirian.orodisk.model.request.CreateConversationRequest;
import top.orosirian.orodisk.model.response.ConversationListResponse;
import top.orosirian.orodisk.model.response.ConversationResponse;
import top.orosirian.orodisk.service.AiService;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/ai")
public class AiController {

    private final AiService aiService;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/conversation")
    public Result<ConversationResponse> createConversation(@RequestBody CreateConversationRequest request) {
        return Result.success(aiService.createConversation(request));
    }

    @GetMapping("/conversations")
    public Result<List<ConversationListResponse>> listConversations() {
        return Result.success(aiService.listConversations());
    }

    @GetMapping("/conversation/{conversationId}")
    public Result<ConversationResponse> getConversation(@PathVariable Long conversationId) {
        return Result.success(aiService.getConversation(conversationId));
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMessage(
            @RequestParam Long conversationId,
            @RequestParam String content,
            @RequestParam(required = false) List<Long> contextFileIds,
            @RequestParam(defaultValue = "0") Long currentFolderId) {

        Long userId = StpUtil.getLoginIdAsLong();
        SseEmitter emitter = new SseEmitter(60000L);

        AiConversation conversation = aiService.validateConversation(conversationId);
        
        aiService.saveUserMessage(conversationId, content);
        
        if (conversation.getTitle() == null || conversation.getTitle().equals("New Chat")) {
            String title = content.length() <= 30 ? content : content.substring(0, 30) + "...";
            aiService.updateConversationTitle(conversationId, title);
        }

        final List<Long> fileIds = contextFileIds != null ? contextFileIds : List.of();

        executor.execute(() -> {
            try {
                aiService.streamChat(
                        conversationId,
                        userId,
                        currentFolderId,
                        fileIds,
                        chunk -> {
                            try {
                                emitter.send(SseEmitter.event()
                                        .name("message")
                                        .data(chunk));
                            } catch (IOException e) {
                                emitter.completeWithError(e);
                            }
                        },
                        () -> {
                            try {
                                emitter.send(SseEmitter.event()
                                        .name("done")
                                        .data("[DONE]"));
                                emitter.complete();
                            } catch (IOException e) {
                                emitter.completeWithError(e);
                            }
                        },
                        error -> {
                            try {
                                emitter.send(SseEmitter.event()
                                        .name("error")
                                        .data(error.getMessage()));
                                emitter.completeWithError(error);
                            } catch (IOException e) {
                                emitter.completeWithError(e);
                            }
                        }
                );
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data(e.getMessage()));
                } catch (IOException ex) {
                    // ignore
                }
                emitter.completeWithError(e);
            }
        });

        emitter.onCompletion(() -> {});
        emitter.onTimeout(emitter::complete);
        emitter.onError(e -> {});

        return emitter;
    }

    @DeleteMapping("/conversation/{conversationId}")
    public Result<Void> deleteConversation(@PathVariable Long conversationId) {
        aiService.deleteConversation(conversationId);
        return Result.success();
    }

}
