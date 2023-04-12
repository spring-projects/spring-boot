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

package org.springframework.boot.autoconfigure.jdbc;

import javax.sql.DataSource;
import javax.sql.XADataSource;

import com.ibm.db2.jcc.DB2XADataSource;
import org.hsqldb.jdbc.pool.JDBCXADataSource;
import org.junit.jupiter.api.Test;
import org.postgresql.xa.PGXADataSource;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.boot.jdbc.XADataSourceWrapper;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link XADataSourceAutoConfiguration}.
 *
 * @author Phillip Webb
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 */
class XADataSourceAutoConfigurationTests {

	@Test
	void wrapExistingXaDataSource() {
		ApplicationContext context = createContext(WrapExisting.class);
		context.getBean(DataSource.class);
		XADataSource source = context.getBean(XADataSource.class);
		MockXADataSourceWrapper wrapper = context.getBean(MockXADataSourceWrapper.class);
		assertThat(wrapper.getXaDataSource()).isEqualTo(source);
	}

	@Test
	void createFromUrl() {
		ApplicationContext context = createContext(FromProperties.class, "spring.datasource.url:jdbc:hsqldb:mem:test",
				"spring.datasource.username:un");
		context.getBean(DataSource.class);
		MockXADataSourceWrapper wrapper = context.getBean(MockXADataSourceWrapper.class);
		JDBCXADataSource dataSource = (JDBCXADataSource) wrapper.getXaDataSource();
		assertThat(dataSource).isNotNull();
		assertThat(dataSource.getUrl()).isEqualTo("jdbc:hsqldb:mem:test");
		assertThat(dataSource.getUser()).isEqualTo("un");
	}

	@Test
	void createNonEmbeddedFromXAProperties() {
		new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(XADataSourceAutoConfiguration.class))
			.withUserConfiguration(FromProperties.class)
			.withClassLoader(new FilteredClassLoader("org.h2.Driver", "org.hsqldb.jdbcDriver"))
			.withPropertyValues("spring.datasource.xa.data-source-class-name:com.ibm.db2.jcc.DB2XADataSource",
					"spring.datasource.xa.properties.user:test", "spring.datasource.xa.properties.password:secret")
			.run((context) -> {
				MockXADataSourceWrapper wrapper = context.getBean(MockXADataSourceWrapper.class);
				XADataSource xaDataSource = wrapper.getXaDataSource();
				assertThat(xaDataSource).isInstanceOf(DB2XADataSource.class);
			});
	}

	@Test
	void createFromClass() throws Exception {
		ApplicationContext context = createContext(FromProperties.class,
				"spring.datasource.xa.data-source-class-name:org.hsqldb.jdbc.pool.JDBCXADataSource",
				"spring.datasource.xa.properties.login-timeout:123");
		context.getBean(DataSource.class);
		MockXADataSourceWrapper wrapper = context.getBean(MockXADataSourceWrapper.class);
		JDBCXADataSource dataSource = (JDBCXADataSource) wrapper.getXaDataSource();
		assertThat(dataSource).isNotNull();
		assertThat(dataSource.getLoginTimeout()).isEqualTo(123);
	}

	@Test
	void definesPropertiesBasedConnectionDetailsByDefault() {
		new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(XADataSourceAutoConfiguration.class))
			.withUserConfiguration(FromProperties.class)
			.run((context) -> assertThat(context).hasSingleBean(PropertiesJdbcConnectionDetails.class));
	}

	@Test
	void shouldUseCustomConnectionDetailsWhenDefined() {
		JdbcConnectionDetails connectionDetails = mock(JdbcConnectionDetails.class);
		given(connectionDetails.getUsername()).willReturn("user-1");
		given(connectionDetails.getPassword()).willReturn("password-1");
		given(connectionDetails.getJdbcUrl()).willReturn("jdbc:postgresql://postgres.example.com:12345/database-1");
		given(connectionDetails.getDriverClassName()).willReturn(DatabaseDriver.POSTGRESQL.getDriverClassName());
		given(connectionDetails.getXaDataSourceClassName())
			.willReturn(DatabaseDriver.POSTGRESQL.getXaDataSourceClassName());
		new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(XADataSourceAutoConfiguration.class))
			.withUserConfiguration(FromProperties.class)
			.withBean(JdbcConnectionDetails.class, () -> connectionDetails)
			.run((context) -> {
				assertThat(context).hasSingleBean(JdbcConnectionDetails.class)
					.doesNotHaveBean(PropertiesJdbcConnectionDetails.class);
				MockXADataSourceWrapper wrapper = context.getBean(MockXADataSourceWrapper.class);
				PGXADataSource dataSource = (PGXADataSource) wrapper.getXaDataSource();
				assertThat(dataSource).isNotNull();
				assertThat(dataSource.getUrl()).startsWith("jdbc:postgresql://postgres.example.com:12345/database-1");
				assertThat(dataSource.getUser()).isEqualTo("user-1");
				assertThat(dataSource.getPassword()).isEqualTo("password-1");
			});
	}

	private ApplicationContext createContext(Class<?> configuration, String... env) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		TestPropertyValues.of(env).applyTo(context);
		context.register(configuration, XADataSourceAutoConfiguration.class);
		context.refresh();
		return context;
	}

	@Configuration(proxyBeanMethods = false)
	static class WrapExisting {

		@Bean
		MockXADataSourceWrapper wrapper() {
			return new MockXADataSourceWrapper();
		}

		@Bean
		XADataSource xaDataSource() {
			return mock(XADataSource.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class FromProperties {

		@Bean
		MockXADataSourceWrapper wrapper() {
			return new MockXADataSourceWrapper();
		}

	}

	static class MockXADataSourceWrapper implements XADataSourceWrapper {

		private XADataSource dataSource;

		@Override
		public DataSource wrapDataSource(XADataSource dataSource) {
			this.dataSource = dataSource;
			return mock(DataSource.class);
		}

		XADataSource getXaDataSource() {
			return this.dataSource;
		}

	}

}
