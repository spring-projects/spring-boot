/*
 * Copyright 2012-2022 the original author or authors.
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

/**
 * Configuration properties for Elasticsearch.
 *
 * @author Andy Wilkinson
 * @since 2.4.0
 */
@ConfigurationProperties("spring.elasticsearch")
public class ElasticsearchProperties {

	/**
	 * Comma-separated list of the Elasticsearch instances to use.
	 */
	private List<String> uris = new ArrayList<>(Collections.singletonList("http://localhost:9200"));

	/**
	 * Username for authentication with Elasticsearch.
	 */
	private String username;

	/**
	 * Password for authentication with Elasticsearch.
	 */
	private String password;

	/**
	 * Connection timeout used when communicating with Elasticsearch.
	 */
	private Duration connectionTimeout = Duration.ofSeconds(1);

	/**
	 * Socket timeout used when communicating with Elasticsearch.
	 */
	private Duration socketTimeout = Duration.ofSeconds(30);

	/**
	 * Prefix added to the path of every request sent to Elasticsearch.
	 */
	private String pathPrefix;

	private final Restclient restclient = new Restclient();

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

	public String getPathPrefix() {
		return this.pathPrefix;
	}

	public void setPathPrefix(String pathPrefix) {
		this.pathPrefix = pathPrefix;
	}

	public Restclient getRestclient() {
		return this.restclient;
	}

	public static class Restclient {

		private final Sniffer sniffer = new Sniffer();

		public Sniffer getSniffer() {
			return this.sniffer;
		}

		public static class Sniffer {

			/**
			 * Interval between consecutive ordinary sniff executions.
			 */
			private Duration interval = Duration.ofMinutes(5);

			/**
			 * Delay of a sniff execution scheduled after a failure.
			 */
			private Duration delayAfterFailure = Duration.ofMinutes(1);

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

	}

}
