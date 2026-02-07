package com.hundred.monitor.ai.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis配置类
 * 配置RedisTemplate的序列化方式
 */
@Slf4j
@Configuration
public class RedisConfig {


    @Bean
    public RedisSerializationContext redisSerializationContext() {


        // 创建配置好的 ObjectMapper
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        mapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        // 支持Java 8日期时间类型
        mapper.registerModule(new JavaTimeModule());

        // 方案1A: 使用新的构造函数（推荐）
        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(
                mapper,
                Object.class
        );

        // 使用StringRedisSerializer来序列化和反序列化redis的key值
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        RedisSerializationContext.RedisSerializationContextBuilder builder = RedisSerializationContext.newSerializationContext();
        builder.key(stringSerializer);
        builder.value(serializer);
        builder.hashKey(stringSerializer);
        builder.hashValue(serializer);

        return builder.build();
    }

    @Bean
    public ReactiveRedisTemplate reactiveRedisTemplate(ReactiveRedisConnectionFactory connectionFactory) {
        RedisSerializationContext serializationContext = redisSerializationContext();
        ReactiveRedisTemplate reactiveRedisTemplate = new ReactiveRedisTemplate(connectionFactory,serializationContext);
        return reactiveRedisTemplate;
    }
}
