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

package org.springframework.boot.autoconfigure.graphql;

import java.time.Duration;
import java.util.Arrays;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@link ConfigurationProperties Properties} for Spring GraphQL.
 *
 * @author Brian Clozel
 * @since 2.7.0
 */
@ConfigurationProperties(prefix = "spring.graphql")
public class GraphQlProperties {

	/**
	 * Path at which to expose a GraphQL request HTTP endpoint.
	 */
	private String path = "/graphql";

	private final Graphiql graphiql = new Graphiql();

	private final Schema schema = new Schema();

	private final Websocket websocket = new Websocket();

	private final Rsocket rsocket = new Rsocket();

	/**
     * Returns the Graphiql instance.
     *
     * @return the Graphiql instance
     */
    public Graphiql getGraphiql() {
		return this.graphiql;
	}

	/**
     * Returns the path of the GraphQlProperties.
     *
     * @return the path of the GraphQlProperties
     */
    public String getPath() {
		return this.path;
	}

	/**
     * Sets the path for the GraphQL endpoint.
     * 
     * @param path the path to set
     */
    public void setPath(String path) {
		this.path = path;
	}

	/**
     * Returns the schema associated with this GraphQlProperties instance.
     *
     * @return the schema associated with this GraphQlProperties instance
     */
    public Schema getSchema() {
		return this.schema;
	}

	/**
     * Returns the WebSocket instance associated with this GraphQlProperties object.
     *
     * @return the WebSocket instance
     */
    public Websocket getWebsocket() {
		return this.websocket;
	}

	/**
     * Returns the RSocket instance associated with this GraphQlProperties object.
     *
     * @return the RSocket instance
     */
    public Rsocket getRsocket() {
		return this.rsocket;
	}

	/**
     * Schema class.
     */
    public static class Schema {

		/**
		 * Locations of GraphQL schema files.
		 */
		private String[] locations = new String[] { "classpath:graphql/**/" };

		/**
		 * File extensions for GraphQL schema files.
		 */
		private String[] fileExtensions = new String[] { ".graphqls", ".gqls" };

		private final Inspection inspection = new Inspection();

		private final Introspection introspection = new Introspection();

		private final Printer printer = new Printer();

		/**
         * Returns an array of locations.
         *
         * @return an array of locations
         */
        public String[] getLocations() {
			return this.locations;
		}

		/**
         * Sets the locations for the Schema.
         * 
         * @param locations an array of locations to be set
         */
        public void setLocations(String[] locations) {
			this.locations = appendSlashIfNecessary(locations);
		}

		/**
         * Returns an array of file extensions supported by the schema.
         *
         * @return an array of file extensions
         */
        public String[] getFileExtensions() {
			return this.fileExtensions;
		}

		/**
         * Sets the file extensions for the Schema.
         * 
         * @param fileExtensions an array of file extensions to be set
         */
        public void setFileExtensions(String[] fileExtensions) {
			this.fileExtensions = fileExtensions;
		}

		/**
         * Appends a slash to each location in the given array if it is not already present.
         * 
         * @param locations the array of locations to be processed
         * @return an array of locations with a slash appended to each location if necessary
         */
        private String[] appendSlashIfNecessary(String[] locations) {
			return Arrays.stream(locations)
				.map((location) -> location.endsWith("/") ? location : location + "/")
				.toArray(String[]::new);
		}

		/**
         * Returns the inspection object associated with this Schema.
         * 
         * @return the inspection object
         */
        public Inspection getInspection() {
			return this.inspection;
		}

		/**
         * Returns the Introspection object associated with this Schema.
         *
         * @return the Introspection object associated with this Schema
         */
        public Introspection getIntrospection() {
			return this.introspection;
		}

		/**
         * Returns the printer object associated with this Schema.
         *
         * @return the printer object
         */
        public Printer getPrinter() {
			return this.printer;
		}

		/**
         * Inspection class.
         */
        public static class Inspection {

