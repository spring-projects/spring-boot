/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.autoconfigure.elasticsearch;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.DeprecatedConfigurationProperty;

/**
 * Deprecated configuration properties for Elasticsearch REST clients.
 *
 * @author Brian Clozel
 * @deprecated since 2.6.0 for removal in 2.8.0.
 */
@ConfigurationProperties(prefix = "spring.elasticsearch.rest")
@Deprecated
class DeprecatedElasticsearchRestClientProperties {

	/**
	 * Comma-separated list of the Elasticsearch instances to use.
	 */
	private List<String> uris = new ArrayList<>(Collections.singletonList("http://localhost:9200"));

	/**
	 * Credentials username.
	 */
	private String username;

	/**
	 * Credentials password.
	 */
	private String password;

	/**
	 * Connection timeout.
	 */
	private Duration connectionTimeout = Duration.ofSeconds(1);

	/**
	 * Read timeout.
	 */
	private Duration readTimeout = Duration.ofSeconds(30);

	private final Sniffer sniffer = new Sniffer();

	private boolean customized = false;

	@DeprecatedConfigurationProperty(replacement = "spring.elasticsearch.uris")
	public List<String> getUris() {
		return this.uris;
	}

	public void setUris(List<String> uris) {
		this.customized = true;
		this.uris = uris;
	}

	@DeprecatedConfigurationProperty(replacement = "spring.elasticsearch.username")
	public String getUsername() {
		return this.username;
	}

	public void setUsername(String username) {
		this.customized = true;
		this.username = username;
	}

	@DeprecatedConfigurationProperty(replacement = "spring.elasticsearch.password")
	public String getPassword() {
		return this.password;
	}

	public void setPassword(String password) {
		this.customized = true;
		this.password = password;
	}

	@DeprecatedConfigurationProperty(replacement = "spring.elasticsearch.connection-timeout")
	public Duration getConnectionTimeout() {
		return this.connectionTimeout;
	}

	public void setConnectionTimeout(Duration connectionTimeout) {
		this.customized = true;
		this.connectionTimeout = connectionTimeout;
	}

	@DeprecatedConfigurationProperty(replacement = "spring.elasticsearch.socket-timeout")
	public Duration getReadTimeout() {
		return this.readTimeout;
	}

	public void setReadTimeout(Duration readTimeout) {
		this.customized = true;
		this.readTimeout = readTimeout;
	}

	boolean isCustomized() {
		return this.customized;
	}

	public Sniffer getSniffer() {
		return this.sniffer;
	}

	@Deprecated
	class Sniffer {

		/**
		 * Interval between consecutive ordinary sniff executions.
		 */
		private Duration interval = Duration.ofMinutes(5);

		/**
		 * Delay of a sniff execution scheduled after a failure.
		 */
		private Duration delayAfterFailure = Duration.ofMinutes(1);

		@DeprecatedConfigurationProperty(replacement = "spring.elasticsearch.restclient.sniffer.interval")
		public Duration getInterval() {
			return this.interval;
		}

		public void setInterval(Duration interval) {
			DeprecatedElasticsearchRestClientProperties.this.customized = true;
			this.interval = interval;
		}

		@DeprecatedConfigurationProperty(replacement = "spring.elasticsearch.restclient.sniffer.delay-after-failure")
		public Duration getDelayAfterFailure() {
			return this.delayAfterFailure;
		}

		public void setDelayAfterFailure(Duration delayAfterFailure) {
			DeprecatedElasticsearchRestClientProperties.this.customized = true;
			this.delayAfterFailure = delayAfterFailure;
		}

	}

}
