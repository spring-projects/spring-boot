/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.autoconfigure.r2dbc;

import java.util.UUID;

import io.r2dbc.h2.H2ConnectionFactory;
import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.proxy.callback.ProxyConfig;
import io.r2dbc.proxy.callback.ProxyConfigHolder;
import io.r2dbc.proxy.listener.ProxyExecutionListener;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Wrapped;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.r2dbc.ConnectionFactoryConfigurations.ProxyConnectionFactoryPostProcessor;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link R2dbcAutoConfiguration} with R2DBC Proxy.
 *
 * @author Tadaya Tsuyukubo
 */
class R2dbcAutoConfigurationProxyTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(R2dbcAutoConfiguration.class));

	@Test
	void configureWithProxyExecutionListenerBean() {
		ProxyExecutionListener listener = mock(ProxyExecutionListener.class);
		this.contextRunner.withBean(ProxyExecutionListener.class, () -> listener).run((context) -> {
			assertThat(context).hasSingleBean(ConnectionFactory.class);
			ConnectionFactory connectionFactory = context.getBean(ConnectionFactory.class);
			assertThat(connectionFactory).isInstanceOf(Wrapped.class).extracting(this::unwrapProxyConnectionFactory)
					.isExactlyInstanceOf(ConnectionPool.class);

			ProxyConfig proxyConfig = extractProxyConfig(connectionFactory);
			assertThat(proxyConfig.getListeners().getListeners()).containsExactly(listener);
		});
	}

	@Test
	void configureWithProxyConfigBean() {
		ProxyConfig proxyConfig = ProxyConfig.builder().build();
		this.contextRunner.withBean(ProxyConfig.class, () -> proxyConfig).run((context) -> {
			ConnectionFactory connectionFactory = context.getBean(ConnectionFactory.class);
			ProxyConfig result = extractProxyConfig(connectionFactory);
			assertThat(result).isSameAs(proxyConfig);
		});
	}

	@Test
	void configureWithNoListenerNoProxyConfigBeans() {
		ConnectionFactory sourceConnectionFactory = ConnectionFactoryBuilder
				.of(new R2dbcProperties(), () -> EmbeddedDatabaseConnection.H2).build();
		this.contextRunner.withBean(ConnectionFactory.class, () -> sourceConnectionFactory).run((context) -> {
			assertThat(context).hasSingleBean(ConnectionFactory.class);
			ConnectionFactory bean = context.getBean(ConnectionFactory.class);
			assertThat(bean).isNotInstanceOf(Wrapped.class).isSameAs(sourceConnectionFactory);
		});
	}

	@Test
	void configureWithNoConnectionFactory() {
		this.contextRunner.run((context) -> assertThat(context).hasNotFailed()
				.doesNotHaveBean(ProxyConnectionFactoryPostProcessor.class));
	}

	@Test
	void configureWithNoR2dbcProxyLibrary() {
		this.contextRunner.withClassLoader(new FilteredClassLoader("io.r2dbc.proxy"))
				.withUserConfiguration(ProxyExecutionListenerConfiguration.class).run((context) -> {
					assertThat(context).doesNotHaveBean(ProxyConnectionFactoryPostProcessor.class)
							.hasSingleBean(ConnectionFactory.class);
					ConnectionFactory connectionFactory = context.getBean(ConnectionFactory.class);
					assertThat(connectionFactory).isExactlyInstanceOf(ConnectionPool.class);
				});
	}

	@Test
	void configureWithGenericConnectionFactory() {
		String dbName = "testdb-" + UUID.randomUUID();
		this.contextRunner.withClassLoader(new FilteredClassLoader("io.r2dbc.pool"))
				.withPropertyValues("spring.r2dbc.url:r2dbc:h2:mem:///" + dbName
						+ "?options=DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
				.withUserConfiguration(ProxyExecutionListenerConfiguration.class).run((context) -> {
					assertThat(context).hasSingleBean(ConnectionFactory.class);
					ConnectionFactory connectionFactory = context.getBean(ConnectionFactory.class);
					assertThat(connectionFactory).isInstanceOf(Wrapped.class)
							.extracting(this::unwrapProxyConnectionFactory)
							.isExactlyInstanceOf(H2ConnectionFactory.class);
				});
	}

	@Test
	void configureWithProxyInUrl() {
		ProxyExecutionListener listener = mock(ProxyExecutionListener.class);
		String dbName = "testdb-" + UUID.randomUUID();
		this.contextRunner.withClassLoader(new FilteredClassLoader("io.r2dbc.pool"))
				.withPropertyValues("spring.r2dbc.url:r2dbc:proxy:h2:mem:///" + dbName
						+ "?options=DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
				.withBean(ProxyExecutionListener.class, () -> listener).run((context) -> {
					ConnectionFactory connectionFactory = context.getBean(ConnectionFactory.class);
					assertThat(connectionFactory).isInstanceOf(Wrapped.class);
					ProxyConfig proxyConfig = extractProxyConfig(connectionFactory);
					assertThat(proxyConfig.getListeners().getListeners()).isEmpty();
				});
	}

	@Test
	void configureWithEnabledFalse() {
		ConnectionFactory sourceConnectionFactory = ConnectionFactoryBuilder
				.of(new R2dbcProperties(), () -> EmbeddedDatabaseConnection.H2).build();
		this.contextRunner.withBean(ConnectionFactory.class, () -> sourceConnectionFactory)
				.withPropertyValues("spring.r2dbc.proxy.enabled=false")
				.withUserConfiguration(ProxyExecutionListenerConfiguration.class).run((context) -> {
					assertThat(context).hasSingleBean(ConnectionFactory.class);
					ConnectionFactory bean = context.getBean(ConnectionFactory.class);
					assertThat(bean).isNotInstanceOf(Wrapped.class);
				});
	}

	@SuppressWarnings("unchecked")
	private ConnectionFactory unwrapProxyConnectionFactory(ConnectionFactory connectionFactory) {
		assertThat(connectionFactory).isInstanceOf(Wrapped.class);
		return ((Wrapped<ConnectionFactory>) connectionFactory).unwrap();
	}

	private ProxyConfig extractProxyConfig(ConnectionFactory connectionFactory) {
		assertThat(connectionFactory).isInstanceOf(ProxyConfigHolder.class);
		return ((ProxyConfigHolder) connectionFactory).getProxyConfig();
	}

	@Configuration(proxyBeanMethods = false)
	private static class ProxyExecutionListenerConfiguration {

		@Bean
		ProxyExecutionListener proxyExecutionListener() {
			return mock(ProxyExecutionListener.class);
		}

	}

}
