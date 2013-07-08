/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.autoconfigure.jdbc;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.util.StringUtils;

/**
 * Base class for configuration of a database pool.
 * 
 * @author Dave Syer
 */
public class AbstractDataSourceConfiguration {

	// TODO: add pool parameters

	@Value("${spring.database.driverClassName:}")
	private String driverClassName;

	@Value("${spring.database.url:}")
	private String url;

	@Value("${spring.database.username:sa}")
	private String username;

	@Value("${spring.database.password:}")
	private String password;

	protected String getDriverClassName() {
		if (StringUtils.hasText(this.driverClassName)) {
			return this.driverClassName;
		}
		EmbeddedDatabaseType embeddedDatabaseType = EmbeddedDatabaseConfiguration
				.getEmbeddedDatabaseType();
		this.driverClassName = EmbeddedDatabaseConfiguration
				.getEmbeddedDatabaseDriverClass(embeddedDatabaseType);
		if (!StringUtils.hasText(this.driverClassName)) {
			throw new BeanCreationException(
					"Cannot determine embedded database driver class for database type "
							+ embeddedDatabaseType
							+ ". If you want an embedded database please put a supoprted one on the classpath.");
		}
		return this.driverClassName;
	}

	protected String getUrl() {
		if (StringUtils.hasText(this.url)) {
			return this.url;
		}
		EmbeddedDatabaseType embeddedDatabaseType = EmbeddedDatabaseConfiguration
				.getEmbeddedDatabaseType();
		this.url = EmbeddedDatabaseConfiguration
				.getEmbeddedDatabaseUrl(embeddedDatabaseType);
		if (!StringUtils.hasText(this.url)) {
			throw new BeanCreationException(
					"Cannot determine embedded database url for database type "
							+ embeddedDatabaseType
							+ ". If you want an embedded database please put a supported on on the classpath.");
		}
		return this.url;
	}

	protected String getUsername() {
		return this.username;
	}

	protected String getPassword() {
		return this.password;
	}
}
