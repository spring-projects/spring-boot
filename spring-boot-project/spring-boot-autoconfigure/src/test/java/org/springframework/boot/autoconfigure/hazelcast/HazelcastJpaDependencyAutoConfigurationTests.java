/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.autoconfigure.hazelcast;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.hazelcast.core.HazelcastInstance;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.hazelcast.HazelcastJpaDependencyAutoConfiguration.HazelcastInstanceEntityManagerFactoryDependsOnPostProcessor;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.EntityManagerFactoryDependsOnPostProcessor;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link HazelcastJpaDependencyAutoConfiguration}.
 *
 * @author Stephane Nicoll
 */
class HazelcastJpaDependencyAutoConfigurationTests {

	private static final String POST_PROCESSOR_BEAN_NAME = HazelcastInstanceEntityManagerFactoryDependsOnPostProcessor.class
			.getName();

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class,
					HibernateJpaAutoConfiguration.class, HazelcastJpaDependencyAutoConfiguration.class))
			.withPropertyValues("spring.datasource.generate-unique-name=true");

	@Test
	void registrationIfHazelcastInstanceHasRegularBeanName() {
		this.contextRunner.withUserConfiguration(HazelcastConfiguration.class).run((context) -> {
			assertThat(postProcessors(context)).containsKey(POST_PROCESSOR_BEAN_NAME);
			assertThat(entityManagerFactoryDependencies(context)).contains("hazelcastInstance");
		});
	}

	@Test
	void noRegistrationIfHazelcastInstanceHasCustomBeanName() {
		this.contextRunner.withUserConfiguration(HazelcastCustomNameConfiguration.class).run((context) -> {
			assertThat(entityManagerFactoryDependencies(context)).doesNotContain("hazelcastInstance");
			assertThat(postProcessors(context)).doesNotContainKey(POST_PROCESSOR_BEAN_NAME);
		});
	}

	@Test
	void noRegistrationWithNoHazelcastInstance() {
		this.contextRunner.run((context) -> {
			assertThat(entityManagerFactoryDependencies(context)).doesNotContain("hazelcastInstance");
			assertThat(postProcessors(context)).doesNotContainKey(POST_PROCESSOR_BEAN_NAME);
		});
	}

	@Test
	void noRegistrationWithNoEntityManagerFactory() {
		new ApplicationContextRunner().withUserConfiguration(HazelcastConfiguration.class)
				.withConfiguration(AutoConfigurations.of(HazelcastJpaDependencyAutoConfiguration.class))
				.run((context) -> assertThat(postProcessors(context)).doesNotContainKey(POST_PROCESSOR_BEAN_NAME));
	}

	private Map<String, EntityManagerFactoryDependsOnPostProcessor> postProcessors(
			AssertableApplicationContext context) {
		return context.getBeansOfType(EntityManagerFactoryDependsOnPostProcessor.class);
	}

	private List<String> entityManagerFactoryDependencies(AssertableApplicationContext context) {
		String[] dependsOn = ((BeanDefinitionRegistry) context.getSourceApplicationContext())
				.getBeanDefinition("entityManagerFactory").getDependsOn();
		return (dependsOn != null) ? Arrays.asList(dependsOn) : Collections.emptyList();
	}

	@Configuration(proxyBeanMethods = false)
	static class HazelcastConfiguration {

		@Bean
		HazelcastInstance hazelcastInstance() {
			return mock(HazelcastInstance.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class HazelcastCustomNameConfiguration {

		@Bean
		HazelcastInstance myHazelcastInstance() {
			return mock(HazelcastInstance.class);
		}

	}

}
