package com.hyq.product.redis;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.io.Serializable;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * @author nanke
 * @date 2020/11/16 8:35 上午
 * 致终于来到这里的勇敢的人:
 * 永远不要放弃！永远不要对自己失望！永远不要逃走辜负了自己。
 * 永远不要哭啼！永远不要说再见！永远不要说慌来伤害目己。
 *
 * Redis服务封装类
 */
@Component
public class RedisService {

    @Resource
    private RedisTemplate<Serializable, Object> productRedisTemplate;

    /**
     * 查询, 如果不存在则创建
     *      设置默认值
     * @param key        key
     * @param supplier   supplier
     * @param expireTime 过期时间
     * @param <T>        T
     * @return 查询到的值或者表达式中的值
     */
    public <T extends Object> T getOrCreate(String key, Supplier<T> supplier, int expireTime){
        T cached = get(key);
        if (cached != null) {
            if (cached instanceof RedisNullValue) {
                return null;
            }
            return cached;
        }
        if (supplier == null) {
            return null;
        }
        T supplierData = supplier.get();
        if (supplierData == null) {
            putNullValue(key, expireTime);
            return null;
        }
        set(key, supplierData, expireTime);
        return supplierData;
    }

    public void putNullValue(String key, int seconds) {
        productRedisTemplate.opsForValue().set(key,new RedisNullValue(),seconds, TimeUnit.SECONDS);
    }

    //============================String=============================

    /**
     * 普通缓存获取
     * @param key 键
     * @return 值
     */
    public <T extends Serializable> T get(String key){
        return key == null ? null : (T) productRedisTemplate.opsForValue().get(key);
    }

    /**
     * 普通缓存放入
     * @param key 键
     * @param value 值
     * @return true成功 false失败
     */
    public boolean set(String key,Object value) {
        try {
            productRedisTemplate.opsForValue().set(key, value);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 普通缓存放入并设置时间
     * @param key 键
     * @param value 值
     * @param time 时间(秒) time要大于0 如果time小于等于0 将设置无限期
     * @return true成功 false 失败
     */
    public boolean set(String key,Object value,long time){
        try {
            if (time > 0) {
                productRedisTemplate.opsForValue().set(key, value, time, TimeUnit.SECONDS);
            } else {
                set(key, value);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 删除缓存
     * @param key 可以传一个值或多个
     */
    @SuppressWarnings("unchecked")
    public void del(String ... key){
        if (key != null && key.length > 0) {
            productRedisTemplate.delete(CollectionUtils.arrayToList(key));
        }
    }

}