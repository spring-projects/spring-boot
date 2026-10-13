/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.flyway.autoconfigure;

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
			.filter(dataSource::equals)
			.findFirst()
			.map((managedDataSource) -> SchemaManagement.MANAGED)
			.orElse(SchemaManagement.UNMANAGED);
	}

}
