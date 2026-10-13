/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.hibernate.autoconfigure;

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

	String getDefaultDdlAuto(DataSource dataSource) {
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
			.map((provider) -> provider.getSchemaManagement(dataSource))
			.filter(SchemaManagement.MANAGED::equals)
			.findFirst()
			.orElse(SchemaManagement.UNMANAGED);
	}

}
