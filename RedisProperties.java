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

import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Redis.
 *
 * @author Example Author
 * @since 1.0.0
 */
@ConfigurationProperties("spring.redis")
public class RedisProperties {

    /**
     * Whether Redis auto-configuration is enabled.
     */
    private boolean enabled = false;

    /**
     * Redis server host name.
     */
    private String host = "localhost";

    /**
     * Redis server port.
     */
    private int port = 6379;

    /**
     * Redis connection timeout in milliseconds.
     */
    private int timeout = 2000;

    /**
     * Redis username for authentication.
     */
    private @Nullable String username;

    /**
     * Redis password for authentication.
     */
    private @Nullable String password;

    /**
     * Maximum number of connections that can be allocated by the pool at a given time.
     */
    private int maxTotal = 8;

    /**
     * Maximum number of idle connections in the pool.
     */
    private int maxIdle = 8;

    /**
     * Minimum number of idle connections in the pool.
     */
    private int minIdle = 0;

    /**
     * Maximum wait time in milliseconds for a connection to be available from the pool.
     */
    private long maxWaitMillis = -1;

    // Getters and setters

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public @Nullable String getUsername() {
        return username;
    }

    public void setUsername(@Nullable String username) {
        this.username = username;
    }

    public @Nullable String getPassword() {
        return password;
    }

    public void setPassword(@Nullable String password) {
        this.password = password;
    }

    public int getMaxTotal() {
        return maxTotal;
    }

    public void setMaxTotal(int maxTotal) {
        this.maxTotal = maxTotal;
    }

    public int getMaxIdle() {
        return maxIdle;
    }

    public void setMaxIdle(int maxIdle) {
        this.maxIdle = maxIdle;
    }

    public int getMinIdle() {
        return minIdle;
    }

    public void setMinIdle(int minIdle) {
        this.minIdle = minIdle;
    }

    public long getMaxWaitMillis() {
        return maxWaitMillis;
    }

    public void setMaxWaitMillis(long maxWaitMillis) {
        this.maxWaitMillis = maxWaitMillis;
    }

}
