package top.orosirian.orodisk.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;

@Slf4j
public class DistributedLock implements AutoCloseable {

    private static final String UNLOCK_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "    return redis.call('del', KEYS[1]) " +
            "else " +
            "    return 0 " +
            "end";

    private volatile boolean locked = false;
    private final StringRedisTemplate redisTemplate;
    private final String lockKey;
    private final String lockValue;
    private final Duration leaseTime;

    public DistributedLock(StringRedisTemplate redisTemplate, String lockKey, Duration leaseTime) {
        this.redisTemplate = redisTemplate;
        this.lockKey = lockKey;
        this.lockValue = UUID.randomUUID().toString();
        this.leaseTime = leaseTime;
    }

    public DistributedLock(StringRedisTemplate redisTemplate, String lockKey, long leaseSeconds) {
        this(redisTemplate, lockKey, Duration.ofSeconds(leaseSeconds));
    }

    public boolean tryLock() {
        locked = Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, leaseTime));
        return locked;
    }

    public boolean tryLock(long retryIntervalMillis, int maxRetryCount) {
        for (int i = 0; i <= maxRetryCount; i++) {
            if (tryLock()) {
                return true;
            }
            if (i < maxRetryCount) {
                try {
                    Thread.sleep(retryIntervalMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("获取锁被中断: key={}", lockKey);
                    return false;
                }
            }
        }
        return false;
    }

    public DistributedLock lock() {
        if (!tryLock()) {
            throw new IllegalStateException("获取锁失败: " + lockKey);
        }
        return this;
    }

    public void unlock() {
        if (!locked) {
            return;
        }
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(UNLOCK_SCRIPT, Long.class);
        Long result = redisTemplate.execute(script, Collections.singletonList(lockKey), lockValue);
        if (result == 1) {
            log.debug("释放锁成功: key={}", lockKey);
        } else {
            log.warn("释放锁失败(锁已过期或被其他线程持有): key={}", lockKey);
        }
        locked = false;
    }

    @Override
    public void close() {
        unlock();
    }
}
