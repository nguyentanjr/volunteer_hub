package com.example.demo.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
@EnableCaching
public class RedisConfig {

    // DON'T name it "objectMapper" - that would override Spring's default!
    // Use a specific name so it's ONLY used for Redis
    private ObjectMapper createRedisObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.findAndRegisterModules();
        
        // Enable default typing ONLY for Redis serialization
        // This preserves type information for proper deserialization
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Object.class)
                .build();
        
        mapper.activateDefaultTyping(
                ptv,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        
        return mapper;
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Use String serializer for keys
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // Use JSON serializer with custom ObjectMapper for Redis only
        GenericJackson2JsonRedisSerializer jsonSerializer =
                new GenericJackson2JsonRedisSerializer(createRedisObjectMapper());
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // Use GenericJackson2JsonRedisSerializer with custom ObjectMapper for Redis only
        GenericJackson2JsonRedisSerializer jsonSerializer =
                new GenericJackson2JsonRedisSerializer(createRedisObjectMapper());

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1)) // Default TTL: 1 hour
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer())
                )
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer)
                )
                .disableCachingNullValues();

        // Custom TTL for specific caches
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                // Dashboard caches - short TTL (data changes frequently)
                .withCacheConfiguration("dashboard",
                        defaultConfig.entryTtl(Duration.ofMinutes(3)))
                // Event-related caches
                .withCacheConfiguration("events",
                        defaultConfig.entryTtl(Duration.ofMinutes(30)))
                .withCacheConfiguration("eventDetails",
                        defaultConfig.entryTtl(Duration.ofMinutes(15)))
                .withCacheConfiguration("recommendations",
                        defaultConfig.entryTtl(Duration.ofMinutes(10)))
                // User caches
                .withCacheConfiguration("users",
                        defaultConfig.entryTtl(Duration.ofHours(2)))
                // Reference data - long TTL (rarely changes)
                .withCacheConfiguration("tags",
                        defaultConfig.entryTtl(Duration.ofHours(24)))
                .withCacheConfiguration("dashboard",
                        defaultConfig.entryTtl(Duration.ofMinutes(5)))
                .transactionAware()
                .build();
    }
}