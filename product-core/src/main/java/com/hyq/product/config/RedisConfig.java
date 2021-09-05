package com.hyq.product.config;

import com.hyq.product.redis.RedisDistLock;
import com.hyq.product.redis.FastJSONRedisSerializer;
import org.apache.commons.lang3.StringUtils;
import org.redisson.Redisson;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.io.Serializable;

/**
 * redis配置类
 * @program:
 * @Description:
 */
@Configuration
public class RedisConfig {

    // TODO hu 配置
    @Value("${spring.redis.host}")
    private String host;
    @Value("${spring.redis.port}")
    private Integer port;
    @Value("${spring.redis.password}")
    private String password;

    @Primary
    @Bean("productRedisFactory")
    public JedisConnectionFactory getConnectionFactory() {
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
        configuration.setHostName(host);
        configuration.setDatabase(0);
        configuration.setPassword(password);
        configuration.setPort(port);
        return new JedisConnectionFactory(configuration);
    }


    @Bean("productRedisTemplate")
    public RedisTemplate<Serializable, Object> redisTemplate(@Qualifier("productRedisFactory") JedisConnectionFactory factory) {
        RedisTemplate<Serializable, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(factory);
        // Key,Value序列化方式
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        FastJSONRedisSerializer valueSerializer = new FastJSONRedisSerializer(Object.class);
        // 设置
        redisTemplate.setKeySerializer(keySerializer);
        redisTemplate.setValueSerializer(valueSerializer);
        redisTemplate.setHashKeySerializer(keySerializer);
        redisTemplate.setHashValueSerializer(valueSerializer);
        return redisTemplate;
    }

    @Bean
    public RedisDistLock getRedisDistLock() {
        Config config = new Config();
        if (StringUtils.isBlank(password)){
            config.useSingleServer().setAddress("redis://" + host + ":" + port);
        }else {
            config.useSingleServer().setAddress("redis://" + host + ":" + port).setPassword(password);
        }
        RedisDistLock distLock = new RedisDistLock();
        distLock.setClient(Redisson.create(config));
        return distLock;
    }

}
