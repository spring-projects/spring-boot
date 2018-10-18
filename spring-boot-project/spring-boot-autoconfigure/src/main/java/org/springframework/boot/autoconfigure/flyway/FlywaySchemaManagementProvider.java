/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.autoconfigure.flyway;

import java.util.stream.StreamSupport;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;

import org.springframework.boot.jdbc.SchemaManagement;
import org.springframework.boot.jdbc.SchemaManagementProvider;

/**
 * A Flyway {@link SchemaManagementProvider} that determines if the schema is managed by
 * looking at available {@link Flyway} instances.
 *
 * @author Stephane Nicoll
 */
class FlywaySchemaManagementProvider implements SchemaManagementProvider {

	private final Iterable<Flyway> flywayInstances;

	FlywaySchemaManagementProvider(Iterable<Flyway> flywayInstances) {
		this.flywayInstances = flywayInstances;
	}

	@Override
	public SchemaManagement getSchemaManagement(DataSource dataSource) {
		return StreamSupport.stream(this.flywayInstances.spliterator(), false)
				.map((flyway) -> flyway.getConfiguration().getDataSource())
				.filter(dataSource::equals).findFirst()
				.map((managedDataSource) -> SchemaManagement.MANAGED)
				.orElse(SchemaManagement.UNMANAGED);
	}

}
