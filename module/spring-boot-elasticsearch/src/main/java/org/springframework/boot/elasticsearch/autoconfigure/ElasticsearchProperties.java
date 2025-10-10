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

package org.springframework.boot.elasticsearch.autoconfigure;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Elasticsearch.
 *
 * @author Andy Wilkinson
 * @since 4.0.0
 */
@ConfigurationProperties("spring.elasticsearch")
public class ElasticsearchProperties {

	/**
	 * List of the Elasticsearch instances to use.
	 */
	private List<String> uris = new ArrayList<>(Collections.singletonList("http://localhost:9200"));

	/**
	 * Username for authentication with Elasticsearch.
	 */
	private @Nullable String username;

	/**
	 * Password for authentication with Elasticsearch.
	 */
	private @Nullable String password;

	/**
	 * API key for authentication with Elasticsearch.
	 */
	private @Nullable String apiKey;

	/**
	 * Connection timeout used when communicating with Elasticsearch.
	 */
	private Duration connectionTimeout = Duration.ofSeconds(1);

	/**
	 * Socket timeout used when communicating with Elasticsearch.
	 */
	private Duration socketTimeout = Duration.ofSeconds(30);

	/**
	 * Whether to enable socket keep alive between client and Elasticsearch.
	 */
	private boolean socketKeepAlive = false;

	/**
	 * Prefix added to the path of every request sent to Elasticsearch.
	 */
	private @Nullable String pathPrefix;

	private final Restclient restclient = new Restclient();

	public List<String> getUris() {
		return this.uris;
	}

	public void setUris(List<String> uris) {
		this.uris = uris;
	}

	public @Nullable String getUsername() {
		return this.username;
	}

	public void setUsername(@Nullable String username) {
		this.username = username;
	}

	public @Nullable String getPassword() {
		return this.password;
	}

	public void setPassword(@Nullable String password) {
		this.password = password;
	}

	public @Nullable String getApiKey() {
		return this.apiKey;
	}

	public void setApiKey(@Nullable String apiKey) {
		this.apiKey = apiKey;
	}

	public Duration getConnectionTimeout() {
		return this.connectionTimeout;
	}

	public void setConnectionTimeout(Duration connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
	}

	public Duration getSocketTimeout() {
		return this.socketTimeout;
	}

	public void setSocketTimeout(Duration socketTimeout) {
		this.socketTimeout = socketTimeout;
	}

	public boolean isSocketKeepAlive() {
		return this.socketKeepAlive;
	}

	public void setSocketKeepAlive(boolean socketKeepAlive) {
		this.socketKeepAlive = socketKeepAlive;
	}

	public @Nullable String getPathPrefix() {
		return this.pathPrefix;
	}

	public void setPathPrefix(@Nullable String pathPrefix) {
		this.pathPrefix = pathPrefix;
	}

	public Restclient getRestclient() {
		return this.restclient;
	}

	public static class Restclient {

		private final Sniffer sniffer = new Sniffer();

		private final Ssl ssl = new Ssl();

		public Sniffer getSniffer() {
			return this.sniffer;
		}

		public Ssl getSsl() {
			return this.ssl;
		}

		public static class Sniffer {

			/**
			 * Whether the sniffer is enabled.
			 */
			private boolean enabled = true;

			/**
			 * Interval between consecutive ordinary sniff executions.
			 */
			private Duration interval = Duration.ofMinutes(5);

			/**
			 * Delay of a sniff execution scheduled after a failure.
			 */
			private Duration delayAfterFailure = Duration.ofMinutes(1);

			public boolean isEnabled() {
				return this.enabled;
			}

			public void setEnabled(boolean enabled) {
				this.enabled = enabled;
			}

			public Duration getInterval() {
				return this.interval;
			}

			public void setInterval(Duration interval) {
				this.interval = interval;
			}

			public Duration getDelayAfterFailure() {
				return this.delayAfterFailure;
			}

			public void setDelayAfterFailure(Duration delayAfterFailure) {
				this.delayAfterFailure = delayAfterFailure;
			}

		}

		public static class Ssl {

			/**
			 * SSL bundle name.
			 */
			private @Nullable String bundle;

			public @Nullable String getBundle() {
				return this.bundle;
			}

			public void setBundle(@Nullable String bundle) {
				this.bundle = bundle;
			}

		}

	}

}
