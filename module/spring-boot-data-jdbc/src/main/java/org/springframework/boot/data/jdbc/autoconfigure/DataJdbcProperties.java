/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.data.jdbc.autoconfigure;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Spring Data JDBC.
 *
 * @author Jens Schauder
 * @since 4.0.0
 */
@ConfigurationProperties("spring.data.jdbc")
public class DataJdbcProperties {

	/**
	 * Dialect to use. By default, the dialect is determined by inspecting the database
	 * connection.
	 */
	private @Nullable DataJdbcDatabaseDialect dialect;

	public @Nullable DataJdbcDatabaseDialect getDialect() {
		return this.dialect;
	}

	public void setDialect(@Nullable DataJdbcDatabaseDialect dialect) {
		this.dialect = dialect;
	}

}
