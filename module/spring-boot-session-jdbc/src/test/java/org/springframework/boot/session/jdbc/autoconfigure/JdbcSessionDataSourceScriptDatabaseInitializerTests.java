/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.session.jdbc.autoconfigure;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import org.springframework.boot.sql.init.DatabaseInitializationSettings;
import org.springframework.boot.sql.init.ScriptDatabaseInitializerSettings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link JdbcSessionDataSourceScriptDatabaseInitializer}.
 *
 * @author Stephane Nicoll
 */
class JdbcSessionDataSourceScriptDatabaseInitializerTests {

	@Test
	void getSettingsWithPlatformDoesNotTouchDataSource() {
		DataSource dataSource = mock(DataSource.class);
		JdbcSessionProperties properties = new JdbcSessionProperties();
		properties.setPlatform("test");
		DatabaseInitializationSettings settings = ScriptDatabaseInitializerSettings
			.get(new JdbcSessionDataSourceScriptDatabaseInitializer(dataSource, properties));
		assertThat(settings.getSchemaLocations())
			.containsOnly("classpath:org/springframework/session/jdbc/schema-test.sql");
		then(dataSource).shouldHaveNoInteractions();
	}

}
