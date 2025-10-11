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

package org.springframework.boot.devtools.autoconfigure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import io.r2dbc.spi.ConnectionFactory;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.boot.devtools.autoconfigure.DevToolsR2dbcAutoConfiguration.R2dbcDatabaseShutdownEvent;
import org.springframework.boot.r2dbc.SimpleConnectionFactoryProvider.SimpleTestConnectionFactory;
import org.springframework.boot.r2dbc.autoconfigure.R2dbcAutoConfiguration;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.testsupport.classpath.ClassPathExclusions;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ObjectUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DevToolsR2dbcAutoConfiguration}.
 *
 * @author Phillip Webb
 */
class DevToolsR2dbcAutoConfigurationTests {

	static List<ConnectionFactory> shutdowns = Collections.synchronizedList(new ArrayList<>());

	abstract static class Common {

		@BeforeEach
		void reset() {
			shutdowns.clear();
		}

		@Test
		void autoConfiguredInMemoryConnectionFactoryIsShutdown() throws Exception {
			ConfigurableApplicationContext context = getContext(this::createContext);
			ConnectionFactory connectionFactory = context.getBean(ConnectionFactory.class);
			context.close();
			assertThat(shutdowns).contains(connectionFactory);
		}

		@Test
		void nonEmbeddedConnectionFactoryIsNotShutdown() throws Exception {
			try (ConfigurableApplicationContext context = getContext(() -> createContext("r2dbc:h2:file:///testdb"))) {
				ConnectionFactory connectionFactory = context.getBean(ConnectionFactory.class);
				context.close();
				assertThat(shutdowns).doesNotContain(connectionFactory);
			}
		}

		@Test
		void singleManuallyConfiguredConnectionFactoryIsNotClosed() throws Exception {
			try (ConfigurableApplicationContext context = getContext(
					() -> createContext(SingleConnectionFactoryConfiguration.class))) {
				ConnectionFactory connectionFactory = context.getBean(ConnectionFactory.class);
				context.close();
				assertThat(shutdowns).doesNotContain(connectionFactory);
			}
		}

		@Test
		void multipleConnectionFactoriesAreIgnored() throws Exception {
			try (ConfigurableApplicationContext context = getContext(
					() -> createContext(MultipleConnectionFactoriesConfiguration.class))) {
				Collection<ConnectionFactory> connectionFactory = context.getBeansOfType(ConnectionFactory.class)
					.values();
				context.close();
				assertThat(shutdowns).doesNotContainAnyElementsOf(connectionFactory);
			}
		}

		@Test
		void emptyFactoryMethodMetadataIgnored() throws Exception {
			ConfigurableApplicationContext context = getContext(this::getEmptyFactoryMethodMetadataIgnoredContext);
			ConnectionFactory connectionFactory = context.getBean(ConnectionFactory.class);
			context.close();
			assertThat(shutdowns).doesNotContain(connectionFactory);
		}

		private ConfigurableApplicationContext getEmptyFactoryMethodMetadataIgnoredContext() {
			AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
			ConnectionFactory connectionFactory = new SimpleTestConnectionFactory();
			AnnotatedGenericBeanDefinition beanDefinition = new AnnotatedGenericBeanDefinition(
					connectionFactory.getClass());
			context.registerBeanDefinition("connectionFactory", beanDefinition);
			context.register(R2dbcAutoConfiguration.class, DevToolsR2dbcAutoConfiguration.class);
			context.refresh();
			return context;
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

		protected final ConfigurableApplicationContext createContext(Class<?>... classes) {
			return createContext(null, classes);
		}

		protected final ConfigurableApplicationContext createContext(@Nullable String url, Class<?>... classes) {
			AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
			if (!ObjectUtils.isEmpty(classes)) {
				context.register(classes);
			}
			context.register(R2dbcAutoConfiguration.class, DevToolsR2dbcAutoConfiguration.class);
			if (url != null) {
				TestPropertyValues.of("spring.r2dbc.url:" + url).applyTo(context);
			}
			context.addApplicationListener(ApplicationListener.forPayload(this::onEvent));
			context.refresh();
			return context;
		}

		private void onEvent(R2dbcDatabaseShutdownEvent event) {
			shutdowns.add(event.getConnectionFactory());
		}

	}

	@Nested
	@ClassPathExclusions("r2dbc-pool*.jar")
	class Embedded extends Common {

	}

	@Nested
	class Pooled extends Common {

	}

	@Configuration(proxyBeanMethods = false)
	static class SingleConnectionFactoryConfiguration {

		@Bean
		ConnectionFactory connectionFactory() {
			return new SimpleTestConnectionFactory();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class MultipleConnectionFactoriesConfiguration {

		@Bean
		ConnectionFactory connectionFactoryOne() {
			return new SimpleTestConnectionFactory();
		}

		@Bean
		ConnectionFactory connectionFactoryTwo() {
			return new SimpleTestConnectionFactory();
		}

	}

}
