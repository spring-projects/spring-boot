/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.autoconfigure.elasticsearch.jest;

import java.util.Collections;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Jest.
 *
 * @author Stephane Nicoll
 * @since 1.4.0
 */
@ConfigurationProperties("spring.elasticsearch.jest")
public class JestProperties {

	/**
	 * Comma-separated list of the Elasticsearch instances to use.
	 */
	private List<String> uris = Collections.singletonList("http://localhost:9200");

	/**
	 * Login user.
	 */
	private String username;

	/**
	 * Login password.
	 */
	private String password;

	/**
	 * Connection timeout in milliseconds.
	 */
	private int connectionTimeout = 3000;

	/**
	 * Read timeout in milliseconds.
	 */
	private int readTimeout = 3000;

	/**
	 * Proxy settings.
	 */
	private final Proxy proxy = new Proxy();

	public List<String> getUris() {
		return this.uris;
	}

	public void setUris(List<String> uris) {
		this.uris = uris;
	}

	public String getUsername() {
		return this.username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return this.password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public int getConnectionTimeout() {
		return this.connectionTimeout;
	}

	public void setConnectionTimeout(int connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
	}

	public int getReadTimeout() {
		return this.readTimeout;
	}

	public void setReadTimeout(int readTimeout) {
		this.readTimeout = readTimeout;
	}

	public Proxy getProxy() {
		return this.proxy;
	}

	public static class Proxy {

		/**
		 * Proxy host the HTTP client should use.
		 */
		private String host;

		/**
		 * Proxy port the HTTP client should use.
		 */
		private Integer port;

		public String getHost() {
			return this.host;
		}

		public void setHost(String host) {
			this.host = host;
		}

		public Integer getPort() {
			return this.port;
		}

		public void setPort(Integer port) {
			this.port = port;
		}

	}

}
