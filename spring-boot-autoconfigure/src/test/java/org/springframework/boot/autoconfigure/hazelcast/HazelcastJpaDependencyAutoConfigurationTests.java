/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.autoconfigure.hazelcast;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.hazelcast.core.HazelcastInstance;
import org.junit.After;
import org.junit.Test;

import org.springframework.boot.autoconfigure.data.jpa.EntityManagerFactoryDependsOnPostProcessor;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link HazelcastJpaDependencyAutoConfiguration}.
 *
 * @author Stephane Nicoll
 */
public class HazelcastJpaDependencyAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@After
	public void closeContext() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void registrationIfHazelcastInstanceHasRegularBeanName() {
		load(HazelcastConfiguration.class);
		assertThat(getPostProcessor())
				.containsKey("hazelcastInstanceJpaDependencyPostProcessor");
		assertThat(getEntityManagerFactoryDependencies()).contains("hazelcastInstance");
	}

	@Test
	public void noRegistrationIfHazelcastInstanceHasCustomBeanName() {
		load(HazelcastCustomNameConfiguration.class);
		assertThat(getEntityManagerFactoryDependencies())
				.doesNotContain("hazelcastInstance");
		assertThat(getPostProcessor())
				.doesNotContainKey("hazelcastInstanceJpaDependencyPostProcessor");
	}

	@Test
	public void noRegistrationWithNoHazelcastInstance() {
		load(null);
		assertThat(getEntityManagerFactoryDependencies())
				.doesNotContain("hazelcastInstance");
		assertThat(getPostProcessor())
				.doesNotContainKey("hazelcastInstanceJpaDependencyPostProcessor");
	}

	@Test
	public void noRegistrationWithNoEntityManagerFactory() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(HazelcastConfiguration.class,
				HazelcastJpaDependencyAutoConfiguration.class);
		this.context.refresh();
		assertThat(getPostProcessor())
				.doesNotContainKey("hazelcastInstanceJpaDependencyPostProcessor");
	}

	private Map<String, EntityManagerFactoryDependsOnPostProcessor> getPostProcessor() {
		return this.context
				.getBeansOfType(EntityManagerFactoryDependsOnPostProcessor.class);
	}

	private List<String> getEntityManagerFactoryDependencies() {
		String[] dependsOn = this.context.getBeanDefinition("entityManagerFactory")
				.getDependsOn();
		return dependsOn != null ? Arrays.asList(dependsOn)
				: Collections.<String>emptyList();
	}

	public void load(Class<?> config) {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		if (config != null) {
			ctx.register(config);
		}
		ctx.register(EmbeddedDataSourceConfiguration.class,
				DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class);
		ctx.register(HazelcastJpaDependencyAutoConfiguration.class);
		ctx.refresh();
		this.context = ctx;
	}

	@Configuration
	static class HazelcastConfiguration {

		@Bean
		public HazelcastInstance hazelcastInstance() {
			return mock(HazelcastInstance.class);
		}

	}

	@Configuration
	static class HazelcastCustomNameConfiguration {

		@Bean
		public HazelcastInstance myHazelcastInstance() {
			return mock(HazelcastInstance.class);
		}

	}

}
