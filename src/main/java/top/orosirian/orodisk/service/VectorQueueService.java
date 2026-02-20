package top.orosirian.orodisk.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import top.orosirian.orodisk.utils.Constant;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
public class VectorQueueService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final DocumentVectorService documentVectorService;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public VectorQueueService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper, DocumentVectorService documentVectorService) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.documentVectorService = documentVectorService;
    }

    public void sendVectorizeMessage(Long storageId) {
        try {
            String message = objectMapper.writeValueAsString(Map.of("storageId", storageId));
            redisTemplate.opsForList().rightPush(Constant.QUEUE_KEY, message);
            log.info("Vectorize message sent: storageId={}", storageId);
        } catch (JsonProcessingException e) {
            log.error("Failed to send vectorize message: storageId={}", storageId, e);
        }
    }

    @SuppressWarnings("unchecked")
    @PostConstruct
    public void startConsumer() {
        executor.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    String message = redisTemplate.opsForList().leftPop(Constant.QUEUE_KEY, 5, java.util.concurrent.TimeUnit.SECONDS);
                    if (message != null) {
                        Map<String, Object> data = objectMapper.readValue(message, Map.class);
                        Long storageId = Long.valueOf(data.get("storageId").toString());
                        log.info("Processing vectorize message: storageId={}", storageId);
                        documentVectorService.vectorizeFile(storageId);
                    }
                } catch (Exception e) {
                    log.error("Error consuming vectorize message", e);
                }
            }
        });
        log.info("Vector queue consumer started");
    }

}
