package com.hyq.product.redis;

import com.hyq.product.enums.EnumRedisLock;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

/**
 * @author nanke
 * @date 2020/11/16 9:59 上午
 * 致终于来到这里的勇敢的人:
 * 永远不要放弃！永远不要对自己失望！永远不要逃走辜负了自己。
 * 永远不要哭啼！永远不要说再见！永远不要说慌来伤害目己。
 */
@Slf4j
public class RedisDistLock {

    private RedissonClient client;

    public void setClient(RedissonClient client) {
        this.client = client;
    }

    /**
     * 尝试加锁
     * @param enumRedisLock
     * @param args
     * @return
     */
    public boolean tryLock(EnumRedisLock enumRedisLock, String... args) {
        String key = enumRedisLock.keyPrefix + String.join("_", args);
        RLock lock = client.getLock(key);
        try {
            return lock.tryLock(enumRedisLock.waitTime, enumRedisLock.lockTime, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(key + "尝试加锁失败");
        }
    }

    /**
     * 释放锁
     * @param enumRedisLock
     * @param args
     */
    public void unlock(EnumRedisLock enumRedisLock, String... args) {
        String key = enumRedisLock.keyPrefix + String.join("_", args);
        RLock lock = client.getLock(key);
        lock.unlock();
    }

}