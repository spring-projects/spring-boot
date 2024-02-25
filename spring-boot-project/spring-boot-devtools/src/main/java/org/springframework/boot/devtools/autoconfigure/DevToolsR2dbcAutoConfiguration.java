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

package org.springframework.boot.devtools.autoconfigure;

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
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
@ConditionalOnClass(ConnectionFactory.class)
@Conditional({ OnEnabledDevToolsCondition.class, DevToolsConnectionFactoryCondition.class })
@AutoConfiguration(after = R2dbcAutoConfiguration.class)
public class DevToolsR2dbcAutoConfiguration {

	/**
     * Creates a new instance of {@link InMemoryR2dbcDatabaseShutdownExecutor}.
     * 
     * @param eventPublisher the {@link ApplicationEventPublisher} used to publish events
     * @param connectionFactory the {@link ConnectionFactory} used to establish database connections
     * @return a new instance of {@link InMemoryR2dbcDatabaseShutdownExecutor}
     */
    @Bean
	InMemoryR2dbcDatabaseShutdownExecutor inMemoryR2dbcDatabaseShutdownExecutor(
			ApplicationEventPublisher eventPublisher, ConnectionFactory connectionFactory) {
		return new InMemoryR2dbcDatabaseShutdownExecutor(eventPublisher, connectionFactory);
	}

	/**
     * InMemoryR2dbcDatabaseShutdownExecutor class.
     */
    final class InMemoryR2dbcDatabaseShutdownExecutor implements DisposableBean {

		private final ApplicationEventPublisher eventPublisher;

		private final ConnectionFactory connectionFactory;

		/**
         * Constructs a new InMemoryR2dbcDatabaseShutdownExecutor with the specified ApplicationEventPublisher and ConnectionFactory.
         * 
         * @param eventPublisher the ApplicationEventPublisher used to publish events
         * @param connectionFactory the ConnectionFactory used to establish database connections
         */
        InMemoryR2dbcDatabaseShutdownExecutor(ApplicationEventPublisher eventPublisher,
				ConnectionFactory connectionFactory) {
			this.eventPublisher = eventPublisher;
			this.connectionFactory = connectionFactory;
		}

		/**
         * This method is called when the application is shutting down. It checks if the database should be shutdown and if so,
         * it executes the shutdown process.
         *
         * @throws Exception if an error occurs during the shutdown process
         */
        @Override
		public void destroy() throws Exception {
			if (shouldShutdown()) {
				Mono.usingWhen(this.connectionFactory.create(), this::executeShutdown, this::closeConnection,
						this::closeConnection, this::closeConnection)
					.block();
				this.eventPublisher.publishEvent(new R2dbcDatabaseShutdownEvent(this.connectionFactory));
			}
		}

		/**
         * Determines whether the application should be shut down based on the type of database connection.
         * 
         * @return {@code true} if the database connection is an embedded database connection, {@code false} otherwise.
         */
        private boolean shouldShutdown() {
			try {
				return EmbeddedDatabaseConnection.isEmbedded(this.connectionFactory);
			}
			catch (Exception ex) {
				return false;
			}
		}

		/**
         * Executes a shutdown command on the given database connection.
         *
         * @param connection the database connection to execute the shutdown command on
         * @return a Mono that represents the completion of the shutdown command
         */
        private Mono<?> executeShutdown(Connection connection) {
			return Mono.from(connection.createStatement("SHUTDOWN").execute());
		}

		/**
         * Closes the given connection and returns a Publisher that completes when the connection is closed.
         * 
         * @param connection the connection to be closed
         * @return a Publisher that completes when the connection is closed
         */
        private Publisher<Void> closeConnection(Connection connection) {
			return closeConnection(connection, null);
		}

		/**
         * Closes the given connection and returns a Publisher that completes when the connection is closed.
         *
         * @param connection the connection to be closed
         * @param ex the throwable that caused the connection to be closed, or null if the connection was closed normally
         * @return a Publisher that completes when the connection is closed
         */
        private Publisher<Void> closeConnection(Connection connection, Throwable ex) {
			return connection.close();
		}

	}

	/**
     * DevToolsConnectionFactoryCondition class.
     */
    static class DevToolsConnectionFactoryCondition extends SpringBootCondition implements ConfigurationCondition {

		/**
         * Returns the configuration phase of this method.
         * 
         * @return The configuration phase of this method.
         */
        @Override
		public ConfigurationPhase getConfigurationPhase() {
			return ConfigurationPhase.REGISTER_BEAN;
		}

		/**
         * Determines the match outcome for the DevTools ConnectionFactory Condition.
         * 
         * @param context the condition context
         * @param metadata the annotated type metadata
         * @return the condition outcome
         */
        @Override
		public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
			ConditionMessage.Builder message = ConditionMessage.forCondition("DevTools ConnectionFactory Condition");
			String[] beanNames = context.getBeanFactory().getBeanNamesForType(ConnectionFactory.class, true, false);
			if (beanNames.length != 1) {
				return ConditionOutcome.noMatch(message.didNotFind("a single ConnectionFactory bean").atAll());
			}
			BeanDefinition beanDefinition = context.getRegistry().getBeanDefinition(beanNames[0]);
			if (beanDefinition instanceof AnnotatedBeanDefinition annotatedBeanDefinition
					&& isAutoConfigured(annotatedBeanDefinition)) {
				return ConditionOutcome.match(message.foundExactly("auto-configured ConnectionFactory"));
			}
			return ConditionOutcome.noMatch(message.didNotFind("an auto-configured ConnectionFactory").atAll());
		}

		/**
         * Determines if the given bean definition is auto-configured.
         * 
         * @param beanDefinition the annotated bean definition to check
         * @return true if the bean definition is auto-configured, false otherwise
         */
        private boolean isAutoConfigured(AnnotatedBeanDefinition beanDefinition) {
			MethodMetadata methodMetadata = beanDefinition.getFactoryMethodMetadata();
			return methodMetadata != null && methodMetadata.getDeclaringClassName()
				.startsWith(R2dbcAutoConfiguration.class.getPackage().getName());
		}

	}

	/**
     * R2dbcDatabaseShutdownEvent class.
     */
    static class R2dbcDatabaseShutdownEvent {

		private final ConnectionFactory connectionFactory;

		/**
         * Constructs a new R2dbcDatabaseShutdownEvent with the specified ConnectionFactory.
         *
         * @param connectionFactory the ConnectionFactory associated with the event
         */
        R2dbcDatabaseShutdownEvent(ConnectionFactory connectionFactory) {
			this.connectionFactory = connectionFactory;
		}

		/**
         * Returns the connection factory associated with this event.
         *
         * @return the connection factory associated with this event
         */
        ConnectionFactory getConnectionFactory() {
			return this.connectionFactory;
		}

	}

}
