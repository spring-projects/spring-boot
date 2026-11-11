/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.integration.autoconfigure;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import org.springframework.boot.sql.init.DatabaseInitializationSettings;
import org.springframework.boot.sql.init.ScriptDatabaseInitializerSettings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link IntegrationDataSourceScriptDatabaseInitializer}.
 *
 * @author Stephane Nicoll
 */
class IntegrationDataSourceScriptDatabaseInitializerTests {

	@Test
	void getSettingsWithPlatformDoesNotTouchDataSource() {
		DataSource dataSource = mock(DataSource.class);
		IntegrationJdbcProperties properties = new IntegrationJdbcProperties();
		properties.setPlatform("test");
		DatabaseInitializationSettings settings = ScriptDatabaseInitializerSettings
			.get(new IntegrationDataSourceScriptDatabaseInitializer(dataSource, properties));
		assertThat(settings.getSchemaLocations())
			.containsOnly("classpath:org/springframework/integration/jdbc/schema-test.sql");
		then(dataSource).shouldHaveNoInteractions();
	}

}
