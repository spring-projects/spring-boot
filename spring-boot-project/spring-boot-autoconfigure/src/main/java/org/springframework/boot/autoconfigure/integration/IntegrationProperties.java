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

package org.springframework.boot.autoconfigure.integration;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.sql.init.DatabaseInitializationMode;

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

	private final Poller poller = new Poller();

	private final Management management = new Management();

	/**
     * Returns the channel associated with this IntegrationProperties object.
     *
     * @return the channel associated with this IntegrationProperties object
     */
    public Channel getChannel() {
		return this.channel;
	}

	/**
     * Returns the endpoint of the IntegrationProperties.
     *
     * @return the endpoint of the IntegrationProperties
     */
    public Endpoint getEndpoint() {
		return this.endpoint;
	}

	/**
     * Returns the error associated with the IntegrationProperties object.
     *
     * @return the error associated with the IntegrationProperties object
     */
    public Error getError() {
		return this.error;
	}

	/**
     * Returns the Jdbc object associated with this IntegrationProperties instance.
     *
     * @return the Jdbc object
     */
    public Jdbc getJdbc() {
		return this.jdbc;
	}

	/**
     * Returns the RSocket object.
     *
     * @return the RSocket object
     */
    public RSocket getRsocket() {
		return this.rsocket;
	}

	/**
     * Returns the Poller object associated with this IntegrationProperties instance.
     *
     * @return the Poller object
     */
    public Poller getPoller() {
		return this.poller;
	}

	/**
     * Returns the management object associated with this IntegrationProperties instance.
     *
     * @return the management object
     */
    public Management getManagement() {
		return this.management;
	}

	/**
     * Channel class.
     */
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

		/**
         * Sets the flag indicating whether the channel should automatically create new instances.
         * 
         * @param autoCreate the flag indicating whether the channel should automatically create new instances
         */
        public void setAutoCreate(boolean autoCreate) {
			this.autoCreate = autoCreate;
		}

		/**
         * Returns a boolean value indicating whether auto creation is enabled for the channel.
         * 
         * @return true if auto creation is enabled, false otherwise
         */
        public boolean isAutoCreate() {
			return this.autoCreate;
		}

		/**
         * Sets the maximum number of unicast subscribers for the channel.
         * 
         * @param maxUnicastSubscribers the maximum number of unicast subscribers to set
         */
        public void setMaxUnicastSubscribers(int maxUnicastSubscribers) {
			this.maxUnicastSubscribers = maxUnicastSubscribers;
		}

		/**
         * Returns the maximum number of unicast subscribers allowed for this channel.
         *
         * @return the maximum number of unicast subscribers
         */
        public int getMaxUnicastSubscribers() {
			return this.maxUnicastSubscribers;
		}

		/**
         * Sets the maximum number of subscribers allowed for a broadcast in this channel.
         * 
         * @param maxBroadcastSubscribers the maximum number of subscribers allowed for a broadcast
         */
        public void setMaxBroadcastSubscribers(int maxBroadcastSubscribers) {
			this.maxBroadcastSubscribers = maxBroadcastSubscribers;
		}

		/**
         * Returns the maximum number of subscribers allowed for broadcasting.
         *
         * @return the maximum number of subscribers allowed for broadcasting
         */
        public int getMaxBroadcastSubscribers() {
			return this.maxBroadcastSubscribers;
		}

	}

	/**
     * Endpoint class.
     */
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

		/**
         * Sets whether an exception should be thrown if a late reply is received.
         * 
         * @param throwExceptionOnLateReply true if an exception should be thrown, false otherwise
         */
        public void setThrowExceptionOnLateReply(boolean throwExceptionOnLateReply) {
			this.throwExceptionOnLateReply = throwExceptionOnLateReply;
		}

		/**
         * Returns a boolean value indicating whether an exception should be thrown on a late reply.
         * 
         * @return true if an exception should be thrown on a late reply, false otherwise
         */
        public boolean isThrowExceptionOnLateReply() {
			return this.throwExceptionOnLateReply;
		}

		/**
         * Returns a list of read-only headers.
         * 
         * @return the list of read-only headers
         */
        public List<String> getReadOnlyHeaders() {
			return this.readOnlyHeaders;
		}

		/**
         * Sets the list of read-only headers for the endpoint.
         * 
         * @param readOnlyHeaders the list of read-only headers to be set
         */
        public void setReadOnlyHeaders(List<String> readOnlyHeaders) {
			this.readOnlyHeaders = readOnlyHeaders;
		}

		/**
         * Returns a list of strings representing the endpoints that are not set to auto-startup.
         *
         * @return a list of strings representing the endpoints that are not set to auto-startup
         */
        public List<String> getNoAutoStartup() {
			return this.noAutoStartup;
		}

		/**
         * Sets the list of endpoints that should not be started automatically.
         * 
         * @param noAutoStartup the list of endpoints to exclude from automatic startup
         */
        public void setNoAutoStartup(List<String> noAutoStartup) {
			this.noAutoStartup = noAutoStartup;
		}

	}

	/**
     * Error class.
     */
    public static class Error {

		/**
		 * Whether to not silently ignore messages on the global 'errorChannel' when there
		 * are no subscribers.
		 */
		private boolean requireSubscribers = true;

		/**
		 * Whether to ignore failures for one or more of the handlers of the global
		 * 'errorChannel'.
		 */
		private boolean ignoreFailures = true;

		/**
         * Returns a boolean value indicating whether subscribers are required.
         * 
         * @return true if subscribers are required, false otherwise
         */
        public boolean isRequireSubscribers() {
			return this.requireSubscribers;
		}

		/**
         * Sets whether subscribers are required for this error.
         * 
         * @param requireSubscribers true if subscribers are required, false otherwise
         */
        public void setRequireSubscribers(boolean requireSubscribers) {
			this.requireSubscribers = requireSubscribers;
		}

		/**
         * Returns a boolean value indicating whether failures should be ignored.
         * 
         * @return true if failures should be ignored, false otherwise
         */
        public boolean isIgnoreFailures() {
			return this.ignoreFailures;
		}

		/**
         * Sets whether to ignore failures.
         * 
         * @param ignoreFailures true to ignore failures, false otherwise
         */
        public void setIgnoreFailures(boolean ignoreFailures) {
			this.ignoreFailures = ignoreFailures;
		}

	}

	/**
     * Jdbc class.
     */
    public static class Jdbc {

		private static final String DEFAULT_SCHEMA_LOCATION = "classpath:org/springframework/"
				+ "integration/jdbc/schema-@@platform@@.sql";

		/**
		 * Path to the SQL file to use to initialize the database schema.
		 */
		private String schema = DEFAULT_SCHEMA_LOCATION;

		/**
		 * Platform to use in initialization scripts if the @@platform@@ placeholder is
		 * used. Auto-detected by default.
		 */
		private String platform;

		/**
		 * Database schema initialization mode.
		 */
		private DatabaseInitializationMode initializeSchema = DatabaseInitializationMode.EMBEDDED;

		/**
         * Returns the schema of the Jdbc object.
         *
         * @return the schema of the Jdbc object
         */
        public String getSchema() {
			return this.schema;
		}

		/**
         * Sets the schema for the JDBC connection.
         * 
         * @param schema the name of the schema to be set
         */
        public void setSchema(String schema) {
			this.schema = schema;
		}

		/**
         * Returns the platform of the JDBC connection.
         * 
         * @return the platform of the JDBC connection
         */
        public String getPlatform() {
			return this.platform;
		}

		/**
         * Sets the platform for the JDBC connection.
         * 
         * @param platform the platform to be set
         */
        public void setPlatform(String platform) {
			this.platform = platform;
		}

		/**
         * Returns the initialization mode for the database schema.
         * 
         * @return the initialization mode for the database schema
         */
        public DatabaseInitializationMode getInitializeSchema() {
			return this.initializeSchema;
		}

		/**
         * Sets the mode for initializing the database schema.
         * 
         * @param initializeSchema the mode for initializing the database schema
         */
        public void setInitializeSchema(DatabaseInitializationMode initializeSchema) {
			this.initializeSchema = initializeSchema;
		}

	}

	/**
     * RSocket class.
     */
    public static class RSocket {

		private final Client client = new Client();

		private final Server server = new Server();

		/**
         * Returns the client associated with this RSocket.
         *
         * @return the client associated with this RSocket
         */
        public Client getClient() {
			return this.client;
		}

		/**
         * Returns the server associated with this RSocket.
         *
         * @return the server associated with this RSocket
         */
        public Server getServer() {
			return this.server;
		}

		/**
         * Client class.
         */
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

			/**
             * Sets the host for the client.
             * 
             * @param host the host to set
             */
            public void setHost(String host) {
				this.host = host;
			}

			/**
             * Returns the host of the client.
             *
             * @return the host of the client
             */
            public String getHost() {
				return this.host;
			}

			/**
             * Sets the port number for the client.
             * 
             * @param port the port number to be set
             */
            public void setPort(Integer port) {
				this.port = port;
			}

			/**
             * Returns the port number of the client.
             *
             * @return the port number of the client
             */
            public Integer getPort() {
				return this.port;
			}

			/**
             * Sets the URI for the client.
             * 
             * @param uri the URI to be set
             */
            public void setUri(URI uri) {
				this.uri = uri;
			}

			/**
             * Returns the URI associated with this Client.
             *
             * @return the URI associated with this Client
             */
            public URI getUri() {
				return this.uri;
			}

		}

		/**
         * Server class.
         */
        public static class Server {

			/**
			 * Whether to handle message mapping for RSocket through Spring Integration.
			 */
			private boolean messageMappingEnabled;

			/**
             * Returns a boolean value indicating whether the message mapping is enabled.
             * 
             * @return true if the message mapping is enabled, false otherwise
             */
            public boolean isMessageMappingEnabled() {
				return this.messageMappingEnabled;
			}

			/**
             * Sets the flag indicating whether message mapping is enabled.
             * 
             * @param messageMappingEnabled true if message mapping is enabled, false otherwise
             */
            public void setMessageMappingEnabled(boolean messageMappingEnabled) {
				this.messageMappingEnabled = messageMappingEnabled;
			}

		}

	}

	/**
     * Poller class.
     */
    public static class Poller {

		/**
		 * Maximum number of messages to poll per polling cycle.
		 */
		private int maxMessagesPerPoll = Integer.MIN_VALUE; // PollerMetadata.MAX_MESSAGES_UNBOUNDED

		/**
		 * How long to wait for messages on poll.
		 */
		private Duration receiveTimeout = Duration.ofSeconds(1); // PollerMetadata.DEFAULT_RECEIVE_TIMEOUT

		/**
		 * Polling delay period. Mutually exclusive with 'cron' and 'fixedRate'.
		 */
		private Duration fixedDelay;

		/**
		 * Polling rate period. Mutually exclusive with 'fixedDelay' and 'cron'.
		 */
		private Duration fixedRate;

		/**
		 * Polling initial delay. Applied for 'fixedDelay' and 'fixedRate'; ignored for
		 * 'cron'.
		 */
		private Duration initialDelay;

		/**
		 * Cron expression for polling. Mutually exclusive with 'fixedDelay' and
		 * 'fixedRate'.
		 */
		private String cron;

		/**
         * Returns the maximum number of messages that can be processed per poll.
         *
         * @return the maximum number of messages per poll
         */
        public int getMaxMessagesPerPoll() {
			return this.maxMessagesPerPoll;
		}

		/**
         * Sets the maximum number of messages to be processed per poll.
         * 
         * @param maxMessagesPerPoll the maximum number of messages to be processed per poll
         */
        public void setMaxMessagesPerPoll(int maxMessagesPerPoll) {
			this.maxMessagesPerPoll = maxMessagesPerPoll;
		}

		/**
         * Returns the receive timeout duration.
         *
         * @return the receive timeout duration
         */
        public Duration getReceiveTimeout() {
			return this.receiveTimeout;
		}

		/**
         * Sets the receive timeout for the Poller.
         * 
         * @param receiveTimeout the receive timeout duration to be set
         */
        public void setReceiveTimeout(Duration receiveTimeout) {
			this.receiveTimeout = receiveTimeout;
		}

		/**
         * Returns the fixed delay for the Poller.
         *
         * @return the fixed delay for the Poller
         */
        public Duration getFixedDelay() {
			return this.fixedDelay;
		}

		/**
         * Sets the fixed delay for the poller.
         * 
         * @param fixedDelay the fixed delay duration to be set
         */
        public void setFixedDelay(Duration fixedDelay) {
			this.fixedDelay = fixedDelay;
		}

		/**
         * Returns the fixed rate at which the poller is scheduled to run.
         *
         * @return the fixed rate at which the poller is scheduled to run
         */
        public Duration getFixedRate() {
			return this.fixedRate;
		}

		/**
         * Sets the fixed rate at which the poller should run.
         * 
         * @param fixedRate the fixed rate at which the poller should run
         */
        public void setFixedRate(Duration fixedRate) {
			this.fixedRate = fixedRate;
		}

		/**
         * Returns the initial delay for the Poller.
         *
         * @return the initial delay for the Poller
         */
        public Duration getInitialDelay() {
			return this.initialDelay;
		}

		/**
         * Sets the initial delay for the Poller.
         * 
         * @param initialDelay the initial delay to be set
         */
        public void setInitialDelay(Duration initialDelay) {
			this.initialDelay = initialDelay;
		}

		/**
         * Returns the cron expression used by the Poller.
         *
         * @return the cron expression
         */
        public String getCron() {
			return this.cron;
		}

		/**
         * Sets the cron expression for scheduling the poller.
         * 
         * @param cron the cron expression to be set
         */
        public void setCron(String cron) {
			this.cron = cron;
		}

	}

	/**
     * Management class.
     */
    public static class Management {

		/**
		 * Whether Spring Integration components should perform logging in the main
		 * message flow. When disabled, such logging will be skipped without checking the
		 * logging level. When enabled, such logging is controlled as normal by the
		 * logging system's log level configuration.
		 */
		private boolean defaultLoggingEnabled = true;

		/**
		 * Comma-separated list of simple patterns to match against the names of Spring
		 * Integration components. When matched, observation instrumentation will be
		 * performed for the component. Please refer to the javadoc of the smartMatch
		 * method of Spring Integration's PatternMatchUtils for details of the pattern
		 * syntax.
		 */
		private List<String> observationPatterns = new ArrayList<>();

		/**
         * Returns a boolean value indicating whether the default logging is enabled.
         * 
         * @return true if the default logging is enabled, false otherwise
         */
        public boolean isDefaultLoggingEnabled() {
			return this.defaultLoggingEnabled;
		}

		/**
         * Sets the default logging enabled flag.
         * 
         * @param defaultLoggingEnabled the flag indicating whether default logging is enabled or not
         */
        public void setDefaultLoggingEnabled(boolean defaultLoggingEnabled) {
			this.defaultLoggingEnabled = defaultLoggingEnabled;
		}

		/**
         * Returns the observation patterns.
         * 
         * @return the observation patterns as a list of strings
         */
        public List<String> getObservationPatterns() {
			return this.observationPatterns;
		}

		/**
         * Sets the observation patterns for the Management class.
         * 
         * @param observationPatterns the list of observation patterns to be set
         */
        public void setObservationPatterns(List<String> observationPatterns) {
			this.observationPatterns = observationPatterns;
		}

	}

}
