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
 * @since 1.5.0
 */
@ConfigurationProperties(prefix = "spring.jdbc")
public class JdbcProperties {

	/**
	 * Settings for JdbcTemplate.
	 */
	private final Template template = new Template();

	public Template getTemplate() {
		return this.template;
	}

	/**
	 * {@link org.springframework.jdbc.core.JdbcTemplate} settings.
	 */
	public static class Template {

		/**
		 * Fetch size.
		 * @see org.springframework.jdbc.core.JdbcTemplate#fetchSize
		 */
		private Integer fetchSize;

		/**
		 * Max row count.
		 * @see org.springframework.jdbc.core.JdbcTemplate#maxRows
		 */
		private Integer maxRows;

		/**
		 * Query timeout in seconds.
		 * @see org.springframework.jdbc.core.JdbcTemplate#queryTimeout
		 */
		private Integer queryTimeout;

		public Integer getFetchSize() {
			return this.fetchSize;
		}

		public void setFetchSize(Integer fetchSize) {
			this.fetchSize = fetchSize;
		}

		public Integer getMaxRows() {
			return this.maxRows;
		}

		public void setMaxRows(Integer maxRows) {
			this.maxRows = maxRows;
		}

		public Integer getQueryTimeout() {
			return this.queryTimeout;
		}

		public void setQueryTimeout(Integer queryTimeout) {
			this.queryTimeout = queryTimeout;
		}
	}

}
