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

package org.springframework.boot.autoconfigure.jmx;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.integration.IntegrationAutoConfiguration;
import org.springframework.boot.context.annotation.UserConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.jmx.config.EnableIntegrationMBeanExport;
import org.springframework.integration.monitor.IntegrationMBeanExporter;
import org.springframework.jmx.export.MBeanExporter;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.jmx.export.naming.MetadataNamingStrategy;
import org.springframework.jmx.export.naming.ObjectNamingStrategy;
import org.springframework.jmx.support.RegistrationPolicy;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JmxAutoConfiguration}.
 *
 * @author Christian Dupuis
 * @author Artsiom Yudovin
 * @author Scott Frederick
 */
class JmxAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(JmxAutoConfiguration.class));

	@Test
	void testDefaultMBeanExport() {
		this.contextRunner.run((context) -> {
			assertThat(context).doesNotHaveBean(MBeanExporter.class);
			assertThat(context).doesNotHaveBean(ObjectNamingStrategy.class);
		});
	}

	@Test
	void testDisabledMBeanExport() {
		this.contextRunner.withPropertyValues("spring.jmx.enabled=false").run((context) -> {
			assertThat(context).doesNotHaveBean(MBeanExporter.class);
			assertThat(context).doesNotHaveBean(ObjectNamingStrategy.class);
		});
	}

	@Test
	void testEnabledMBeanExport() {
		this.contextRunner.withPropertyValues("spring.jmx.enabled=true").run((context) -> {
			assertThat(context).hasSingleBean(MBeanExporter.class);
			assertThat(context).hasSingleBean(ParentAwareNamingStrategy.class);
			MBeanExporter exporter = context.getBean(MBeanExporter.class);
			assertThat(exporter).hasFieldOrPropertyWithValue("ensureUniqueRuntimeObjectNames", false);
			assertThat(exporter).hasFieldOrPropertyWithValue("registrationPolicy", RegistrationPolicy.FAIL_ON_EXISTING);

			MetadataNamingStrategy naming = (MetadataNamingStrategy) ReflectionTestUtils.getField(exporter,
					"namingStrategy");
			assertThat(naming).hasFieldOrPropertyWithValue("ensureUniqueRuntimeObjectNames", false);
		});
	}

	@Test
	void testDefaultDomainConfiguredOnMBeanExport() {
		this.contextRunner
			.withPropertyValues("spring.jmx.enabled=true", "spring.jmx.default-domain=my-test-domain",
					"spring.jmx.unique-names=true", "spring.jmx.registration-policy=IGNORE_EXISTING")
			.run((context) -> {
				assertThat(context).hasSingleBean(MBeanExporter.class);
				MBeanExporter exporter = context.getBean(MBeanExporter.class);
				assertThat(exporter).hasFieldOrPropertyWithValue("ensureUniqueRuntimeObjectNames", true);
				assertThat(exporter).hasFieldOrPropertyWithValue("registrationPolicy",
						RegistrationPolicy.IGNORE_EXISTING);

				MetadataNamingStrategy naming = (MetadataNamingStrategy) ReflectionTestUtils.getField(exporter,
						"namingStrategy");
				assertThat(naming).hasFieldOrPropertyWithValue("defaultDomain", "my-test-domain");
				assertThat(naming).hasFieldOrPropertyWithValue("ensureUniqueRuntimeObjectNames", true);
			});
	}

	@Test
	void testBasicParentContext() {
		try (AnnotationConfigApplicationContext parent = new AnnotationConfigApplicationContext()) {
			parent.register(JmxAutoConfiguration.class);
			parent.refresh();
			this.contextRunner.withParent(parent).run((context) -> assertThat(context.isRunning()));
		}
	}

	@Test
	void testParentContext() {
		try (AnnotationConfigApplicationContext parent = new AnnotationConfigApplicationContext()) {
			parent.register(JmxAutoConfiguration.class, TestConfiguration.class);
			parent.refresh();
			this.contextRunner.withParent(parent)
				.withConfiguration(UserConfigurations.of(TestConfiguration.class))
				.run((context) -> assertThat(context.isRunning()));
		}
	}

	@Test
	void customJmxDomain() {
		this.contextRunner.withConfiguration(UserConfigurations.of(CustomJmxDomainConfiguration.class))
			.withConfiguration(AutoConfigurations.of(JmxAutoConfiguration.class, IntegrationAutoConfiguration.class))
			.run((context) -> {
				assertThat(context).hasSingleBean(IntegrationMBeanExporter.class);
				IntegrationMBeanExporter exporter = context.getBean(IntegrationMBeanExporter.class);
				assertThat(exporter).hasFieldOrPropertyWithValue("domain", "foo.my");
			});
	}

	@Configuration(proxyBeanMethods = false)
	@EnableIntegrationMBeanExport(defaultDomain = "foo.my")
	static class CustomJmxDomainConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	static class TestConfiguration {

		@Bean
		Counter counter() {
			return new Counter();
		}

	}

	@ManagedResource
	public static class Counter {

		private int counter = 0;

		@ManagedAttribute
		public int get() {
			return this.counter;
		}

		@ManagedOperation
		public void increment() {
			this.counter++;
		}

	}

}
