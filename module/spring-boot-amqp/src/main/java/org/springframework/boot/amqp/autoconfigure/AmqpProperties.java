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

package org.springframework.boot.amqp.autoconfigure;

import java.time.Duration;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for AMQP 1.0 connectivity.
 *
 * @author Stephane Nicoll
 * @since 4.2.0
 */
@ConfigurationProperties("spring.amqp")
public class AmqpProperties {

	/**
	 * AMQP broker host.
	 */
	private String host = "localhost";

	/**
	 * AMQP broker port.
	 */
	private int port = 5672;

	/**
	 * Login user to authenticate to the broker.
	 */
	private @Nullable String username;

	/**
	 * Password used to authenticate to the broker.
	 */
	private @Nullable String password;

	private final Client client = new Client();

	private final Listener listener = new Listener();

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

	public Client getClient() {
		return this.client;
	}

	public Listener getListener() {
		return this.listener;
	}

	/**
	 * Client-level settings.
	 */
	public static class Client {

		/**
		 * Default destination address for send operations when none is specified.
		 */
		private @Nullable String defaultToAddress;

		/**
		 * Maximum time to wait for request operations to complete.
		 */
		private Duration completionTimeout = Duration.ofSeconds(60);

		public @Nullable String getDefaultToAddress() {
			return this.defaultToAddress;
		}

		public void setDefaultToAddress(@Nullable String defaultToAddress) {
			this.defaultToAddress = defaultToAddress;
		}

		public Duration getCompletionTimeout() {
			return this.completionTimeout;
		}

		public void setCompletionTimeout(Duration completionTimeout) {
			this.completionTimeout = completionTimeout;
		}

	}

	public static class Listener {

		/**
		 * Whether to start the container automatically on startup.
		 */
		private boolean autoStartup = true;

		/**
		 * Number of consumers.
		 */
		private int concurrency = 1;

		/**
		 * Initial number of credits to grant to the AMQP receiver.
		 */
		private int initialCredits = 100;

		/**
		 * Timeout for receive operations.
		 */
		private Duration receiveTimeout = Duration.ofSeconds(1);

		/**
		 * How long to wait for all consumers to shut down successfully.
		 */
		private Duration gracefulShutdownPeriod = Duration.ofSeconds(30);

		/**
		 * Whether to requeue messages in case of error.
		 */
		private boolean defaultRequeueRejected = true;

		public boolean isAutoStartup() {
			return this.autoStartup;
		}

		public void setAutoStartup(boolean autoStartup) {
			this.autoStartup = autoStartup;
		}

		public int getConcurrency() {
			return this.concurrency;
		}

		public void setConcurrency(int concurrency) {
			this.concurrency = concurrency;
		}

		public int getInitialCredits() {
			return this.initialCredits;
		}

		public void setInitialCredits(int initialCredits) {
			this.initialCredits = initialCredits;
		}

		public Duration getReceiveTimeout() {
			return this.receiveTimeout;
		}

		public void setReceiveTimeout(Duration receiveTimeout) {
			this.receiveTimeout = receiveTimeout;
		}

		public Duration getGracefulShutdownPeriod() {
			return this.gracefulShutdownPeriod;
		}

		public void setGracefulShutdownPeriod(Duration gracefulShutdownPeriod) {
			this.gracefulShutdownPeriod = gracefulShutdownPeriod;
		}

		public boolean isDefaultRequeueRejected() {
			return this.defaultRequeueRejected;
		}

		public void setDefaultRequeueRejected(boolean defaultRequeueRejected) {
			this.defaultRequeueRejected = defaultRequeueRejected;
		}

	}

}
