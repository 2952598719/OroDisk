package top.orosirian.orodisk.service;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.orosirian.orodisk.mappers.UserMapper;
import top.orosirian.orodisk.model.entity.User;
import top.orosirian.orodisk.model.request.LoginRequest;
import top.orosirian.orodisk.model.request.RegisterRequest;
import top.orosirian.orodisk.model.response.LoginResponse;
import top.orosirian.orodisk.model.response.UserInfoResponse;
import top.orosirian.orodisk.utils.Constant;
import top.orosirian.orodisk.utils.exceptions.BusinessException;
import top.orosirian.orodisk.utils.enums.UserStatus;

import java.time.Duration;
import java.util.Objects;

@Slf4j
@Service
public class UserService {

    @Value("${disk.quota.default-size}")
    private Long defaultQuota;

    private final UserMapper userMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public UserService(UserMapper userMapper, StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.userMapper = userMapper;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userMapper.selectByUsername(request.getUsername(), null);
        if (user == null) {
            throw new BusinessException("Invalid username or password");
        }

        if (!BCrypt.checkpw(request.getPassword(), user.getPassword())) {
            throw new BusinessException("Invalid username or password");
        }

        if (Objects.equals(user.getStatus(), UserStatus.FORBIDDEN.getCode())) {
            throw new BusinessException("Account has been disabled");
        }

        if (Objects.equals(user.getStatus(), UserStatus.UNREGISTER.getCode())) {
            throw new BusinessException("Account has been cancelled");
        }

        StpUtil.login(user.getUserId());

        LoginResponse response = new LoginResponse();
        response.setToken(StpUtil.getTokenValue());
        response.setUserInfo(new UserInfoResponse(user));

        log.info("User login successful: {}", user.getUsername());
        return response;
    }

    @Transactional
    public void register(RegisterRequest request) {
        User existUser = userMapper.selectByUsername(request.getUsername(), null);
        if (existUser != null) {
            throw new BusinessException("Username already exists");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(BCrypt.hashpw(request.getPassword(), BCrypt.gensalt()));
        user.setTotalQuota(defaultQuota);
        user.setUsedQuota(0L);
        user.setStatus(UserStatus.NORMAL.getCode());
        user.setType(1);

        userMapper.insert(user);
        log.info("User registration successful: {}", user.getUsername());
    }

    public void logout() {
        StpUtil.logout();
        log.info("User logout successful");
    }

    public UserInfoResponse getCurrentUser() {
        Long userId = StpUtil.getLoginIdAsLong();
        User user = userMapper.selectById(userId, UserStatus.NORMAL.getCode());
        if (user == null) {
            throw new BusinessException("User not found");
        }
        return new UserInfoResponse(user);
    }

    public UserInfoResponse getUserById(Long userId) {
        User user = userMapper.selectById(userId, UserStatus.NORMAL.getCode());
        if (user == null) {
            throw new BusinessException("User not found");
        }
        return new UserInfoResponse(user);
    }

    @Transactional
    public void updatePassword(String oldPassword, String newPassword) {
        Long userId = StpUtil.getLoginIdAsLong();
        User user = userMapper.selectById(userId, UserStatus.NORMAL.getCode());
        if (user == null) {
            throw new BusinessException("User not found");
        }

        if (!BCrypt.checkpw(oldPassword, user.getPassword())) {
            throw new BusinessException("Incorrect old password");
        }

        user.setPassword(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
        userMapper.update(user);
        log.info("Password updated successfully: {}", user.getUsername());
    }

    /**
     * Get user quota information (with cache)
     */
    public UserQuota getUserQuota(Long userId) {
        String cacheKey = Constant.USER_QUOTA_CACHE_PREFIX + userId;
        String cached = redisTemplate.opsForValue().get(cacheKey);
        
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, UserQuota.class);
            } catch (JsonProcessingException e) {
                log.warn("Failed to parse quota cache: userId={}", userId, e);
            }
        }
        
        User user = userMapper.selectById(userId, UserStatus.NORMAL.getCode());
        if (user == null) {
            throw new BusinessException("User not found");
        }
        
        UserQuota quota = new UserQuota(user.getTotalQuota(), user.getUsedQuota());
        setQuotaCache(userId, quota);
        return quota;
    }

    /**
     * Increase used quota (cache + database)
     */
    public void incrementUsedQuota(Long userId, Long fileSize) {
        userMapper.incrementQuota(userId, fileSize);
        updateQuotaCache(userId, fileSize);
        log.debug("Increase quota: userId={}, fileSize={}", userId, fileSize);
    }

    /**
     * Decrease used quota (cache + database)
     */
    public void decrementUsedQuota(Long userId, Long fileSize) {
        userMapper.decrementQuota(userId, fileSize);
        updateQuotaCache(userId, -fileSize);
        log.debug("Decrease quota: userId={}, fileSize={}", userId, fileSize);
    }

    /**
     * Evict quota cache (called when quota changes)
     */
    public void evictQuotaCache(Long userId) {
        String cacheKey = Constant.USER_QUOTA_CACHE_PREFIX + userId;
        redisTemplate.delete(cacheKey);
        log.debug("Evict quota cache: userId={}", userId);
    }

    private void setQuotaCache(Long userId, UserQuota quota) {
        String cacheKey = Constant.USER_QUOTA_CACHE_PREFIX + userId;
        try {
            String json = objectMapper.writeValueAsString(quota);
            redisTemplate.opsForValue().set(cacheKey, json, Duration.ofSeconds(Constant.CACHE_QUOTA_TTL));
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize quota cache: userId={}", userId, e);
        }
    }

    private void updateQuotaCache(Long userId, Long delta) {
        String cacheKey = Constant.USER_QUOTA_CACHE_PREFIX + userId;
        String cached = redisTemplate.opsForValue().get(cacheKey);
        
        if (cached != null) {
            try {
                UserQuota quota = objectMapper.readValue(cached, UserQuota.class);
                quota.setUsedQuota(quota.getUsedQuota() + delta);
                setQuotaCache(userId, quota);
            } catch (JsonProcessingException e) {
                log.warn("Failed to update quota cache: userId={}", userId, e);
                redisTemplate.delete(cacheKey);
            }
        }
    }

    /**
     * User quota information (for caching)
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class UserQuota {
        private Long totalQuota;
        private Long usedQuota;
    }
}
