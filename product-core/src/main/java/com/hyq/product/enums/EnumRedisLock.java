package com.hyq.product.enums;

/**
 * 分布式锁key枚举
 */
public enum EnumRedisLock {

    INVENTORY_READ("inventoryRead",10L,30000L),
    ;

    /**
     * 锁前缀
     */
    public String keyPrefix;
    /**
     * 锁等待时间 毫秒
     */
    public Long waitTime;
    /**
     * 锁超时时间 毫秒
     */
    public Long lockTime;

    EnumRedisLock(String keyPrefix, Long waitTime, Long lockTime) {
        this.keyPrefix = keyPrefix;
        this.waitTime = waitTime;
        this.lockTime = lockTime;
    }
}
