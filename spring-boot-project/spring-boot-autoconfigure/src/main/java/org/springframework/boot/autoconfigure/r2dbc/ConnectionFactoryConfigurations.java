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

package org.springframework.boot.autoconfigure.r2dbc;

import java.util.List;
import java.util.stream.Collectors;

import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import io.r2dbc.spi.ConnectionFactory;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcProperties.Pool;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.r2dbc.EmbeddedDatabaseConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Actual {@link ConnectionFactory} configurations.
 *
 * @author Mark Paluch
 * @author Stephane Nicoll
 * @author Rodolpho S. Couto
 */
abstract class ConnectionFactoryConfigurations {

	protected static ConnectionFactory createConnectionFactory(R2dbcProperties properties, ClassLoader classLoader,
			List<ConnectionFactoryOptionsBuilderCustomizer> optionsCustomizers) {
		try {
			return org.springframework.boot.r2dbc.ConnectionFactoryBuilder
					.withOptions(new ConnectionFactoryOptionsInitializer().initialize(properties,
							() -> EmbeddedDatabaseConnection.get(classLoader)))
					.configure((options) -> {
						for (ConnectionFactoryOptionsBuilderCustomizer optionsCustomizer : optionsCustomizers) {
							optionsCustomizer.customize(options);
						}
					}).build();
		}
		catch (IllegalStateException ex) {
			String message = ex.getMessage();
			if (message != null && message.contains("driver=pool")
					&& !ClassUtils.isPresent("io.r2dbc.pool.ConnectionPool", classLoader)) {
				throw new MissingR2dbcPoolDependencyException();
			}
			throw ex;
		}
	}

	@Configuration(proxyBeanMethods = false)
	@Conditional(PooledConnectionFactoryCondition.class)
	@ConditionalOnMissingBean(ConnectionFactory.class)
	static class PoolConfiguration {

		@Configuration(proxyBeanMethods = false)
		@ConditionalOnClass(ConnectionPool.class)
		static class PooledConnectionFactoryConfiguration {

			@Bean(destroyMethod = "dispose")
			ConnectionPool connectionFactory(R2dbcProperties properties, ResourceLoader resourceLoader,
					ObjectProvider<ConnectionFactoryOptionsBuilderCustomizer> customizers) {
				ConnectionFactory connectionFactory = createConnectionFactory(properties,
						resourceLoader.getClassLoader(), customizers.orderedStream().collect(Collectors.toList()));
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
			}

		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnProperty(prefix = "spring.r2dbc.pool", value = "enabled", havingValue = "false",
			matchIfMissing = true)
	@ConditionalOnMissingBean(ConnectionFactory.class)
	static class GenericConfiguration {

		@Bean
		ConnectionFactory connectionFactory(R2dbcProperties properties, ResourceLoader resourceLoader,
				ObjectProvider<ConnectionFactoryOptionsBuilderCustomizer> customizers) {
			return createConnectionFactory(properties, resourceLoader.getClassLoader(),
					customizers.orderedStream().collect(Collectors.toList()));
		}

	}

	/**
	 * {@link Condition} that checks that a {@link ConnectionPool} is requested. The
	 * condition matches if pooling was opt-in via configuration. If any of the
	 * spring.r2dbc.pool.* properties have been configured, an exception is thrown if the
	 * URL also contains pooling-related options or io.r2dbc.pool.ConnectionPool is not on
	 * the class path.
	 */
	static class PooledConnectionFactoryCondition extends SpringBootCondition {

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
			BindResult<Pool> pool = Binder.get(context.getEnvironment()).bind("spring.r2dbc.pool",
					Bindable.of(Pool.class));
			if (hasPoolUrl(context.getEnvironment())) {
				if (pool.isBound()) {
					throw new MultipleConnectionPoolConfigurationsException();
				}
				return ConditionOutcome.noMatch("URL-based pooling has been configured");
			}
			if (pool.isBound() && !ClassUtils.isPresent("io.r2dbc.pool.ConnectionPool", context.getClassLoader())) {
				throw new MissingR2dbcPoolDependencyException();
			}
			if (pool.orElseGet(Pool::new).isEnabled()) {
				return ConditionOutcome.match("Property-based pooling is enabled");
			}
			return ConditionOutcome.noMatch("Property-based pooling is disabled");
		}

		private boolean hasPoolUrl(Environment environment) {
			String url = environment.getProperty("spring.r2dbc.url");
			return StringUtils.hasText(url) && url.contains(":pool:");
		}

	}

}
