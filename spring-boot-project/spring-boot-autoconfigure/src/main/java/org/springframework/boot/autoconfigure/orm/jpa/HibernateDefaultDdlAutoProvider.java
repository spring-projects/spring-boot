/*
 * Copyright 2012-2019 the original author or authors.
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

import java.util.stream.StreamSupport;

import javax.sql.DataSource;

import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.jdbc.SchemaManagement;
import org.springframework.boot.jdbc.SchemaManagementProvider;

/**
 * A {@link SchemaManagementProvider} that invokes a configurable number of
 * {@link SchemaManagementProvider} instances for embedded data sources only.
 *
 * @author Stephane Nicoll
 */
class HibernateDefaultDdlAutoProvider implements SchemaManagementProvider {

	private final Iterable<SchemaManagementProvider> providers;

	HibernateDefaultDdlAutoProvider(Iterable<SchemaManagementProvider> providers) {
		this.providers = providers;
	}

	public String getDefaultDdlAuto(DataSource dataSource) {
		if (!EmbeddedDatabaseConnection.isEmbedded(dataSource)) {
			return "none";
		}
		SchemaManagement schemaManagement = getSchemaManagement(dataSource);
		if (SchemaManagement.MANAGED.equals(schemaManagement)) {
			return "none";
		}
		return "create-drop";

	}

	@Override
	public SchemaManagement getSchemaManagement(DataSource dataSource) {
		return StreamSupport.stream(this.providers.spliterator(), false)
				.map((provider) -> provider.getSchemaManagement(dataSource)).filter(SchemaManagement.MANAGED::equals)
				.findFirst().orElse(SchemaManagement.UNMANAGED);
	}

}
