package top.orosirian.orodisk.service;

import cn.dev33.satoken.stp.StpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.orosirian.orodisk.mappers.AiConversationMapper;
import top.orosirian.orodisk.mappers.AiMessageMapper;
import top.orosirian.orodisk.model.entity.AiConversation;
import top.orosirian.orodisk.model.entity.AiMessage;
import top.orosirian.orodisk.model.request.CreateConversationRequest;
import top.orosirian.orodisk.model.response.ConversationListResponse;
import top.orosirian.orodisk.model.response.ConversationResponse;
import top.orosirian.orodisk.model.response.MessageResponse;
import top.orosirian.orodisk.utils.Tools;
import top.orosirian.orodisk.utils.exceptions.BusinessException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
@Service
public class AiService {

    private final AiConversationMapper conversationMapper;
    private final AiMessageMapper messageMapper;
    private final ChatClient chatClient;
    private final DocumentVectorService documentVectorService;
    private final Tools tools;

    public AiService(AiConversationMapper conversationMapper, AiMessageMapper messageMapper,
                     ChatClient.Builder chatClientBuilder, DocumentVectorService documentVectorService, Tools tools) {
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
        this.chatClient = chatClientBuilder.build();
        this.documentVectorService = documentVectorService;
        this.tools = tools;
    }

    @Transactional
    public ConversationResponse createConversation(CreateConversationRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();

        AiConversation conversation = new AiConversation();
        conversation.setUserId(userId);
        conversation.setTitle(request.getTitle() != null ? request.getTitle() : "New Chat");
        conversation.setContextType(request.getContextType());
        conversation.setContextId(request.getContextId());
        conversation.setModelName("glm-5");
        conversation.setStatus(0);

        conversationMapper.insert(conversation);
        log.info("AI conversation created: userId={}, conversationId={}", userId, conversation.getConversationId());

        return convertToConversationResponse(conversation, new ArrayList<>());
    }

    public List<ConversationListResponse> listConversations() {
        Long userId = StpUtil.getLoginIdAsLong();
        List<AiConversation> conversations = conversationMapper.selectByUserId(userId, 0);

        return conversations.stream()
                .map(this::convertToConversationListResponse)
                .toList();
    }

    public ConversationResponse getConversation(Long conversationId) {
        Long userId = StpUtil.getLoginIdAsLong();

        AiConversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null) {
            throw new BusinessException("Conversation not found");
        }

        if (!conversation.getUserId().equals(userId)) {
            throw new BusinessException("No permission to access this conversation");
        }

