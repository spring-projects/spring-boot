/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.jdbc;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for JDBC.
 *
 * @author Kazuki Shimizu
 * @author Stephane Nicoll
 * @since 2.0.0
 */
@ConfigurationProperties(prefix = "spring.jdbc")
public class JdbcProperties {

	private final Template template = new Template();

	public Template getTemplate() {
		return this.template;
	}

	/**
	 * {@code JdbcTemplate} settings.
	 */
	public static class Template {

		/**
		 * Number of rows that should be fetched from the database when more rows are
		 * needed. Use -1 to use the JDBC driver's default configuration.
		 */
		private int fetchSize = -1;

		/**
		 * Maximum number of rows. Use -1 to use the JDBC driver's default configuration.
		 */
		private int maxRows = -1;

		/**
		 * Query timeout in seconds. Use -1 to use the JDBC driver's default
		 * configuration.
		 */
		private int queryTimeout = -1;

		public int getFetchSize() {
			return this.fetchSize;
		}

		public void setFetchSize(int fetchSize) {
			this.fetchSize = fetchSize;
		}

		public int getMaxRows() {
			return this.maxRows;
		}

		public void setMaxRows(int maxRows) {
			this.maxRows = maxRows;
		}

		public int getQueryTimeout() {
			return this.queryTimeout;
		}

		public void setQueryTimeout(int queryTimeout) {
			this.queryTimeout = queryTimeout;
		}

	}

}
