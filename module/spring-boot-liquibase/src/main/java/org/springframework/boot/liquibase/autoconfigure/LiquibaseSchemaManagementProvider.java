/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.liquibase.autoconfigure;

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
			.map(SpringLiquibase::getDataSource)
			.filter(dataSource::equals)
			.findFirst()
			.map((managedDataSource) -> SchemaManagement.MANAGED)
			.orElse(SchemaManagement.UNMANAGED);
	}

}
