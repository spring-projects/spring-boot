/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.r2dbc.init;

import java.util.UUID;

import io.r2dbc.spi.ConnectionFactory;

import org.springframework.boot.r2dbc.ConnectionFactoryBuilder;
import org.springframework.boot.sql.init.AbstractScriptDatabaseInitializerTests;
import org.springframework.boot.sql.init.DatabaseInitializationSettings;
import org.springframework.boot.testsupport.BuildOutput;
import org.springframework.r2dbc.core.DatabaseClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link R2dbcScriptDatabaseInitializer}.
 *
 * @author Andy Wilkinson
 */
class R2dbcScriptDatabaseInitializerTests
		extends AbstractScriptDatabaseInitializerTests<R2dbcScriptDatabaseInitializer> {

	private final ConnectionFactory embeddedConnectionFactory = ConnectionFactoryBuilder
		.withUrl("r2dbc:h2:mem:///" + UUID.randomUUID() + "?options=DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
		.build();

	private final ConnectionFactory standaloneConnectionFactory = ConnectionFactoryBuilder
		.withUrl(
				"r2dbc:h2:file:///"
						+ new BuildOutput(R2dbcScriptDatabaseInitializerTests.class).getRootLocation()
							.getAbsolutePath()
							.replace('\\', '/')
						+ "/" + UUID.randomUUID() + "?options=DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
		.build();

	@Override
	protected R2dbcScriptDatabaseInitializer createEmbeddedDatabaseInitializer(
			DatabaseInitializationSettings settings) {
		return new R2dbcScriptDatabaseInitializer(this.embeddedConnectionFactory, settings);
	}

	@Override
	protected R2dbcScriptDatabaseInitializer createStandaloneDatabaseInitializer(
			DatabaseInitializationSettings settings) {
		return new R2dbcScriptDatabaseInitializer(this.standaloneConnectionFactory, settings);
	}

	@Override
	protected int numberOfEmbeddedRows(String sql) {
		return numberOfRows(this.embeddedConnectionFactory, sql);
	}

	@Override
	protected int numberOfStandaloneRows(String sql) {
		return numberOfRows(this.standaloneConnectionFactory, sql);
	}

	private int numberOfRows(ConnectionFactory connectionFactory, String sql) {
		Integer result = DatabaseClient.create(connectionFactory)
			.sql(sql)
			.map((row, metadata) -> row.get(0))
			.first()
			.map((number) -> ((Number) number).intValue())
			.block();
		assertThat(result).isNotNull();
		return result;
	}

	@Override
	protected void assertDatabaseAccessed(boolean accessed, R2dbcScriptDatabaseInitializer initializer) {
		// No-op as R2DBC does not need to access the database to determine its type
	}

}
