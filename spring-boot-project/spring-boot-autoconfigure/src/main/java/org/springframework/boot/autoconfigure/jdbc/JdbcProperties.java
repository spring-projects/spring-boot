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

package org.springframework.boot.autoconfigure.jdbc;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;

/**
 * Configuration properties for JDBC.
 *
 * @author Kazuki Shimizu
 * @author Stephane Nicoll
 * @author Yanming Zhou
 * @since 2.0.0
 */
@ConfigurationProperties("spring.jdbc")
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
		 * Query timeout. Default is to use the JDBC driver's default configuration. If a
		 * duration suffix is not specified, seconds will be used.
		 */
		@DurationUnit(ChronoUnit.SECONDS)
		private Duration queryTimeout;

		/**
		 * If this variable is {@code false}, we will throw exceptions on SQL warnings.
		 */
		private boolean ignoreWarnings = true;

		/**
		 * If this variable is set to true, then all results checking will be bypassed for
		 * any callable statement processing. This can be used to avoid a bug in some
		 * older Oracle JDBC drivers like 10.1.0.2.
		 */
		private boolean skipResultsProcessing;

		/**
		 * If this variable is set to true then all results from a stored procedure call
		 * that don't have a corresponding SqlOutParameter declaration will be bypassed.
		 * All other results processing will be take place unless the variable
		 * {@code skipResultsProcessing} is set to {@code true}.
		 */
		private boolean skipUndeclaredResults;

		/**
		 * If this variable is set to true then execution of a CallableStatement will
		 * return the results in a Map that uses case-insensitive names for the
		 * parameters.
		 */
		private boolean resultsMapCaseInsensitive;

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

		public Duration getQueryTimeout() {
			return this.queryTimeout;
		}

		public void setQueryTimeout(Duration queryTimeout) {
			this.queryTimeout = queryTimeout;
		}

		public boolean isIgnoreWarnings() {
			return this.ignoreWarnings;
		}

		public void setIgnoreWarnings(boolean ignoreWarnings) {
			this.ignoreWarnings = ignoreWarnings;
		}

		public boolean isSkipResultsProcessing() {
			return this.skipResultsProcessing;
		}

		public void setSkipResultsProcessing(boolean skipResultsProcessing) {
			this.skipResultsProcessing = skipResultsProcessing;
		}

		public boolean isSkipUndeclaredResults() {
			return this.skipUndeclaredResults;
		}

		public void setSkipUndeclaredResults(boolean skipUndeclaredResults) {
			this.skipUndeclaredResults = skipUndeclaredResults;
		}

		public boolean isResultsMapCaseInsensitive() {
			return this.resultsMapCaseInsensitive;
		}

		public void setResultsMapCaseInsensitive(boolean resultsMapCaseInsensitive) {
			this.resultsMapCaseInsensitive = resultsMapCaseInsensitive;
		}

	}

}
