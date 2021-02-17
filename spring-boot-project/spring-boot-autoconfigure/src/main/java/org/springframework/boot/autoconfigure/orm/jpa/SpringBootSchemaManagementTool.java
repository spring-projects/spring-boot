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

package org.springframework.boot.autoconfigure.orm.jpa;

import java.util.Map;

import org.hibernate.tool.schema.internal.HibernateSchemaManagementTool;
import org.hibernate.tool.schema.spi.SchemaCreator;
import org.hibernate.tool.schema.spi.SchemaManagementTool;

import org.springframework.boot.autoconfigure.jdbc.DataSourceInitializer;

/**
 * Spring Boot {@link SchemaManagementTool}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class SpringBootSchemaManagementTool extends HibernateSchemaManagementTool {

	private final DataSourceInitializer dataSourceInitializer;

	SpringBootSchemaManagementTool(DataSourceInitializer dataSourceInitializer) {
		this.dataSourceInitializer = dataSourceInitializer;
	}

	@Override
	@SuppressWarnings("rawtypes")
	public SchemaCreator getSchemaCreator(Map options) {
		SchemaCreator creator = super.getSchemaCreator(options);
		return new SpringBootSchemaCreator(this, creator, this.dataSourceInitializer);
	}

}
