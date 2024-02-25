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

package org.springframework.boot.autoconfigure.r2dbc;

import java.util.List;

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
import org.springframework.boot.r2dbc.ConnectionFactoryDecorator;
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
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Moritz Halbritter
 */
abstract class ConnectionFactoryConfigurations {

	/**
	 * Creates a connection factory with the given properties, connection details, class
	 * loader, options customizers, and decorators.
	 * @param properties the R2dbcProperties object containing the connection properties
	 * @param connectionDetails the R2dbcConnectionDetails object containing the
	 * connection details
	 * @param classLoader the ClassLoader to use for loading classes
	 * @param optionsCustomizers the list of ConnectionFactoryOptionsBuilderCustomizer
	 * objects to customize the options
	 * @param decorators the list of ConnectionFactoryDecorator objects to decorate the
	 * connection factory
	 * @return the created ConnectionFactory object
	 * @throws MissingR2dbcPoolDependencyException if the R2DBC pool dependency is missing
	 * @throws IllegalStateException if an error occurs while creating the connection
	 * factory
	 */
	protected static ConnectionFactory createConnectionFactory(R2dbcProperties properties,
			R2dbcConnectionDetails connectionDetails, ClassLoader classLoader,
			List<ConnectionFactoryOptionsBuilderCustomizer> optionsCustomizers,
			List<ConnectionFactoryDecorator> decorators) {
		try {
			return org.springframework.boot.r2dbc.ConnectionFactoryBuilder
				.withOptions(new ConnectionFactoryOptionsInitializer().initialize(properties, connectionDetails,
						() -> EmbeddedDatabaseConnection.get(classLoader)))
				.configure((options) -> {
					for (ConnectionFactoryOptionsBuilderCustomizer optionsCustomizer : optionsCustomizers) {
						optionsCustomizer.customize(options);
					}
				})
				.decorators(decorators)
				.build();
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

	/**
	 * PoolConfiguration class.
	 */
	@Configuration(proxyBeanMethods = false)
	@Conditional(PooledConnectionFactoryCondition.class)
	@ConditionalOnMissingBean(ConnectionFactory.class)
	static class PoolConfiguration {

		/**
		 * PooledConnectionFactoryConfiguration class.
		 */
		@Configuration(proxyBeanMethods = false)
		@ConditionalOnClass(ConnectionPool.class)
		static class PooledConnectionFactoryConfiguration {

			/**
			 * Creates a connection pool using the provided R2dbcProperties,
			 * connectionDetails, resourceLoader, customizers, and decorators.
			 * @param properties the R2dbcProperties containing the pool configuration
			 * @param connectionDetails the R2dbcConnectionDetails for establishing the
			 * connection
			 * @param resourceLoader the ResourceLoader for loading resources
			 * @param customizers the customizers for customizing the
			 * ConnectionFactoryOptionsBuilder
			 * @param decorators the decorators for decorating the ConnectionFactory
			 * @return a ConnectionPool instance
			 */
			@Bean(destroyMethod = "dispose")
			ConnectionPool connectionFactory(R2dbcProperties properties,
					ObjectProvider<R2dbcConnectionDetails> connectionDetails, ResourceLoader resourceLoader,
					ObjectProvider<ConnectionFactoryOptionsBuilderCustomizer> customizers,
					ObjectProvider<ConnectionFactoryDecorator> decorators) {
				ConnectionFactory connectionFactory = createConnectionFactory(properties,
						connectionDetails.getIfAvailable(), resourceLoader.getClassLoader(),
						customizers.orderedStream().toList(), decorators.orderedStream().toList());
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
				map.from(pool.getMinIdle()).to(builder::minIdle);
				map.from(pool.getMaxValidationTime()).to(builder::maxValidationTime);
				return new ConnectionPool(builder.build());
			}

		}

	}

	/**
	 * GenericConfiguration class.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnProperty(prefix = "spring.r2dbc.pool", value = "enabled", havingValue = "false",
			matchIfMissing = true)
	@ConditionalOnMissingBean(ConnectionFactory.class)
	static class GenericConfiguration {

		/**
		 * Creates a ConnectionFactory bean using the provided R2dbcProperties,
		 * connectionDetails, resourceLoader, customizers, and decorators.
		 * @param properties The R2dbcProperties object containing the configuration
		 * properties for the ConnectionFactory.
		 * @param connectionDetails The R2dbcConnectionDetails object containing the
		 * connection details for the ConnectionFactory.
		 * @param resourceLoader The ResourceLoader object used to load resources.
		 * @param customizers The ObjectProvider of
		 * ConnectionFactoryOptionsBuilderCustomizer objects used to customize the
		 * ConnectionFactory options.
		 * @param decorators The ObjectProvider of ConnectionFactoryDecorator objects used
		 * to decorate the ConnectionFactory.
		 * @return The created ConnectionFactory bean.
		 */
		@Bean
		ConnectionFactory connectionFactory(R2dbcProperties properties,
				ObjectProvider<R2dbcConnectionDetails> connectionDetails, ResourceLoader resourceLoader,
				ObjectProvider<ConnectionFactoryOptionsBuilderCustomizer> customizers,
				ObjectProvider<ConnectionFactoryDecorator> decorators) {
			return createConnectionFactory(properties, connectionDetails.getIfAvailable(),
					resourceLoader.getClassLoader(), customizers.orderedStream().toList(),
					decorators.orderedStream().toList());
		}

	}

	/**
	 * {@link Condition} that checks that a {@link ConnectionPool} is requested. The
	 * condition matches if pooling was opt-in through configuration. If any of the
	 * spring.r2dbc.pool.* properties have been configured, an exception is thrown if the
	 * URL also contains pooling-related options or io.r2dbc.pool.ConnectionPool is not on
	 * the class path.
	 */
	static class PooledConnectionFactoryCondition extends SpringBootCondition {

		/**
		 * Determines the match outcome for the PooledConnectionFactoryCondition.
		 * @param context the condition context
		 * @param metadata the annotated type metadata
		 * @return the condition outcome
		 */
		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
			BindResult<Pool> pool = Binder.get(context.getEnvironment())
				.bind("spring.r2dbc.pool", Bindable.of(Pool.class));
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

		/**
		 * Checks if the given environment has a pool URL.
		 * @param environment the environment to check
		 * @return true if the environment has a pool URL, false otherwise
		 */
		private boolean hasPoolUrl(Environment environment) {
			String url = environment.getProperty("spring.r2dbc.url");
			return StringUtils.hasText(url) && url.contains(":pool:");
		}

	}

}
