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

package org.springframework.boot.autoconfigure.integration;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceInitializationMode;

/**
 * Configuration properties for Spring Integration.
 *
 * @author Vedran Pavic
 * @author Stephane Nicoll
 * @author Artem Bilan
 * @since 2.0.0
 */
@ConfigurationProperties(prefix = "spring.integration")
public class IntegrationProperties {

	private final Channel channel = new Channel();

	private final Endpoint endpoint = new Endpoint();

	private final Error error = new Error();

	private final Jdbc jdbc = new Jdbc();

	private final RSocket rsocket = new RSocket();

	public Channel getChannel() {
		return this.channel;
	}

	public Endpoint getEndpoint() {
		return this.endpoint;
	}

	public Error getError() {
		return this.error;
	}

	public Jdbc getJdbc() {
		return this.jdbc;
	}

	public RSocket getRsocket() {
		return this.rsocket;
	}

	public static class Channel {

		/**
		 * Whether to create input channels if necessary.
		 */
		private boolean autoCreate = true;

		/**
		 * Default number of subscribers allowed on, for example, a 'DirectChannel'.
		 */
		private int maxUnicastSubscribers = Integer.MAX_VALUE;

		/**
		 * Default number of subscribers allowed on, for example, a
		 * 'PublishSubscribeChannel'.
		 */
		private int maxBroadcastSubscribers = Integer.MAX_VALUE;

		public void setAutoCreate(boolean autoCreate) {
			this.autoCreate = autoCreate;
		}

		public boolean isAutoCreate() {
			return this.autoCreate;
		}

		public void setMaxUnicastSubscribers(int maxUnicastSubscribers) {
			this.maxUnicastSubscribers = maxUnicastSubscribers;
		}

		public int getMaxUnicastSubscribers() {
			return this.maxUnicastSubscribers;
		}

		public void setMaxBroadcastSubscribers(int maxBroadcastSubscribers) {
			this.maxBroadcastSubscribers = maxBroadcastSubscribers;
		}

		public int getMaxBroadcastSubscribers() {
			return this.maxBroadcastSubscribers;
		}

	}

	public static class Endpoint {

		/**
		 * Whether to throw an exception when a reply is not expected anymore by a
		 * gateway.
		 */
		private boolean throwExceptionOnLateReply = false;

		/**
		 * A comma-separated list of message header names that should not be populated
		 * into Message instances during a header copying operation.
		 */
		private List<String> readOnlyHeaders = new ArrayList<>();

		/**
		 * A comma-separated list of endpoint bean names patterns that should not be
		 * started automatically during application startup.
		 */
		private List<String> noAutoStartup = new ArrayList<>();

		public void setThrowExceptionOnLateReply(boolean throwExceptionOnLateReply) {
			this.throwExceptionOnLateReply = throwExceptionOnLateReply;
		}

		public boolean isThrowExceptionOnLateReply() {
			return this.throwExceptionOnLateReply;
		}

		public List<String> getReadOnlyHeaders() {
			return this.readOnlyHeaders;
		}

		public void setReadOnlyHeaders(List<String> readOnlyHeaders) {
			this.readOnlyHeaders = readOnlyHeaders;
		}

		public List<String> getNoAutoStartup() {
			return this.noAutoStartup;
		}

		public void setNoAutoStartup(List<String> noAutoStartup) {
			this.noAutoStartup = noAutoStartup;
		}

	}

	public static class Error {

		/**
		 * Whether to not silently ignore messages on the global 'errorChannel' when they
		 * are no subscribers.
		 */
		private boolean requireSubscribers = true;

		/**
		 * Whether to ignore failures for one or more of the handlers of the global
		 * 'errorChannel'.
		 */
		private boolean ignoreFailures = true;

		public boolean isRequireSubscribers() {
			return this.requireSubscribers;
		}

		public void setRequireSubscribers(boolean requireSubscribers) {
			this.requireSubscribers = requireSubscribers;
		}

		public boolean isIgnoreFailures() {
			return this.ignoreFailures;
		}

		public void setIgnoreFailures(boolean ignoreFailures) {
			this.ignoreFailures = ignoreFailures;
		}

	}

	public static class Jdbc {

		private static final String DEFAULT_SCHEMA_LOCATION = "classpath:org/springframework/"
				+ "integration/jdbc/schema-@@platform@@.sql";

		/**
		 * Path to the SQL file to use to initialize the database schema.
		 */
		private String schema = DEFAULT_SCHEMA_LOCATION;

		/**
		 * Database schema initialization mode.
		 */
		private DataSourceInitializationMode initializeSchema = DataSourceInitializationMode.EMBEDDED;

		public String getSchema() {
			return this.schema;
		}

		public void setSchema(String schema) {
			this.schema = schema;
		}

		public DataSourceInitializationMode getInitializeSchema() {
			return this.initializeSchema;
		}

		public void setInitializeSchema(DataSourceInitializationMode initializeSchema) {
			this.initializeSchema = initializeSchema;
		}

	}

	public static class RSocket {

		private final Client client = new Client();

		private final Server server = new Server();

		public Client getClient() {
			return this.client;
		}

		public Server getServer() {
			return this.server;
		}

		public static class Client {

			/**
			 * TCP RSocket server host to connect to.
			 */
			private String host;

			/**
			 * TCP RSocket server port to connect to.
			 */
			private Integer port;

			/**
			 * WebSocket RSocket server uri to connect to.
			 */
			private URI uri;

			public void setHost(String host) {
				this.host = host;
			}

			public String getHost() {
				return this.host;
			}

			public void setPort(Integer port) {
				this.port = port;
			}

			public Integer getPort() {
				return this.port;
			}

			public void setUri(URI uri) {
				this.uri = uri;
			}

			public URI getUri() {
				return this.uri;
			}

		}

		public static class Server {

			/**
			 * Whether to handle message mapping for RSocket via Spring Integration.
			 */
			private boolean messageMappingEnabled;

			public boolean isMessageMappingEnabled() {
				return this.messageMappingEnabled;
			}

			public void setMessageMappingEnabled(boolean messageMappingEnabled) {
				this.messageMappingEnabled = messageMappingEnabled;
			}

		}

	}

}
