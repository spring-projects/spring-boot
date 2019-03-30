/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.autoconfigure.orm.jpa;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link HibernateProperties}.
 *
 * @author Stephane Nicoll
 * @author Artsiom Yudovin
 */
public class HibernatePropertiesTests {

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
		this.contextRunner.run(assertHibernateProperties((hibernateProperties) -> {
			assertThat(hibernateProperties)
					.doesNotContainKeys("hibernate.ejb.naming_strategy");
			assertThat(hibernateProperties).containsEntry(
					AvailableSettings.PHYSICAL_NAMING_STRATEGY,
					SpringPhysicalNamingStrategy.class.getName());
			assertThat(hibernateProperties).containsEntry(
					AvailableSettings.IMPLICIT_NAMING_STRATEGY,
					SpringImplicitNamingStrategy.class.getName());
		}));
	}

	@Test
	public void hibernate5CustomNamingStrategies() {
		this.contextRunner.withPropertyValues(
				"spring.jpa.hibernate.naming.implicit-strategy:com.example.Implicit",
				"spring.jpa.hibernate.naming.physical-strategy:com.example.Physical")
				.run(assertHibernateProperties((hibernateProperties) -> {
					assertThat(hibernateProperties).contains(
							entry(AvailableSettings.IMPLICIT_NAMING_STRATEGY,
									"com.example.Implicit"),
							entry(AvailableSettings.PHYSICAL_NAMING_STRATEGY,
									"com.example.Physical"));
					assertThat(hibernateProperties)
							.doesNotContainKeys("hibernate.ejb.naming_strategy");
				}));
	}

	@Test
	public void hibernate5CustomNamingStrategiesViaJpaProperties() {
		this.contextRunner.withPropertyValues(
				"spring.jpa.properties.hibernate.implicit_naming_strategy:com.example.Implicit",
				"spring.jpa.properties.hibernate.physical_naming_strategy:com.example.Physical")
				.run(assertHibernateProperties((hibernateProperties) -> {
					// You can override them as we don't provide any default
					assertThat(hibernateProperties).contains(
							entry(AvailableSettings.IMPLICIT_NAMING_STRATEGY,
									"com.example.Implicit"),
							entry(AvailableSettings.PHYSICAL_NAMING_STRATEGY,
									"com.example.Physical"));
					assertThat(hibernateProperties)
							.doesNotContainKeys("hibernate.ejb.naming_strategy");
				}));
	}

	@Test
	public void useNewIdGeneratorMappingsDefault() {
		this.contextRunner.run(assertHibernateProperties(
				(hibernateProperties) -> assertThat(hibernateProperties).containsEntry(
						AvailableSettings.USE_NEW_ID_GENERATOR_MAPPINGS, "true")));
	}

	@Test
	public void useNewIdGeneratorMappingsFalse() {
		this.contextRunner
				.withPropertyValues(
						"spring.jpa.hibernate.use-new-id-generator-mappings:false")
				.run(assertHibernateProperties(
						(hibernateProperties) -> assertThat(hibernateProperties)
								.containsEntry(
										AvailableSettings.USE_NEW_ID_GENERATOR_MAPPINGS,
										"false")));
	}

	@Test
	public void scannerUsesDisabledScannerByDefault() {
		this.contextRunner.run(assertHibernateProperties(
				(hibernateProperties) -> assertThat(hibernateProperties).containsEntry(
						AvailableSettings.SCANNER,
						"org.hibernate.boot.archive.scan.internal.DisabledScanner")));
	}

	@Test
	public void scannerCanBeCustomized() {
		this.contextRunner.withPropertyValues(
				"spring.jpa.properties.hibernate.archive.scanner:org.hibernate.boot.archive.scan.internal.StandardScanner")
				.run(assertHibernateProperties((hibernateProperties) -> assertThat(
						hibernateProperties).containsEntry(AvailableSettings.SCANNER,
								"org.hibernate.boot.archive.scan.internal.StandardScanner")));
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
		return assertHibernateProperties((hibernateProperties) -> {
			assertThat(hibernateProperties).containsEntry(AvailableSettings.HBM2DDL_AUTO,
					expectedDdlAuto);
			verify(this.ddlAutoSupplier, never()).get();
		});
	}

	private ContextConsumer<AssertableApplicationContext> assertHibernateProperties(
			Consumer<Map<String, Object>> consumer) {
		return (context) -> {
			assertThat(context).hasSingleBean(JpaProperties.class);
			assertThat(context).hasSingleBean(HibernateProperties.class);
			Map<String, Object> hibernateProperties = context
					.getBean(HibernateProperties.class).determineHibernateProperties(
							context.getBean(JpaProperties.class).getProperties(),
							new HibernateSettings().ddlAuto(this.ddlAutoSupplier));
			consumer.accept(hibernateProperties);
		};
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties({ JpaProperties.class, HibernateProperties.class })
	static class TestConfiguration {

	}

}
