/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.autoconfigure.graphql;

import java.time.Duration;
import java.util.Arrays;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

/**
 * {@link ConfigurationProperties Properties} for Spring GraphQL.
 *
 * @author Brian Clozel
 * @since 2.7.0
 */
@ConfigurationProperties("spring.graphql")
public class GraphQlProperties {

	/**
	 * Path at which to expose a GraphQL request HTTP endpoint.
	 */
	private String path = "/graphql";

	private final Graphiql graphiql = new Graphiql();

	private final Schema schema = new Schema();

	private final Websocket websocket = new Websocket();

	private final Rsocket rsocket = new Rsocket();

	private final Sse sse = new Sse();

	public Graphiql getGraphiql() {
		return this.graphiql;
	}

	public String getPath() {
		return this.path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public Schema getSchema() {
		return this.schema;
	}

	public Websocket getWebsocket() {
		return this.websocket;
	}

	public Rsocket getRsocket() {
		return this.rsocket;
	}

	public Sse getSse() {
		return this.sse;
	}

	public static class Schema {

		/**
		 * Locations of GraphQL schema files.
		 */
		private String[] locations = new String[] { "classpath:graphql/**/" };

		/**
		 * File extensions for GraphQL schema files.
		 */
		private String[] fileExtensions = new String[] { ".graphqls", ".gqls" };

		/**
		 * Locations of additional, individual schema files to parse.
		 */
		private Resource[] additionalFiles = new Resource[0];

		private final Inspection inspection = new Inspection();

		private final Introspection introspection = new Introspection();

		private final Printer printer = new Printer();

		public String[] getLocations() {
			return this.locations;
		}

		public void setLocations(String[] locations) {
			this.locations = appendSlashIfNecessary(locations);
		}

		public String[] getFileExtensions() {
			return this.fileExtensions;
		}

		public void setFileExtensions(String[] fileExtensions) {
			this.fileExtensions = fileExtensions;
		}

		public Resource[] getAdditionalFiles() {
			return this.additionalFiles;
		}

		public void setAdditionalFiles(Resource[] additionalFiles) {
			this.additionalFiles = additionalFiles;
		}

		private String[] appendSlashIfNecessary(String[] locations) {
			return Arrays.stream(locations)
				.map((location) -> location.endsWith("/") ? location : location + "/")
				.toArray(String[]::new);
		}

		public Inspection getInspection() {
			return this.inspection;
		}

		public Introspection getIntrospection() {
			return this.introspection;
		}

		public Printer getPrinter() {
			return this.printer;
		}

		public static class Inspection {

			/**
			 * Whether schema should be compared to the application to detect missing
			 * mappings.
			 */
			private boolean enabled = true;

			public boolean isEnabled() {
				return this.enabled;
			}

			public void setEnabled(boolean enabled) {
				this.enabled = enabled;
			}

		}

		public static class Introspection {

			/**
			 * Whether field introspection should be enabled at the schema level.
			 */
			private boolean enabled = true;

			public boolean isEnabled() {
				return this.enabled;
			}

			public void setEnabled(boolean enabled) {
				this.enabled = enabled;
			}

		}

		public static class Printer {

			/**
			 * Whether the endpoint that prints the schema is enabled. Schema is available
			 * under spring.graphql.path + "/schema".
			 */
			private boolean enabled = false;

			public boolean isEnabled() {
				return this.enabled;
			}

			public void setEnabled(boolean enabled) {
				this.enabled = enabled;
			}

		}

	}

	public static class Graphiql {

		/**
		 * Path to the GraphiQL UI endpoint.
		 */
		private String path = "/graphiql";

		/**
		 * Whether the default GraphiQL UI is enabled.
		 */
		private boolean enabled = false;

		public String getPath() {
			return this.path;
		}

		public void setPath(String path) {
			this.path = path;
		}

		public boolean isEnabled() {
			return this.enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

	}

	public static class Websocket {

		/**
		 * Path of the GraphQL WebSocket subscription endpoint.
		 */
		private String path;

		/**
		 * Time within which the initial {@code CONNECTION_INIT} type message must be
		 * received.
		 */
		private Duration connectionInitTimeout = Duration.ofSeconds(60);

		/**
		 * Maximum idle period before a server keep-alive ping is sent to client.
		 */
		private Duration keepAlive;

		public String getPath() {
			return this.path;
		}

		public void setPath(String path) {
			this.path = path;
		}

		public Duration getConnectionInitTimeout() {
			return this.connectionInitTimeout;
		}

		public void setConnectionInitTimeout(Duration connectionInitTimeout) {
			this.connectionInitTimeout = connectionInitTimeout;
		}

		public Duration getKeepAlive() {
			return this.keepAlive;
		}

		public void setKeepAlive(Duration keepAlive) {
			this.keepAlive = keepAlive;
		}

	}

	public static class Rsocket {

		/**
		 * Mapping of the RSocket message handler.
		 */
		private String mapping;

		public String getMapping() {
			return this.mapping;
		}

		public void setMapping(String mapping) {
			this.mapping = mapping;
		}

	}

	public static class Sse {

		/**
		 * Time required for concurrent handling to complete.
		 */
		private Duration timeout;

		public Duration getTimeout() {
			return this.timeout;
		}

		public void setTimeout(Duration timeout) {
			this.timeout = timeout;
		}

	}

}
