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

package org.springframework.boot.devtools.autoconfigure;

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration;
import org.springframework.boot.devtools.autoconfigure.DevToolsR2dbcAutoConfiguration.DevToolsConnectionFactoryCondition;
import org.springframework.boot.r2dbc.EmbeddedDatabaseConnection;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ConfigurationCondition;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.core.type.MethodMetadata;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for DevTools-specific R2DBC
 * configuration.
 *
 * @author Phillip Webb
 * @since 2.5.6
 */
@AutoConfigureAfter(R2dbcAutoConfiguration.class)
@ConditionalOnClass(ConnectionFactory.class)
@Conditional({ OnEnabledDevToolsCondition.class, DevToolsConnectionFactoryCondition.class })
@Configuration(proxyBeanMethods = false)
public class DevToolsR2dbcAutoConfiguration {

	@Bean
	InMemoryR2dbcDatabaseShutdownExecutor inMemoryR2dbcDatabaseShutdownExecutor(
			ApplicationEventPublisher eventPublisher, ConnectionFactory connectionFactory) {
		return new InMemoryR2dbcDatabaseShutdownExecutor(eventPublisher, connectionFactory);
	}

	final class InMemoryR2dbcDatabaseShutdownExecutor implements DisposableBean {

		private final ApplicationEventPublisher eventPublisher;

		private final ConnectionFactory connectionFactory;

		InMemoryR2dbcDatabaseShutdownExecutor(ApplicationEventPublisher eventPublisher,
				ConnectionFactory connectionFactory) {
			this.eventPublisher = eventPublisher;
			this.connectionFactory = connectionFactory;
		}

		@Override
		public void destroy() throws Exception {
			if (shouldShutdown()) {
				Mono.usingWhen(this.connectionFactory.create(), this::executeShutdown, this::closeConnection,
						this::closeConnection, this::closeConnection).block();
				this.eventPublisher.publishEvent(new R2dbcDatabaseShutdownEvent(this.connectionFactory));
			}
		}

		private boolean shouldShutdown() {
			try {
				return EmbeddedDatabaseConnection.isEmbedded(this.connectionFactory);
			}
			catch (Exception ex) {
				return false;
			}
		}

		private Mono<?> executeShutdown(Connection connection) {
			return Mono.from(connection.createStatement("SHUTDOWN").execute());
		}

		private Publisher<Void> closeConnection(Connection connection) {
			return closeConnection(connection, null);
		}

		private Publisher<Void> closeConnection(Connection connection, Throwable ex) {
			return connection.close();
		}

	}

	static class DevToolsConnectionFactoryCondition extends SpringBootCondition implements ConfigurationCondition {

		@Override
		public ConfigurationPhase getConfigurationPhase() {
			return ConfigurationPhase.REGISTER_BEAN;
		}

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
			ConditionMessage.Builder message = ConditionMessage.forCondition("DevTools ConnectionFactory Condition");
			String[] beanNames = context.getBeanFactory().getBeanNamesForType(ConnectionFactory.class, true, false);
			if (beanNames.length != 1) {
				return ConditionOutcome.noMatch(message.didNotFind("a single ConnectionFactory bean").atAll());
			}
			BeanDefinition beanDefinition = context.getRegistry().getBeanDefinition(beanNames[0]);
			if (beanDefinition instanceof AnnotatedBeanDefinition
					&& isAutoConfigured((AnnotatedBeanDefinition) beanDefinition)) {
				return ConditionOutcome.match(message.foundExactly("auto-configured ConnectionFactory"));
			}
			return ConditionOutcome.noMatch(message.didNotFind("an auto-configured ConnectionFactory").atAll());
		}

		private boolean isAutoConfigured(AnnotatedBeanDefinition beanDefinition) {
			MethodMetadata methodMetadata = beanDefinition.getFactoryMethodMetadata();
			return methodMetadata != null && methodMetadata.getDeclaringClassName()
					.startsWith(R2dbcAutoConfiguration.class.getPackage().getName());
		}

	}

	static class R2dbcDatabaseShutdownEvent {

		private final ConnectionFactory connectionFactory;

		R2dbcDatabaseShutdownEvent(ConnectionFactory connectionFactory) {
			this.connectionFactory = connectionFactory;
		}

		ConnectionFactory getConnectionFactory() {
			return this.connectionFactory;
		}

	}

}
