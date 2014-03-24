/*
 * Copyright 2012-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.redis;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Redis.
 * 
 * @author Dave Syer
 */
@ConfigurationProperties(prefix = "spring.redis")
public class RedisProperties {

	private String host = "localhost";

	private String password;

	private int port = 6379;

	private RedisProperties.Pool pool;

	public String getHost() {
		return this.host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return this.port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getPassword() {
		return this.password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public RedisProperties.Pool getPool() {
		return this.pool;
	}

	public void setPool(RedisProperties.Pool pool) {
		this.pool = pool;
	}

	/**
	 * Pool properties.
	 */
	public static class Pool {

		private int maxIdle = 8;

		private int minIdle = 0;

		private int maxActive = 8;

		private int maxWait = -1;

		public int getMaxIdle() {
			return this.maxIdle;
		}

		public void setMaxIdle(int maxIdle) {
			this.maxIdle = maxIdle;
		}

		public int getMinIdle() {
			return this.minIdle;
		}

		public void setMinIdle(int minIdle) {
			this.minIdle = minIdle;
		}

		public int getMaxActive() {
			return this.maxActive;
		}

		public void setMaxActive(int maxActive) {
			this.maxActive = maxActive;
		}

		public int getMaxWait() {
			return this.maxWait;
		}

		public void setMaxWait(int maxWait) {
			this.maxWait = maxWait;
		}
	}

}
