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
 */
abstract class ConnectionFactoryConfigurations {

	protected static ConnectionFactory createConnectionFactory(R2dbcProperties properties, ClassLoader classLoader,
			List<ConnectionFactoryOptionsBuilderCustomizer> optionsCustomizers) {
		return ConnectionFactoryBuilder.of(properties, () -> EmbeddedDatabaseConnection.get(classLoader))
				.configure((options) -> {
					for (ConnectionFactoryOptionsBuilderCustomizer optionsCustomizer : optionsCustomizers) {
						optionsCustomizer.customize(options);
					}
				}).build();
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(ConnectionPool.class)
	@Conditional(PooledConnectionFactoryCondition.class)
	@ConditionalOnMissingBean(ConnectionFactory.class)
	static class Pool {

		@Bean(destroyMethod = "dispose")
		ConnectionPool connectionFactory(R2dbcProperties properties, ResourceLoader resourceLoader,
				ObjectProvider<ConnectionFactoryOptionsBuilderCustomizer> customizers) {
			ConnectionFactory connectionFactory = createConnectionFactory(properties, resourceLoader.getClassLoader(),
					customizers.orderedStream().collect(Collectors.toList()));
			R2dbcProperties.Pool pool = properties.getPool();
			ConnectionPoolConfiguration.Builder builder = ConnectionPoolConfiguration.builder(connectionFactory)
					.maxSize(pool.getMaxSize()).initialSize(pool.getInitialSize()).maxIdleTime(pool.getMaxIdleTime());
			if (StringUtils.hasText(pool.getValidationQuery())) {
				builder.validationQuery(pool.getValidationQuery());
			}
			return new ConnectionPool(builder.build());
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnProperty(prefix = "spring.r2dbc.pool", value = "enabled", havingValue = "false",
			matchIfMissing = true)
	@ConditionalOnMissingBean(ConnectionFactory.class)
	static class Generic {

		@Bean
		ConnectionFactory connectionFactory(R2dbcProperties properties, ResourceLoader resourceLoader,
				ObjectProvider<ConnectionFactoryOptionsBuilderCustomizer> customizers) {
			return createConnectionFactory(properties, resourceLoader.getClassLoader(),
					customizers.orderedStream().collect(Collectors.toList()));
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

}
