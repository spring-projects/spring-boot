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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import io.r2dbc.proxy.ProxyConnectionFactory;
import io.r2dbc.proxy.callback.ProxyConfig;
import io.r2dbc.proxy.listener.ProxyExecutionListener;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Wrapped;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

/**
 * Actual {@link ConnectionFactory} configurations.
 *
 * @author Mark Paluch
 * @author Stephane Nicoll
 * @author Rodolpho S. Couto
 * @author Tadaya Tsuyukubo
 */
abstract class ConnectionFactoryConfigurations {

	protected static ConnectionFactory createConnectionFactory(R2dbcProperties properties, ClassLoader classLoader,
			List<ConnectionFactoryOptionsBuilderCustomizer> optionsCustomizers,
			Function<ConnectionFactory, ConnectionFactory> connectionfactoryPostProcessor) {
		ConnectionFactory connectionFactory = ConnectionFactoryBuilder
				.of(properties, () -> EmbeddedDatabaseConnection.get(classLoader)).configure((options) -> {
					for (ConnectionFactoryOptionsBuilderCustomizer optionsCustomizer : optionsCustomizers) {
						optionsCustomizer.customize(options);
					}
				}).build();
		return connectionfactoryPostProcessor.apply(connectionFactory);
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(ConnectionPool.class)
	@Conditional(PooledConnectionFactoryCondition.class)
	@ConditionalOnMissingBean(ConnectionFactory.class)
	static class Pool {

		@Bean
		ConnectionFactory connectionFactory(R2dbcProperties properties, ResourceLoader resourceLoader,
				ObjectProvider<ConnectionFactoryOptionsBuilderCustomizer> customizers,
				ObjectProvider<ProxyConnectionFactoryPostProcessor> proxyPostProcessorProvider) {
			Function<ConnectionFactory, ConnectionFactory> postProcessor = (connectionFactory) -> {
				R2dbcProperties.Pool pool = properties.getPool();
				PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
				ConnectionPoolConfiguration.Builder builder = ConnectionPoolConfiguration.builder(connectionFactory);
				map.from(pool.getMaxIdleTime()).to(builder::maxIdleTime);
				map.from(pool.getMaxLifeTime()).to(builder::maxLifeTime);
				map.from(pool.getMaxAcquireTime()).to(builder::maxAcquireTime);
				map.from(pool.getMaxCreateConnectionTime()).to(builder::maxCreateConnectionTime);
				map.from(pool.getInitialSize()).to(builder::initialSize);
				map.from(pool.getMaxSize()).to(builder::maxSize);
				map.from(pool.getValidationQuery()).whenHasText().to(builder::validationQuery);
				map.from(pool.getValidationDepth()).to(builder::validationDepth);
				return new ConnectionPool(builder.build());
			};

			ProxyConnectionFactoryPostProcessor proxyPostProcessor = proxyPostProcessorProvider.getIfAvailable();
			if (proxyPostProcessor != null) {
				postProcessor = postProcessor.andThen(proxyPostProcessor);
			}

			return createConnectionFactory(properties, resourceLoader.getClassLoader(),
					customizers.orderedStream().collect(Collectors.toList()), postProcessor);
		}

		@Bean
		DisposableBean connectionPoolDisposableBean(ConnectionFactory connectionFactory) {
			return () -> {
				if (connectionFactory instanceof ConnectionPool) {
					((ConnectionPool) connectionFactory).dispose();
				}
				else {
					if (connectionFactory instanceof Wrapped) {
						Object unwrapped = ((Wrapped<?>) connectionFactory).unwrap();
						if (unwrapped instanceof ConnectionPool) {
							((ConnectionPool) unwrapped).dispose();
						}
					}
				}
			};
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnProperty(prefix = "spring.r2dbc.pool", value = "enabled", havingValue = "false",
			matchIfMissing = true)
	@ConditionalOnMissingBean(ConnectionFactory.class)
	static class Generic {

		@Bean
		ConnectionFactory connectionFactory(R2dbcProperties properties, ResourceLoader resourceLoader,
				ObjectProvider<ConnectionFactoryOptionsBuilderCustomizer> customizers,
				ObjectProvider<ProxyConnectionFactoryPostProcessor> proxyPostProcessorProvider) {
			ProxyConnectionFactoryPostProcessor proxyPostProcessor = proxyPostProcessorProvider.getIfAvailable();
			Function<ConnectionFactory, ConnectionFactory> postProcessor = (proxyPostProcessor != null)
					? proxyPostProcessor : Function.identity();
			return createConnectionFactory(properties, resourceLoader.getClassLoader(),
					customizers.orderedStream().collect(Collectors.toList()), postProcessor);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(ProxyConnectionFactory.class)
	@Conditional({ ProxyConnectionFactoryPreCondition.class, ProxyBeanCondition.class })
	static class Proxy {

		@Bean
		ProxyConnectionFactoryPostProcessor proxyConnectionFactoryPostProcessor(
				ObjectProvider<ProxyExecutionListener> proxyExecutionListeners,
				ObjectProvider<ProxyConfig> proxyConfigProvider) {
			List<ProxyExecutionListener> listeners = new ArrayList<>();
			proxyExecutionListeners.orderedStream().forEach(listeners::add);
			return new ProxyConnectionFactoryPostProcessor(listeners, proxyConfigProvider.getIfUnique());
		}

	}

	static class ProxyConnectionFactoryPostProcessor implements Function<ConnectionFactory, ConnectionFactory> {

		private final List<ProxyExecutionListener> listeners;

		private final ProxyConfig proxyConfig;

		ProxyConnectionFactoryPostProcessor(List<ProxyExecutionListener> listeners, ProxyConfig proxyConfig) {
			this.listeners = listeners;
			this.proxyConfig = proxyConfig;
		}

		@Override
		public ConnectionFactory apply(ConnectionFactory connectionFactory) {
			ProxyConnectionFactory.Builder builder = ProxyConnectionFactory.builder(connectionFactory);
			if (this.proxyConfig != null) {
				builder.proxyConfig(this.proxyConfig);
			}
			this.listeners.forEach(builder::listener);
			return builder.build();
		}

	}

	/**
	 * {@link Condition} that checks that a {@link ConnectionPool} is requested. The
	 * condition matches if pooling was opt-in via configuration and the r2dbc url does
	 * not contain pooling-related options.
	 */
	static class PooledConnectionFactoryCondition extends SpringBootCondition {

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
			boolean poolEnabled = context.getEnvironment().getProperty("spring.r2dbc.pool.enabled", Boolean.class,
					true);
			if (poolEnabled) {
				// Make sure the URL does not have pool options
				String url = context.getEnvironment().getProperty("spring.r2dbc.url");
				boolean pooledUrl = StringUtils.hasText(url) && url.contains(":pool:");
				if (pooledUrl) {
					return ConditionOutcome.noMatch("R2DBC Connection URL contains pooling-related options");
				}
				return ConditionOutcome
						.match("Pooling is enabled and R2DBC Connection URL does not contain pooling-related options");
			}
			return ConditionOutcome.noMatch("Pooling is disabled");
		}

	}

	static class ProxyConnectionFactoryPreCondition extends SpringBootCondition {

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
			boolean proxyEnabled = context.getEnvironment().getProperty("spring.r2dbc.proxy.enabled", Boolean.class,
					true);
			if (!proxyEnabled) {
				return ConditionOutcome.noMatch("Proxy is disabled");
			}

			String url = context.getEnvironment().getProperty("spring.r2dbc.url");
			boolean proxyUrl = StringUtils.hasText(url) && url.contains(":proxy:");
			if (proxyUrl) {
				return ConditionOutcome.noMatch("R2DBC Connection URL contains proxy-related options");
			}

			return ConditionOutcome
					.match("Proxy is enabled and R2DBC Connection URL does not contain pooling-related options");
		}

	}

	static class ProxyBeanCondition extends AnyNestedCondition {

		ProxyBeanCondition() {
			super(ConfigurationPhase.REGISTER_BEAN);
		}

		@ConditionalOnBean(ProxyConfig.class)
		static final class ProxyConfigCondition {

		}

		@ConditionalOnBean(ProxyExecutionListener.class)
		static final class ProxyExecutionListenerCondition {

		}

	}

}
