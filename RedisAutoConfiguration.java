/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.autoconfigure.redis;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * Auto-configuration for Redis using Jedis client.
 * <p>
 * To enable Redis auto-configuration, set {@code spring.redis.enabled: true}
 * in your application.properties or application.yml file.
 *
 * @author Example Author
 * @since 1.0.0
 */
@AutoConfiguration
@EnableConfigurationProperties(RedisProperties.class)
@ConditionalOnClass({ Jedis.class, JedisPool.class })
@ConditionalOnProperty(name = "spring.redis.enabled", havingValue = "true", matchIfMissing = false)
public class RedisAutoConfiguration {

    private final RedisProperties properties;

    public RedisAutoConfiguration(RedisProperties properties) {
        this.properties = properties;
    }

    @Bean
    @ConditionalOnMissingBean
    JedisPoolConfig jedisPoolConfig() {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(properties.getMaxTotal());
        config.setMaxIdle(properties.getMaxIdle());
        config.setMinIdle(properties.getMinIdle());
        config.setMaxWaitMillis(properties.getMaxWaitMillis());
        return config;
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean
    JedisPool jedisPool(JedisPoolConfig poolConfig) {
        return new JedisPool(
            poolConfig,
            properties.getHost(),
            properties.getPort(),
            properties.getTimeout(),
            properties.getUsername(),
            properties.getPassword()
        );
    }

    @Bean
    @ConditionalOnMissingBean
    Jedis jedis(JedisPool jedisPool) {
        return jedisPool.getResource();
    }

}
