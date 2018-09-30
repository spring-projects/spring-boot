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

package org.springframework.boot.autoconfigure.integration;

import javax.sql.DataSource;

import org.springframework.boot.jdbc.AbstractDataSourceInitializer;
import org.springframework.boot.jdbc.DataSourceInitializationMode;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;

/**
 * Initializer for Spring Integration schema.
 *
 * @author Vedran Pavic
 * @since 2.0.0
 */
public class IntegrationDataSourceInitializer extends AbstractDataSourceInitializer {

	private final IntegrationProperties.Jdbc properties;

	public IntegrationDataSourceInitializer(DataSource dataSource,
			ResourceLoader resourceLoader, IntegrationProperties properties) {
		super(dataSource, resourceLoader);
		Assert.notNull(properties, "IntegrationProperties must not be null");
		this.properties = properties.getJdbc();
	}

	@Override
	protected DataSourceInitializationMode getMode() {
		return this.properties.getInitializeSchema();
	}

	@Override
	protected String getSchemaLocation() {
		return this.properties.getSchema();
	}

}
