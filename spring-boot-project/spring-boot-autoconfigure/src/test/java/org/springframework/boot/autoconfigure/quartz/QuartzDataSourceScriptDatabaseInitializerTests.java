/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.autoconfigure.quartz;

import java.util.Arrays;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import org.springframework.boot.sql.init.DatabaseInitializationSettings;
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
		QuartzProperties properties = new QuartzProperties();
		properties.getJdbc().setPlatform("test");
		DatabaseInitializationSettings settings = QuartzDataSourceScriptDatabaseInitializer.getSettings(dataSource,
				properties);
		assertThat(settings.getSchemaLocations())
			.containsOnly("classpath:org/quartz/impl/jdbcjobstore/tables_test.sql");
		then(dataSource).shouldHaveNoInteractions();
	}

	@Test
	void customizeSetCommentPrefixes() {
		QuartzProperties properties = new QuartzProperties();
		properties.getJdbc().setPlatform("test");
		properties.getJdbc().setCommentPrefix(Arrays.asList("##", "--"));
		QuartzDataSourceScriptDatabaseInitializer initializer = new QuartzDataSourceScriptDatabaseInitializer(
				mock(DataSource.class), properties);
		ResourceDatabasePopulator populator = mock(ResourceDatabasePopulator.class);
		initializer.customize(populator);
		then(populator).should().setCommentPrefixes("##", "--");
	}

}
