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

package org.springframework.boot.autoconfigure.graphql;

import java.util.Arrays;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@link ConfigurationProperties properties} for Spring GraphQL.
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

	private final Schema schema = new Schema();

	public String getPath() {
		return this.path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public Schema getSchema() {
		return this.schema;
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

		private String[] appendSlashIfNecessary(String[] locations) {
			return Arrays.stream(locations).map((location) -> location.endsWith("/") ? location : location + "/")
					.toArray(String[]::new);
		}

		public Printer getPrinter() {
			return this.printer;
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

}
