/*
 * Copyright 2012-2023 the original author or authors.
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
	 * Whether to enable socket keep alive between client and Elasticsearch.
	 */
	private boolean socketKeepAlive = false;

	/**
	 * Prefix added to the path of every request sent to Elasticsearch.
	 */
	private String pathPrefix;

	private final Restclient restclient = new Restclient();

	/**
     * Returns the list of URIs.
     *
     * @return the list of URIs
     */
    public List<String> getUris() {
		return this.uris;
	}

	/**
     * Sets the list of URIs for Elasticsearch.
     * 
     * @param uris the list of URIs to set
     */
    public void setUris(List<String> uris) {
		this.uris = uris;
	}

	/**
     * Returns the username associated with the Elasticsearch properties.
     *
     * @return the username
     */
    public String getUsername() {
		return this.username;
	}

	/**
     * Sets the username for authentication.
     * 
     * @param username the username to set
     */
    public void setUsername(String username) {
		this.username = username;
	}

	/**
     * Returns the password.
     *
     * @return the password
     */
    public String getPassword() {
		return this.password;
	}

	/**
     * Sets the password for the Elasticsearch connection.
     * 
     * @param password the password to set
     */
    public void setPassword(String password) {
		this.password = password;
	}

	/**
     * Returns the connection timeout duration.
     *
     * @return the connection timeout duration
     */
    public Duration getConnectionTimeout() {
		return this.connectionTimeout;
	}

	/**
     * Sets the connection timeout for the Elasticsearch client.
     * 
     * @param connectionTimeout the connection timeout duration
     */
    public void setConnectionTimeout(Duration connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
	}

	/**
     * Returns the socket timeout duration.
     *
     * @return the socket timeout duration
     */
    public Duration getSocketTimeout() {
		return this.socketTimeout;
	}

	/**
     * Sets the socket timeout for the Elasticsearch client.
     * 
     * @param socketTimeout the duration of the socket timeout
     */
    public void setSocketTimeout(Duration socketTimeout) {
		this.socketTimeout = socketTimeout;
	}

	/**
     * Returns the value indicating whether the socket keep-alive option is enabled.
     *
     * @return {@code true} if the socket keep-alive option is enabled, {@code false} otherwise
     */
    public boolean isSocketKeepAlive() {
		return this.socketKeepAlive;
	}

	/**
     * Sets the value indicating whether the socket keep-alive option is enabled.
     * 
     * @param socketKeepAlive the value indicating whether the socket keep-alive option is enabled
     */
    public void setSocketKeepAlive(boolean socketKeepAlive) {
		this.socketKeepAlive = socketKeepAlive;
	}

	/**
     * Returns the path prefix.
     * 
     * @return the path prefix
     */
    public String getPathPrefix() {
		return this.pathPrefix;
	}

	/**
     * Sets the path prefix for Elasticsearch.
     * 
     * @param pathPrefix the path prefix to be set
     */
    public void setPathPrefix(String pathPrefix) {
		this.pathPrefix = pathPrefix;
	}

	/**
     * Returns the RestClient object associated with this ElasticsearchProperties instance.
     *
     * @return the RestClient object
     */
    public Restclient getRestclient() {
		return this.restclient;
	}

	/**
     * Restclient class.
     */
    public static class Restclient {

		private final Sniffer sniffer = new Sniffer();

		private final Ssl ssl = new Ssl();

		/**
         * Returns the Sniffer object associated with this RestClient.
         * 
         * @return the Sniffer object
         */
        public Sniffer getSniffer() {
			return this.sniffer;
		}

		/**
         * Returns the SSL object associated with this RestClient.
         *
         * @return the SSL object
         */
        public Ssl getSsl() {
			return this.ssl;
		}

		/**
         * Sniffer class.
         */
        public static class Sniffer {

			/**
			 * Interval between consecutive ordinary sniff executions.
			 */
			private Duration interval = Duration.ofMinutes(5);

			/**
			 * Delay of a sniff execution scheduled after a failure.
			 */
			private Duration delayAfterFailure = Duration.ofMinutes(1);

			/**
             * Returns the interval between two events.
             *
             * @return the interval between two events
             */
            public Duration getInterval() {
				return this.interval;
			}

			/**
             * Sets the interval at which the sniffer should run.
             * 
             * @param interval the duration between each execution of the sniffer
             */
            public void setInterval(Duration interval) {
				this.interval = interval;
			}

			/**
             * Returns the delay after a failure occurs.
             * 
             * @return the delay after a failure occurs
             */
            public Duration getDelayAfterFailure() {
				return this.delayAfterFailure;
			}

			/**
             * Sets the delay after a failure.
             * 
             * @param delayAfterFailure the duration of delay after a failure
             */
            public void setDelayAfterFailure(Duration delayAfterFailure) {
				this.delayAfterFailure = delayAfterFailure;
			}

		}

		/**
         * Ssl class.
         */
        public static class Ssl {

			/**
			 * SSL bundle name.
			 */
			private String bundle;

			/**
             * Returns the bundle associated with this Ssl object.
             * 
             * @return the bundle associated with this Ssl object
             */
            public String getBundle() {
				return this.bundle;
			}

			/**
             * Sets the bundle for the Ssl class.
             * 
             * @param bundle the bundle to be set
             */
            public void setBundle(String bundle) {
				this.bundle = bundle;
			}

		}

	}

}