			/**
			 * Whether schema should be compared to the application to detect missing
			 * mappings.
			 */
			private boolean enabled = true;

			/**
             * Returns the current status of the enabled flag.
             *
             * @return true if the enabled flag is set, false otherwise.
             */
            public boolean isEnabled() {
				return this.enabled;
			}

			/**
             * Sets the enabled status of the Inspection.
             * 
             * @param enabled the enabled status to be set
             */
            public void setEnabled(boolean enabled) {
				this.enabled = enabled;
			}

		}

		/**
         * Introspection class.
         */
        public static class Introspection {

			/**
			 * Whether field introspection should be enabled at the schema level.
			 */
			private boolean enabled = true;

			/**
             * Returns the current state of the enabled flag.
             *
             * @return true if the flag is enabled, false otherwise.
             */
            public boolean isEnabled() {
				return this.enabled;
			}

			/**
             * Sets the enabled status of the Introspection.
             * 
             * @param enabled the enabled status to be set
             */
            public void setEnabled(boolean enabled) {
				this.enabled = enabled;
			}

		}

		/**
         * Printer class.
         */
        public static class Printer {

			/**
			 * Whether the endpoint that prints the schema is enabled. Schema is available
			 * under spring.graphql.path + "/schema".
			 */
			private boolean enabled = false;

			/**
             * Returns the current status of the printer.
             * 
             * @return true if the printer is enabled, false otherwise.
             */
            public boolean isEnabled() {
				return this.enabled;
			}

			/**
             * Sets the enabled status of the printer.
             * 
             * @param enabled the new enabled status of the printer
             */
            public void setEnabled(boolean enabled) {
				this.enabled = enabled;
			}

		}

	}

	/**
     * Graphiql class.
     */
    public static class Graphiql {

		/**
		 * Path to the GraphiQL UI endpoint.
		 */
		private String path = "/graphiql";

		/**
		 * Whether the default GraphiQL UI is enabled.
		 */
		private boolean enabled = false;

		/**
         * Returns the path of the Graphiql instance.
         *
         * @return the path of the Graphiql instance
         */
        public String getPath() {
			return this.path;
		}

		/**
         * Sets the path for the Graphiql class.
         * 
         * @param path the path to be set
         */
        public void setPath(String path) {
			this.path = path;
		}

		/**
         * Returns the current status of the enabled flag.
         * 
         * @return true if the enabled flag is set to true, false otherwise.
         */
        public boolean isEnabled() {
			return this.enabled;
		}

		/**
         * Sets the enabled status of the Graphiql.
         * 
         * @param enabled the enabled status to be set
         */
        public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

	}

	/**
     * Websocket class.
     */
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
         * Returns the path of the WebSocket connection.
         * 
         * @return the path of the WebSocket connection
         */
        public String getPath() {
			return this.path;
		}

		/**
         * Sets the path for the WebSocket connection.
         * 
         * @param path the path to set for the WebSocket connection
         */
        public void setPath(String path) {
			this.path = path;
		}

		/**
         * Returns the connection initialization timeout.
         *
         * @return the connection initialization timeout
         */
        public Duration getConnectionInitTimeout() {
			return this.connectionInitTimeout;
		}

		/**
         * Sets the connection initialization timeout.
         * 
         * @param connectionInitTimeout the duration to wait for the connection to initialize
         */
        public void setConnectionInitTimeout(Duration connectionInitTimeout) {
			this.connectionInitTimeout = connectionInitTimeout;
		}

	}

	/**
     * Rsocket class.
     */
    public static class Rsocket {

		/**
		 * Mapping of the RSocket message handler.
		 */
		private String mapping;

		/**
         * Returns the mapping value of the Rsocket object.
         *
         * @return the mapping value of the Rsocket object
         */
        public String getMapping() {
			return this.mapping;
		}

		/**
         * Sets the mapping for the Rsocket.
         * 
         * @param mapping the mapping to be set
         */
        public void setMapping(String mapping) {
			this.mapping = mapping;
		}

	}

}
