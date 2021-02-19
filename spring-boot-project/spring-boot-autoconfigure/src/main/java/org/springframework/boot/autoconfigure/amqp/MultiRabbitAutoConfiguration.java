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

package org.springframework.boot.autoconfigure.amqp;

import java.util.Map;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.impl.CredentialsProvider;
import com.rabbitmq.client.impl.CredentialsRefreshService;

import org.springframework.amqp.rabbit.config.RabbitListenerConfigUtils;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactoryContextWrapper;
import org.springframework.amqp.rabbit.connection.ConnectionNameStrategy;
import org.springframework.amqp.rabbit.connection.SimpleRoutingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration.RabbitConnectionFactoryCreator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ResourceLoader;

/**
 * Class responsible for auto-configuring the necessary beans to enable multiple RabbitMQ
 * servers.
 *
 * @author Wander Costa
 * @since 2.4
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ RabbitTemplate.class, Channel.class })
@EnableConfigurationProperties({ RabbitProperties.class, MultiRabbitProperties.class })
@Import(RabbitAnnotationDrivenConfiguration.class)
public class MultiRabbitAutoConfiguration {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnProperty(prefix = "spring.multirabbitmq", name = "enabled", havingValue = "true")
	protected static class MultiRabbitConnectionFactoryCreator extends RabbitConnectionFactoryCreator
			implements BeanFactoryAware, ApplicationContextAware {

		private ConfigurableListableBeanFactory beanFactory;

		private ApplicationContext applicationContext;

		@Bean
		@ConditionalOnMissingBean
		public ConnectionFactoryContextWrapper contextWrapper(
				@Qualifier(RabbitListenerConfigUtils.RABBIT_CONNECTION_FACTORY_BEAN_NAME) final ConnectionFactory connectionFactory) {
			return new ConnectionFactoryContextWrapper(connectionFactory);
		}

		@Primary
		@Bean(RabbitListenerConfigUtils.RABBIT_CONNECTION_FACTORY_BEAN_NAME)
		public ConnectionFactory routingConnectionFactory(final RabbitProperties rabbitProperties,
				final MultiRabbitProperties multiRabbitProperties, final ResourceLoader resourceLoader,
				final ObjectProvider<CredentialsProvider> credentialsProvider,
				final ObjectProvider<CredentialsRefreshService> credentialsRefreshService,
				final ObjectProvider<ConnectionNameStrategy> connectionNameStrategy) throws Exception {
			final MultiRabbitConnectionFactoryWrapper wrapper = new MultiRabbitConnectionFactoryWrapper();
			final ConnectionFactory defaultConnectionFactory = super.rabbitConnectionFactory(rabbitProperties,
					resourceLoader, credentialsProvider, credentialsRefreshService, connectionNameStrategy);
			wrapper.setDefaultConnectionFactory(defaultConnectionFactory);
			this.registerNewContainerFactoryBean(RabbitListenerConfigUtils.MULTI_RABBIT_CONTAINER_FACTORY_BEAN_NAME,
					defaultConnectionFactory);
			this.registerNewRabbitAdminBean(RabbitListenerConfigUtils.RABBIT_ADMIN_BEAN_NAME, defaultConnectionFactory);

			for (final Map.Entry<String, RabbitProperties> entry : multiRabbitProperties.getConnections().entrySet()) {
				final String key = entry.getKey();
				final RabbitProperties properties = entry.getValue();
				final ConnectionFactory connectionFactory = super.rabbitConnectionFactory(properties, resourceLoader,
						credentialsProvider, credentialsRefreshService, connectionNameStrategy);
				this.registerNewContainerFactoryBean(key, connectionFactory);
				this.registerNewRabbitAdminBean(key.concat(RabbitListenerConfigUtils.MULTI_RABBIT_ADMIN_SUFFIX),
						connectionFactory);
				wrapper.addConnectionFactory(key, connectionFactory);
			}

			final SimpleRoutingConnectionFactory connectionFactory = new SimpleRoutingConnectionFactory();
			connectionFactory.setTargetConnectionFactories(wrapper.getConnectionFactories());
			connectionFactory.setDefaultTargetConnectionFactory(wrapper.getDefaultConnectionFactory());
			return connectionFactory;
		}

		private void registerNewContainerFactoryBean(final String name, final ConnectionFactory connectionFactory) {
			final SimpleRabbitListenerContainerFactory containerFactory = new SimpleRabbitListenerContainerFactory();
			containerFactory.setApplicationContext(this.applicationContext);
			containerFactory.setConnectionFactory(connectionFactory);
			this.beanFactory.registerSingleton(name, containerFactory);
		}

		private void registerNewRabbitAdminBean(final String name, final ConnectionFactory connectionFactory) {
			final RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
			rabbitAdmin.setApplicationContext(this.applicationContext);
			rabbitAdmin.setBeanName(name);
			rabbitAdmin.afterPropertiesSet();
			this.beanFactory.registerSingleton(name, rabbitAdmin);
		}

		@Override
		public void setBeanFactory(final BeanFactory beanFactory) {
			this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
		}

		@Override
		public void setApplicationContext(final ApplicationContext applicationContext) {
			this.applicationContext = applicationContext;
		}

	}

}
