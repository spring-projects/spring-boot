/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.autoconfigure.liquibase;

import java.util.stream.StreamSupport;

import javax.sql.DataSource;

import liquibase.integration.spring.SpringLiquibase;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.jdbc.SchemaManagement;
import org.springframework.boot.jdbc.SchemaManagementProvider;

/**
 * A Liquibase {@link SchemaManagementProvider} that determines if the schema is managed
 * by looking at available {@link SpringLiquibase} instances.
 *
 * @author Stephane Nicoll
 */
class LiquibaseSchemaManagementProvider implements SchemaManagementProvider {

	private final Iterable<SpringLiquibase> liquibaseInstances;

	LiquibaseSchemaManagementProvider(ObjectProvider<SpringLiquibase> liquibases) {
		this.liquibaseInstances = liquibases;
	}

	@Override
	public SchemaManagement getSchemaManagement(DataSource dataSource) {
		return StreamSupport.stream(this.liquibaseInstances.spliterator(), false)
				.map(SpringLiquibase::getDataSource).filter(dataSource::equals)
				.findFirst().map((managedDataSource) -> SchemaManagement.MANAGED)
				.orElse(SchemaManagement.UNMANAGED);
	}

}
