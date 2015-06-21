/*
 * Copyright 2012-2015 the original author or authors.
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

import org.jooq.SQLDialect;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the JOOQ database library.
 *
 * @author Andreas Ahlenstorf
 */
@ConfigurationProperties(prefix = "spring.datasource.jooq")
public class JooqProperties {

	private String sqlDialect;

	/**
	 * The {@link SQLDialect} JOOQ should use when communicating with the configured
	 * datasource, e.g. "POSTGRES".
	 */
	public String getSqlDialect() {
		return sqlDialect;
	}

	/**
	 * The {@link SQLDialect} JOOQ should use when communicating with the configured
	 * datasource, e.g. "POSTGRES".
	 */
	public void setSqlDialect(String sqlDialect) {
		this.sqlDialect = sqlDialect;
	}
}
