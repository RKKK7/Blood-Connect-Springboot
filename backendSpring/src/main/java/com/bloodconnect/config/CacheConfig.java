package com.bloodconnect.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;

/**
 * Enables Spring's caching abstraction backed by Redis.
 *
 * Values are stored as JSON and each named cache has its own time-to-live so
 * stale data self-expires without manual eviction:
 *
 *   leaderboard  - 5 min  (changes only when donations are recorded)
 *   nearbyDonors - 2 min  (geo search results; tolerate slight staleness)
 *   healthTips   - 1 hour (expensive Groq AI calls; safe to reuse)
 *
 * If CACHE_TYPE=none, Spring uses a no-op cache and this manager is ignored,
 * so the app still boots with no Redis server present.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Our cached values are Maps/Lists that include java.time.Instant fields
     * (createdAt, lastDonated, ...). The default Redis JSON serializer can't
     * handle Java-8 time types, so we register JavaTimeModule. Default typing
     * is enabled so generic Maps/Lists round-trip back to the same types.
     */
    private GenericJackson2JsonRedisSerializer jsonSerializer() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder().allowIfSubType(Object.class).build(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);
        return new GenericJackson2JsonRedisSerializer(mapper);
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration base = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(jsonSerializer()))
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> perCache = Map.of(
                "leaderboard",  base.entryTtl(Duration.ofMinutes(5)),
                "nearbyDonors", base.entryTtl(Duration.ofMinutes(2)),
                "healthTips",   base.entryTtl(Duration.ofHours(1))
        );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(base.entryTtl(Duration.ofMinutes(5)))
                .withInitialCacheConfigurations(perCache)
                .build();
    }
}
