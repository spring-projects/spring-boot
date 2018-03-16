/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.autoconfigure.orm.jpa;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.sql.DataSource;

import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.cfg.AvailableSettings;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.orm.jpa.hibernate.SpringImplicitNamingStrategy;
import org.springframework.boot.orm.jpa.hibernate.SpringPhysicalNamingStrategy;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.vendor.Database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link JpaProperties}.
 *
 * @author Stephane Nicoll
 */
public class JpaPropertiesTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(TestConfiguration.class);

	@Mock
	private Supplier<String> ddlAutoSupplier;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void noCustomNamingStrategy() {
		this.contextRunner.run(assertJpaProperties((properties) -> {
			Map<String, Object> hibernateProperties = properties
					.getHibernateProperties(new HibernateSettings());
			assertThat(hibernateProperties)
					.doesNotContainKeys("hibernate.ejb.naming_strategy");
			assertThat(hibernateProperties).containsEntry(
					"hibernate.physical_naming_strategy",
					SpringPhysicalNamingStrategy.class.getName());
			assertThat(hibernateProperties).containsEntry(
					"hibernate.implicit_naming_strategy",
					SpringImplicitNamingStrategy.class.getName());
		}));
	}

	@Test
	public void hibernate5CustomNamingStrategies() {
		this.contextRunner.withPropertyValues(
				"spring.jpa.hibernate.naming.implicit-strategy:com.example.Implicit",
				"spring.jpa.hibernate.naming.physical-strategy:com.example.Physical")
				.run(assertJpaProperties((properties) -> {
					Map<String, Object> hibernateProperties = properties
							.getHibernateProperties(new HibernateSettings());
					assertThat(hibernateProperties).contains(
							entry("hibernate.implicit_naming_strategy",
									"com.example.Implicit"),
							entry("hibernate.physical_naming_strategy",
									"com.example.Physical"));
					assertThat(hibernateProperties)
							.doesNotContainKeys("hibernate.ejb.naming_strategy");
				}));
	}

	@Test
	public void namingStrategyInstancesCanBeUsed() {
		this.contextRunner.run(assertJpaProperties((properties) -> {
			ImplicitNamingStrategy implicitStrategy = mock(ImplicitNamingStrategy.class);
			PhysicalNamingStrategy physicalStrategy = mock(PhysicalNamingStrategy.class);
			Map<String, Object> hibernateProperties = properties.getHibernateProperties(
					new HibernateSettings().implicitNamingStrategy(implicitStrategy)
							.physicalNamingStrategy(physicalStrategy));
			assertThat(hibernateProperties).contains(
					entry("hibernate.implicit_naming_strategy", implicitStrategy),
					entry("hibernate.physical_naming_strategy", physicalStrategy));
			assertThat(hibernateProperties)
					.doesNotContainKeys("hibernate.ejb.naming_strategy");
		}));
	}

	@Test
	public void namingStrategyInstancesTakePrecedenceOverNamingStrategyProperties() {
		this.contextRunner.withPropertyValues(
				"spring.jpa.hibernate.naming.implicit-strategy:com.example.Implicit",
				"spring.jpa.hibernate.naming.physical-strategy:com.example.Physical")
				.run(assertJpaProperties((properties) -> {
					ImplicitNamingStrategy implicitStrategy = mock(
							ImplicitNamingStrategy.class);
					PhysicalNamingStrategy physicalStrategy = mock(
							PhysicalNamingStrategy.class);
					Map<String, Object> hibernateProperties = properties
							.getHibernateProperties(new HibernateSettings()
									.implicitNamingStrategy(implicitStrategy)
									.physicalNamingStrategy(physicalStrategy));
					assertThat(hibernateProperties).contains(
							entry("hibernate.implicit_naming_strategy", implicitStrategy),
							entry("hibernate.physical_naming_strategy",
									physicalStrategy));
					assertThat(hibernateProperties)
							.doesNotContainKeys("hibernate.ejb.naming_strategy");
				}));
	}

	@Test
	public void hibernatePropertiesCustomizerTakePrecedenceOverStrategyInstancesAndNamingStrategyProperties() {
		this.contextRunner.withPropertyValues(
				"spring.jpa.hibernate.naming.implicit-strategy:com.example.Implicit",
				"spring.jpa.hibernate.naming.physical-strategy:com.example.Physical")
				.run(assertJpaProperties((properties) -> {
					ImplicitNamingStrategy implicitStrategy = mock(
							ImplicitNamingStrategy.class);
					PhysicalNamingStrategy physicalStrategy = mock(
							PhysicalNamingStrategy.class);
					ImplicitNamingStrategy effectiveImplicitStrategy = mock(
							ImplicitNamingStrategy.class);
					PhysicalNamingStrategy effectivePhysicalStrategy = mock(
							PhysicalNamingStrategy.class);
					HibernatePropertiesCustomizer customizer = (hibernateProperties) -> {
						hibernateProperties.put("hibernate.implicit_naming_strategy",
								effectiveImplicitStrategy);
						hibernateProperties.put("hibernate.physical_naming_strategy",
								effectivePhysicalStrategy);
					};
					Map<String, Object> hibernateProperties = properties
							.getHibernateProperties(new HibernateSettings()
									.implicitNamingStrategy(implicitStrategy)
									.physicalNamingStrategy(physicalStrategy)
									.hibernatePropertiesCustomizers(
											Collections.singleton(customizer)));
					assertThat(hibernateProperties).contains(
							entry("hibernate.implicit_naming_strategy",
									effectiveImplicitStrategy),
							entry("hibernate.physical_naming_strategy",
									effectivePhysicalStrategy));
					assertThat(hibernateProperties)
							.doesNotContainKeys("hibernate.ejb.naming_strategy");
				}));
	}

	@Test
	public void hibernate5CustomNamingStrategiesViaJpaProperties() {
		this.contextRunner.withPropertyValues(
				"spring.jpa.properties.hibernate.implicit_naming_strategy:com.example.Implicit",
				"spring.jpa.properties.hibernate.physical_naming_strategy:com.example.Physical")
				.run(assertJpaProperties((properties) -> {
					Map<String, Object> hibernateProperties = properties
							.getHibernateProperties(new HibernateSettings());
					// You can override them as we don't provide any default
					assertThat(hibernateProperties).contains(
							entry("hibernate.implicit_naming_strategy",
									"com.example.Implicit"),
							entry("hibernate.physical_naming_strategy",
									"com.example.Physical"));
					assertThat(hibernateProperties)
							.doesNotContainKeys("hibernate.ejb.naming_strategy");
				}));
	}

	@Test
	public void useNewIdGeneratorMappingsDefault() {
		this.contextRunner.run(assertJpaProperties((properties) -> {
			Map<String, Object> hibernateProperties = properties
					.getHibernateProperties(new HibernateSettings());
			assertThat(hibernateProperties).containsEntry(
					AvailableSettings.USE_NEW_ID_GENERATOR_MAPPINGS, "true");
		}));
	}

	@Test
	public void useNewIdGeneratorMappingsFalse() {
		this.contextRunner
				.withPropertyValues(
						"spring.jpa.hibernate.use-new-id-generator-mappings:false")
				.run(assertJpaProperties((properties) -> {
					Map<String, Object> hibernateProperties = properties
							.getHibernateProperties(new HibernateSettings());
					assertThat(hibernateProperties).containsEntry(
							AvailableSettings.USE_NEW_ID_GENERATOR_MAPPINGS, "false");
				}));
	}

	@Test
	public void determineDatabaseNoCheckIfDatabaseIsSet() {
		this.contextRunner.withPropertyValues("spring.jpa.database=postgresql")
				.run(assertJpaProperties((properties) -> {
					DataSource dataSource = mockStandaloneDataSource();
					Database database = properties.determineDatabase(dataSource);
					assertThat(database).isEqualTo(Database.POSTGRESQL);
					try {
						verify(dataSource, never()).getConnection();
					}
					catch (SQLException ex) {
						throw new IllegalStateException("Should not happen", ex);
					}
				}));
	}

	@Test
	public void determineDatabaseWithKnownUrl() {
		this.contextRunner.run(assertJpaProperties((properties) -> {
			Database database = properties
					.determineDatabase(mockDataSource("jdbc:h2:mem:testdb"));
			assertThat(database).isEqualTo(Database.H2);
		}));
	}

	@Test
	public void determineDatabaseWithKnownUrlAndUserConfig() {
		this.contextRunner.withPropertyValues("spring.jpa.database=mysql")
				.run(assertJpaProperties((properties) -> {
					Database database = properties
							.determineDatabase(mockDataSource("jdbc:h2:mem:testdb"));
					assertThat(database).isEqualTo(Database.MYSQL);
				}));
	}

	@Test
	public void defaultDdlAutoIsNotInvokedIfPropertyIsSet() {
		this.contextRunner.withPropertyValues("spring.jpa.hibernate.ddl-auto=validate")
				.run(assertDefaultDdlAutoNotInvoked("validate"));
	}

	@Test
	public void defaultDdlAutoIsNotInvokedIfHibernateSpecificPropertyIsSet() {
		this.contextRunner
				.withPropertyValues("spring.jpa.properties.hibernate.hbm2ddl.auto=create")
				.run(assertDefaultDdlAutoNotInvoked("create"));
	}

	private ContextConsumer<AssertableApplicationContext> assertDefaultDdlAutoNotInvoked(
			String expectedDdlAuto) {
		return assertJpaProperties((properties) -> {
			Map<String, Object> hibernateProperties = properties.getHibernateProperties(
					new HibernateSettings().ddlAuto(this.ddlAutoSupplier));
			assertThat(hibernateProperties).containsEntry("hibernate.hbm2ddl.auto",
					expectedDdlAuto);
			verify(this.ddlAutoSupplier, never()).get();
		});
	}

	@Test
	public void determineDatabaseWithUnknownUrl() {
		this.contextRunner.run(assertJpaProperties((properties) -> {
			Database database = properties
					.determineDatabase(mockDataSource("jdbc:unknown://localhost"));
			assertThat(database).isEqualTo(Database.DEFAULT);
		}));
	}

	private DataSource mockStandaloneDataSource() {
		try {
			DataSource ds = mock(DataSource.class);
			given(ds.getConnection()).willThrow(SQLException.class);
			return ds;
		}
		catch (SQLException ex) {
			throw new IllegalStateException("Should not happen", ex);
		}
	}

	private DataSource mockDataSource(String jdbcUrl) {
		DataSource ds = mock(DataSource.class);
		try {
			DatabaseMetaData metadata = mock(DatabaseMetaData.class);
			given(metadata.getURL()).willReturn(jdbcUrl);
			Connection connection = mock(Connection.class);
			given(connection.getMetaData()).willReturn(metadata);
			given(ds.getConnection()).willReturn(connection);
		}
		catch (SQLException e) {
			// Do nothing
		}
		return ds;
	}

	private ContextConsumer<AssertableApplicationContext> assertJpaProperties(
			Consumer<JpaProperties> consumer) {
		return (context) -> {
			assertThat(context).hasSingleBean(JpaProperties.class);
			consumer.accept(context.getBean(JpaProperties.class));
		};
	}

	@Configuration
	@EnableConfigurationProperties(JpaProperties.class)
	static class TestConfiguration {

	}

}
