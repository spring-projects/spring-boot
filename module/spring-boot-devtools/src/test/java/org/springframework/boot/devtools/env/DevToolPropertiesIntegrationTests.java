/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.devtools.env;

import java.net.URL;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.devtools.restart.RestartInitializer;
import org.springframework.boot.devtools.restart.Restarter;
import org.springframework.boot.testsupport.classpath.ForkedClassPath;
import org.springframework.boot.testsupport.classpath.resources.WithResource;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Integration tests for the configuration of development-time properties
 *
 * @author Andy Wilkinson
 */
class DevToolPropertiesIntegrationTests {

	private @Nullable ConfigurableApplicationContext context;

	@BeforeEach
	void setup() {
		Restarter.initialize(new String[] {}, false, new MockInitializer(), false);
	}

	@AfterEach
	void cleanup() {
		if (this.context != null) {
			this.context.close();
		}
		Restarter.clearInstance();
	}

	@Test
	@ForkedClassPath
	@WithResource(name = "META-INF/spring-devtools.properties", content = "defaults.com.example.enabled=true")
	void classPropertyConditionIsAffectedByDevToolProperties() throws Exception {
		SpringApplication application = new SpringApplication(ClassConditionConfiguration.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		this.context = getContext(application::run);
		this.context.getBean(ClassConditionConfiguration.class);
	}

	@Test
	@ForkedClassPath
	@WithResource(name = "META-INF/spring-devtools.properties", content = "defaults.com.example.enabled=true")
	void beanMethodPropertyConditionIsAffectedByDevToolProperties() throws Exception {
		SpringApplication application = new SpringApplication(BeanConditionConfiguration.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		this.context = getContext(application::run);
		this.context.getBean(MyBean.class);
	}

	@Test
	@ForkedClassPath
	@WithResource(name = "META-INF/spring-devtools.properties", content = "defaults.com.example.enabled=true")
	void postProcessWhenRestarterDisabledAndRemoteSecretNotSetShouldNotAddPropertySource() throws Exception {
		Restarter.clearInstance();
		Restarter.disable();
		SpringApplication application = new SpringApplication(BeanConditionConfiguration.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		this.context = getContext(application::run);
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class).isThrownBy(() -> {
			assertThat(this.context).isNotNull();
			this.context.getBean(MyBean.class);
		});
	}

	@Test
	@ForkedClassPath
	@WithResource(name = "META-INF/spring-devtools.properties", content = "defaults.com.example.enabled=true")
	void postProcessWhenRestarterDisabledAndRemoteSecretSetShouldAddPropertySource() throws Exception {
		Restarter.clearInstance();
		Restarter.disable();
		SpringApplication application = new SpringApplication(BeanConditionConfiguration.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		application.setDefaultProperties(Collections.singletonMap("spring.devtools.remote.secret", "donttell"));
		this.context = getContext(application::run);
		this.context.getBean(MyBean.class);
	}

	@Test
	@ForkedClassPath
	@WithResource(name = "META-INF/spring-devtools.properties", content = """
			defaults.com.example.one=alpha
			defaults.com.example.two=bravo
			""")
	void postProcessSetsPropertyDefaults() throws Exception {
		SpringApplication application = new SpringApplication(TestConfiguration.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		this.context = getContext(application::run);
		ConfigurableEnvironment environment = this.context.getEnvironment();
		String one = environment.getProperty("com.example.one");
		assertThat(one).isEqualTo("alpha");
		String two = environment.getProperty("com.example.two");
		assertThat(two).isEqualTo("bravo");
	}

	protected ConfigurableApplicationContext getContext(Supplier<ConfigurableApplicationContext> supplier)
			throws Exception {
		AtomicReference<ConfigurableApplicationContext> atomicReference = new AtomicReference<>();
		Thread thread = new Thread(() -> {
			ConfigurableApplicationContext context = supplier.get();
			atomicReference.getAndSet(context);
		});
		thread.start();
		thread.join();
		ConfigurableApplicationContext context = atomicReference.get();
		assertThat(context).isNotNull();
		return context;
	}

	@Configuration(proxyBeanMethods = false)
	static class TestConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnProperty("com.example.enabled")
	static class ClassConditionConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	static class BeanConditionConfiguration {

		@Bean
		@ConditionalOnProperty("com.example.enabled")
		MyBean myBean() {
			return new MyBean();
		}

	}

	static class MyBean {

	}

	static class MockInitializer implements RestartInitializer {

		@Override
		public URL[] getInitialUrls(Thread thread) {
			return new URL[] {};
		}

	}

}
