/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.flyway;

import java.sql.Connection;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.callback.FlywayCallback;
import org.flywaydb.core.internal.callback.SqlScriptFlywayCallback;
import org.hibernate.engine.transaction.jta.platform.internal.NoJtaPlatform;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InOrder;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.jdbc.SchemaManagement;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.MapPropertySource;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.stereotype.Component;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link FlywayAutoConfiguration}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Vedran Pavic
 * @author Eddú Meléndez
 */
public class FlywayAutoConfigurationTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	@Before
	public void init() {
		TestPropertyValues.of("spring.datasource.name:flywaytest").applyTo(this.context);
	}

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void noDataSource() throws Exception {
		registerAndRefresh(FlywayAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		assertThat(this.context.getBeanNamesForType(Flyway.class).length).isEqualTo(0);
	}

	@Test
	public void createDataSource() throws Exception {
		TestPropertyValues.of("spring.flyway.url:jdbc:hsqldb:mem:flywaytest",
				"spring.flyway.user:sa").applyTo(this.context);
		registerAndRefresh(EmbeddedDataSourceConfiguration.class,
				FlywayAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		Flyway flyway = this.context.getBean(Flyway.class);
		assertThat(flyway.getDataSource()).isNotNull();
	}

	@Test
	public void flywayDataSource() throws Exception {
		registerAndRefresh(FlywayDataSourceConfiguration.class,
				EmbeddedDataSourceConfiguration.class, FlywayAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		Flyway flyway = this.context.getBean(Flyway.class);
		assertThat(flyway.getDataSource())
				.isEqualTo(this.context.getBean("flywayDataSource"));
	}

	@Test
	public void schemaManagementProviderDetectsDataSource() throws Exception {
		registerAndRefresh(FlywayDataSourceConfiguration.class,
				EmbeddedDataSourceConfiguration.class, FlywayAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		FlywaySchemaManagementProvider schemaManagementProvider = this.context
				.getBean(FlywaySchemaManagementProvider.class);
		assertThat(schemaManagementProvider
				.getSchemaManagement(this.context.getBean(DataSource.class)))
						.isEqualTo(SchemaManagement.UNMANAGED);
		assertThat(schemaManagementProvider.getSchemaManagement(
				this.context.getBean("flywayDataSource", DataSource.class)))
						.isEqualTo(SchemaManagement.MANAGED);
	}

	@Test
	public void defaultFlyway() throws Exception {
		registerAndRefresh(EmbeddedDataSourceConfiguration.class,
				FlywayAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		Flyway flyway = this.context.getBean(Flyway.class);
		assertThat(flyway.getLocations()).containsExactly("classpath:db/migration");
	}

	@Test
	public void overrideLocations() throws Exception {
		TestPropertyValues
				.of("spring.flyway.locations:classpath:db/changelog,classpath:db/migration")
				.applyTo(this.context);
		registerAndRefresh(EmbeddedDataSourceConfiguration.class,
				FlywayAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		Flyway flyway = this.context.getBean(Flyway.class);
		assertThat(flyway.getLocations()).containsExactly("classpath:db/changelog",
				"classpath:db/migration");
	}

	@Test
	public void overrideLocationsList() throws Exception {
		TestPropertyValues
				.of("spring.flyway.locations[0]:classpath:db/changelog",
						"spring.flyway.locations[1]:classpath:db/migration")
				.applyTo(this.context);
		registerAndRefresh(EmbeddedDataSourceConfiguration.class,
				FlywayAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		Flyway flyway = this.context.getBean(Flyway.class);
		assertThat(flyway.getLocations()).containsExactly("classpath:db/changelog",
				"classpath:db/migration");
	}

	@Test
	public void overrideSchemas() throws Exception {
		TestPropertyValues.of("spring.flyway.schemas:public").applyTo(this.context);
		registerAndRefresh(EmbeddedDataSourceConfiguration.class,
				FlywayAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		Flyway flyway = this.context.getBean(Flyway.class);
		assertThat(Arrays.asList(flyway.getSchemas()).toString()).isEqualTo("[public]");
	}

	@Test
	public void changeLogDoesNotExist() throws Exception {
		TestPropertyValues.of("spring.flyway.locations:file:no-such-dir")
				.applyTo(this.context);
		this.thrown.expect(BeanCreationException.class);
		registerAndRefresh(EmbeddedDataSourceConfiguration.class,
				FlywayAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
	}

	@Test
	public void checkLocationsAllMissing() throws Exception {
		TestPropertyValues
				.of("spring.flyway.locations:classpath:db/missing1,classpath:db/migration2",
						"spring.flyway.check-location:true")
				.applyTo(this.context);
		this.thrown.expect(BeanCreationException.class);
		this.thrown.expectMessage("Cannot find migrations location in");
		registerAndRefresh(EmbeddedDataSourceConfiguration.class,
				FlywayAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
	}

	@Test
	public void checkLocationsAllExist() throws Exception {
		TestPropertyValues
				.of("spring.flyway.locations:classpath:db/changelog,classpath:db/migration",
						"spring.flyway.check-location:true")
				.applyTo(this.context);
		registerAndRefresh(EmbeddedDataSourceConfiguration.class,
				FlywayAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
	}

	@Test
	public void customFlywayMigrationStrategy() throws Exception {
		registerAndRefresh(EmbeddedDataSourceConfiguration.class,
				FlywayAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class,
				MockFlywayMigrationStrategy.class);
		assertThat(this.context.getBean(Flyway.class)).isNotNull();
		this.context.getBean(MockFlywayMigrationStrategy.class).assertCalled();
	}

	@Test
	public void customFlywayMigrationInitializer() throws Exception {
		registerAndRefresh(CustomFlywayMigrationInitializer.class,
				EmbeddedDataSourceConfiguration.class, FlywayAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		assertThat(this.context.getBean(Flyway.class)).isNotNull();
		FlywayMigrationInitializer initializer = this.context
				.getBean(FlywayMigrationInitializer.class);
		assertThat(initializer.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE);
	}

	@Test
	public void customFlywayWithJpa() throws Exception {
		registerAndRefresh(CustomFlywayWithJpaConfiguration.class,
				EmbeddedDataSourceConfiguration.class, FlywayAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
	}

	@Test
	public void overrideBaselineVersionString() throws Exception {
		TestPropertyValues.of("spring.flyway.baseline-version=0").applyTo(this.context);
		registerAndRefresh(EmbeddedDataSourceConfiguration.class,
				FlywayAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		Flyway flyway = this.context.getBean(Flyway.class);
		assertThat(flyway.getBaselineVersion())
				.isEqualTo(MigrationVersion.fromVersion("0"));
	}

	@Test
	public void overrideBaselineVersionNumber() throws Exception {
		Map<String, Object> source = Collections
				.<String, Object>singletonMap("spring.flyway.baseline-version", 1);
		this.context.getEnvironment().getPropertySources()
				.addLast(new MapPropertySource("flyway", source));
		registerAndRefresh(EmbeddedDataSourceConfiguration.class,
				FlywayAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		Flyway flyway = this.context.getBean(Flyway.class);
		assertThat(flyway.getBaselineVersion())
				.isEqualTo(MigrationVersion.fromVersion("1"));
	}

	@Test
	public void useVendorDirectory() throws Exception {
		TestPropertyValues
				.of("spring.flyway.locations=classpath:db/vendors/{vendor},classpath:db/changelog")
				.applyTo(this.context);
		registerAndRefresh(EmbeddedDataSourceConfiguration.class,
				FlywayAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		Flyway flyway = this.context.getBean(Flyway.class);
		assertThat(flyway.getLocations()).containsExactlyInAnyOrder(
				"classpath:db/vendors/h2", "classpath:db/changelog");
	}

	@Test
	public void callbacksAreConfiguredAndOrdered() throws Exception {
		registerAndRefresh(EmbeddedDataSourceConfiguration.class,
				FlywayAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class,
				CallbackConfiguration.class);
		assertThat(this.context.getBeansOfType(Flyway.class)).hasSize(1);
		Flyway flyway = this.context.getBean(Flyway.class);
		FlywayCallback callbackOne = this.context.getBean("callbackOne",
				FlywayCallback.class);
		FlywayCallback callbackTwo = this.context.getBean("callbackTwo",
				FlywayCallback.class);
		assertThat(flyway.getCallbacks()).hasSize(3);
		assertThat(flyway.getCallbacks()).startsWith(callbackTwo, callbackOne);
		assertThat(flyway.getCallbacks()[2]).isInstanceOf(SqlScriptFlywayCallback.class);
		InOrder orderedCallbacks = inOrder(callbackOne, callbackTwo);
		orderedCallbacks.verify(callbackTwo).beforeMigrate(any(Connection.class));
		orderedCallbacks.verify(callbackOne).beforeMigrate(any(Connection.class));
	}

	private void registerAndRefresh(Class<?>... annotatedClasses) {
		this.context.register(annotatedClasses);
		this.context.refresh();
	}

	@Configuration
	protected static class FlywayDataSourceConfiguration {

		@Bean
		@Primary
		public DataSource normalDataSource() {
			return DataSourceBuilder.create().url("jdbc:hsqldb:mem:normal").username("sa")
					.build();
		}

		@FlywayDataSource
		@Bean
		public DataSource flywayDataSource() {
			return DataSourceBuilder.create().url("jdbc:hsqldb:mem:flywaytest")
					.username("sa").build();
		}

	}

	@Configuration
	protected static class CustomFlywayMigrationInitializer {

		@Bean
		public FlywayMigrationInitializer flywayMigrationInitializer(Flyway flyway) {
			FlywayMigrationInitializer initializer = new FlywayMigrationInitializer(
					flyway);
			initializer.setOrder(Ordered.HIGHEST_PRECEDENCE);
			return initializer;
		}

	}

	@Configuration
	protected static class CustomFlywayWithJpaConfiguration {

		private final DataSource dataSource;

		protected CustomFlywayWithJpaConfiguration(DataSource dataSource) {
			this.dataSource = dataSource;
		}

		@Bean
		public Flyway flyway() {
			return new Flyway();
		}

		@Bean
		public LocalContainerEntityManagerFactoryBean entityManagerFactoryBean() {
			Map<String, Object> properties = new HashMap<>();
			properties.put("configured", "manually");
			properties.put("hibernate.transaction.jta.platform", NoJtaPlatform.INSTANCE);
			return new EntityManagerFactoryBuilder(new HibernateJpaVendorAdapter(),
					properties, null).dataSource(this.dataSource).build();
		}

	}

	@Component
	protected static class MockFlywayMigrationStrategy
			implements FlywayMigrationStrategy {

		private boolean called = false;

		@Override
		public void migrate(Flyway flyway) {
			this.called = true;
		}

		public void assertCalled() {
			assertThat(this.called).isTrue();
		}

	}

	@Configuration
	static class CallbackConfiguration {

		@Bean
		@Order(1)
		public FlywayCallback callbackOne() {
			return mock(FlywayCallback.class);
		}

		@Bean
		@Order(0)
		public FlywayCallback callbackTwo() {
			return mock(FlywayCallback.class);
		}

	}

}
