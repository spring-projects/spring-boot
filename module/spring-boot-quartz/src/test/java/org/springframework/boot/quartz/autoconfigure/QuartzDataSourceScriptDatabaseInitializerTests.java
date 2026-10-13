/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.quartz.autoconfigure;

import java.util.Arrays;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import org.springframework.boot.sql.init.DatabaseInitializationSettings;
import org.springframework.boot.sql.init.ScriptDatabaseInitializerSettings;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link QuartzDataSourceScriptDatabaseInitializer}.
 *
 * @author Stephane Nicoll
 */
class QuartzDataSourceScriptDatabaseInitializerTests {

	@Test
	void getSettingsWithPlatformDoesNotTouchDataSource() {
		DataSource dataSource = mock(DataSource.class);
		QuartzJdbcProperties properties = new QuartzJdbcProperties();
		properties.setPlatform("test");
		DatabaseInitializationSettings settings = ScriptDatabaseInitializerSettings
			.get(new QuartzDataSourceScriptDatabaseInitializer(dataSource, properties));
		assertThat(settings.getSchemaLocations())
			.containsOnly("classpath:org/quartz/impl/jdbcjobstore/tables_test.sql");
		then(dataSource).shouldHaveNoInteractions();
	}

	@Test
	void customizeSetCommentPrefixes() {
		QuartzJdbcProperties properties = new QuartzJdbcProperties();
		properties.setPlatform("test");
		properties.setCommentPrefix(Arrays.asList("##", "--"));
		QuartzDataSourceScriptDatabaseInitializer initializer = new QuartzDataSourceScriptDatabaseInitializer(
				mock(DataSource.class), properties);
		ResourceDatabasePopulator populator = mock(ResourceDatabasePopulator.class);
		initializer.customize(populator);
		then(populator).should().setCommentPrefixes("##", "--");
	}

}
