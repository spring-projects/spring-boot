/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.test.autoconfigure.orm.jpa;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.junit.runner.classpath.ClassPathExclusions;
import org.springframework.boot.junit.runner.classpath.ModifiedClassPathRunner;
import org.springframework.boot.test.autoconfigure.jdbc.TestDatabaseAutoConfiguration;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Specific tests for {@link TestDatabaseAutoConfiguration} when no embedded database is
 * available.
 *
 * @author Stephane Nicoll
 */
@RunWith(ModifiedClassPathRunner.class)
@ClassPathExclusions({ "h2-*.jar", "hsqldb-*.jar", "derby-*.jar" })
public class TestDatabaseAutoConfigurationNoEmbeddedTests {

	private ConfigurableApplicationContext context;

	@After
	public void closeContext() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void applyAnyReplace() {
		try {
			load(ExistingDataSourceConfiguration.class);
		}
		catch (BeanCreationException ex) {
			String message = ex.getMessage();
			assertThat(message).contains(
					"Failed to replace DataSource with an embedded database for tests.");
			assertThat(message).contains(
					"If you want an embedded database please put a supported one on the "
							+ "classpath");
			assertThat(message).contains(
					"or tune the replace attribute of @AutoconfigureTestDatabase.");
		}
	}

	@Test
	public void applyNoReplace() {
		load(ExistingDataSourceConfiguration.class, "spring.test.database.replace=NONE");
		assertThat(this.context.getBeansOfType(DataSource.class)).hasSize(1);
		assertThat(this.context.getBean(DataSource.class))
				.isSameAs(this.context.getBean("myCustomDataSource"));
	}

	public void load(Class<?> config, String... environment) {
		this.context = doLoad(config, environment);
	}

	public ConfigurableApplicationContext doLoad(Class<?> config, String... environment) {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		if (config != null) {
			ctx.register(config);
		}
		ctx.register(TestDatabaseAutoConfiguration.class);
		EnvironmentTestUtils.addEnvironment(ctx, environment);
		ctx.refresh();
		return ctx;
	}

	@Configuration
	static class ExistingDataSourceConfiguration {

		@Bean
		public DataSource myCustomDataSource() {
			return mock(DataSource.class);
		}

	}

}
