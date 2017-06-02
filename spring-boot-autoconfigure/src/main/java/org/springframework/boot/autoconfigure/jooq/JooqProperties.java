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

package org.springframework.boot.autoconfigure.jooq;

import javax.sql.DataSource;

import org.jooq.SQLDialect;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the JOOQ database library.
 *
 * @author Andreas Ahlenstorf
 * @author Michael Simons
 * @since 1.3.0
 */
@ConfigurationProperties(prefix = "spring.jooq")
public class JooqProperties {

	/**
	 * SQL dialect to use, auto-detected by default.
	 */
	private SQLDialect sqlDialect;

	public SQLDialect getSqlDialect() {
		return this.sqlDialect;
	}

	public void setSqlDialect(SQLDialect sqlDialect) {
		this.sqlDialect = sqlDialect;
	}

	/**
	 * Determine the {@link SQLDialect} to use based on this configuration and the primary
	 * {@link DataSource}.
	 * @param dataSource the data source
	 * @return the {@code SQLDialect} to use for that {@link DataSource}
	 */
	public SQLDialect determineSqlDialect(DataSource dataSource) {
		if (this.sqlDialect != null) {
			return this.sqlDialect;
		}
		return SqlDialectLookup.getDialect(dataSource);
	}

}