        List<AiMessage> messages = messageMapper.selectByConversationId(conversationId, null);
        return convertToConversationResponse(conversation, messages);
    }

    public AiConversation validateConversation(Long conversationId) {
        Long userId = StpUtil.getLoginIdAsLong();
        AiConversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null) {
            throw new BusinessException("Conversation not found");
        }
        if (!conversation.getUserId().equals(userId)) {
            throw new BusinessException("No permission to access this conversation");
        }
        return conversation;
    }

    public void saveUserMessage(Long conversationId, String content) {
        AiMessage userMessage = new AiMessage();
        userMessage.setConversationId(conversationId);
        userMessage.setRole("user");
        userMessage.setContent(content);
        messageMapper.insert(userMessage);
    }

    public void saveAssistantMessage(Long conversationId, String content) {
        AiMessage assistantMessage = new AiMessage();
        assistantMessage.setConversationId(conversationId);
        assistantMessage.setRole("assistant");
        assistantMessage.setContent(content);
        messageMapper.insert(assistantMessage);
    }

    public void updateConversationTitle(Long conversationId, String title) {
        conversationMapper.updateTitle(conversationId, title);
    }

    public void streamChat(Long conversationId, Long userId, Long currentFolderId, List<Long> contextFileIds, Consumer<String> onChunk, Runnable onComplete, Consumer<Throwable> onError) {
        List<AiMessage> historyMessages = messageMapper.selectByConversationId(conversationId, null);
        String systemPrompt = "你是一个有帮助的AI助手";
        if (contextFileIds != null && !contextFileIds.isEmpty()) {
            String query = historyMessages.getLast().getContent();
            List<Document> documents = documentVectorService.searchByFileIds(query, contextFileIds, 5);
            String ragContext = "";
            if (!documents.isEmpty()) {
                StringBuilder context = new StringBuilder();
                for (int i = 0; i < documents.size(); i++) {
                    Document doc = documents.get(i);
                    context.append("【文档片段 ").append(i + 1).append("】\n");
                    context.append(doc.getText()).append("\n\n");
                }
                ragContext = context.toString();
            }
            if (!ragContext.isEmpty()) {
                systemPrompt = "你是一个有帮助的AI助手。以下是一些参考文档内容，如果用户问到相关内容，请将其作为参考回答用户问题：\n\n" + ragContext;
            }
        }
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemPrompt));
        for (AiMessage aiMessage : historyMessages) {
            if (aiMessage.getRole().equals("user")) {
                messages.add(new UserMessage(aiMessage.getContent()));
            } else if (aiMessage.getRole().equals("assistant")) {
                messages.add(new AssistantMessage(aiMessage.getContent()));
            }
        }
        StringBuilder fullResponse = new StringBuilder();
        Map<String, Object> toolContext = new HashMap<>();
        toolContext.put("userId", userId);
        toolContext.put("currentFolderId", currentFolderId);
        chatClient.prompt(new Prompt(messages))
                .tools(tools)
                .toolContext(toolContext)
                .stream()
                .chatResponse()
                .doOnNext(response -> {
                    String text = response.getResult().getOutput().getText();
                    if (text != null && !text.isEmpty()) {
                        fullResponse.append(text);
                        onChunk.accept(text);
                    }
                })
                .doOnComplete(() -> {
                    String fullContent = fullResponse.toString();
                    saveAssistantMessage(conversationId, fullContent);
                    onComplete.run();
                    log.info("AI stream completed: conversationId={}", conversationId);
                })
                .doOnError(error -> {
                    log.error("AI stream error: {}", error.getMessage());
                    onError.accept(error);
                })
                .subscribe();
    }

    @Transactional
    public void deleteConversation(Long conversationId) {
        Long userId = StpUtil.getLoginIdAsLong();

        AiConversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null) {
            throw new BusinessException("Conversation not found");
        }

        if (!conversation.getUserId().equals(userId)) {
            throw new BusinessException("No permission to delete this conversation");
        }

        messageMapper.deleteByConversationId(conversationId);
        conversationMapper.deleteById(conversationId);

        log.info("AI conversation deleted: userId={}, conversationId={}", userId, conversationId);
    }

    private ConversationResponse convertToConversationResponse(AiConversation conversation, List<AiMessage> messages) {
        ConversationResponse response = new ConversationResponse();
        response.setConversationId(conversation.getConversationId());
        response.setTitle(conversation.getTitle());
        response.setContextType(conversation.getContextType());
        response.setContextId(conversation.getContextId());
        response.setModelName(conversation.getModelName());
        response.setCreatedTime(conversation.getCreatedTime());
        response.setUpdatedTime(conversation.getUpdatedTime());
        response.setMessages(messages.stream()
                .map(this::convertToMessageResponse)
                .toList());
        return response;
    }

    private ConversationListResponse convertToConversationListResponse(AiConversation conversation) {
        ConversationListResponse response = new ConversationListResponse();
        response.setConversationId(conversation.getConversationId());
        response.setTitle(conversation.getTitle());
        response.setContextType(conversation.getContextType());
        response.setContextId(conversation.getContextId());
        response.setCreatedTime(conversation.getCreatedTime());
        response.setUpdatedTime(conversation.getUpdatedTime());
        return response;
    }

    private MessageResponse convertToMessageResponse(AiMessage message) {
        MessageResponse response = new MessageResponse();
        response.setMessageId(message.getMessageId());
        response.setRole(message.getRole());
        response.setContent(message.getContent());
        response.setCreatedTime(message.getCreatedTime());
        return response;
    }

}
