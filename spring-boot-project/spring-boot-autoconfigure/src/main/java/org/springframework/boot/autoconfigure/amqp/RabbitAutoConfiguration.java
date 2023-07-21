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

package org.springframework.boot.autoconfigure.amqp;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.impl.CredentialsProvider;
import com.rabbitmq.client.impl.CredentialsRefreshService;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionNameStrategy;
import org.springframework.amqp.rabbit.connection.RabbitConnectionFactoryBean;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitMessagingTemplate;
import org.springframework.amqp.rabbit.core.RabbitOperations;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ResourceLoader;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link RabbitTemplate}.
 * <p>
 * This configuration class is active only when the RabbitMQ and Spring AMQP client
 * libraries are on the classpath.
 * <p>
 * Registers the following beans:
 * <ul>
 * <li>{@link org.springframework.amqp.rabbit.core.RabbitTemplate RabbitTemplate} if there
 * is no other bean of the same type in the context.</li>
 * <li>{@link org.springframework.amqp.rabbit.connection.CachingConnectionFactory
 * CachingConnectionFactory} instance if there is no other bean of the same type in the
 * context.</li>
 * <li>{@link org.springframework.amqp.core.AmqpAdmin } instance as long as
 * {@literal spring.rabbitmq.dynamic=true}.</li>
 * </ul>
 *
 * @author Greg Turnquist
 * @author Josh Long
 * @author Stephane Nicoll
 * @author Gary Russell
 * @author Phillip Webb
 * @author Artsiom Yudovin
 * @author Chris Bono
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @since 1.0.0
 */
@AutoConfiguration
@ConditionalOnClass({ RabbitTemplate.class, Channel.class })
@EnableConfigurationProperties(RabbitProperties.class)
@Import({ RabbitAnnotationDrivenConfiguration.class, RabbitStreamConfiguration.class })
public class RabbitAutoConfiguration {

	@Configuration(proxyBeanMethods = false)
	protected static class RabbitConnectionFactoryCreator {

		private final RabbitProperties properties;

		protected RabbitConnectionFactoryCreator(RabbitProperties properties,
				ObjectProvider<RabbitConnectionDetails> connectionDetails) {
			this.properties = properties;
		}

		@Bean
		@ConditionalOnMissingBean(RabbitConnectionDetails.class)
		RabbitConnectionDetails rabbitConnectionDetails() {
			return new PropertiesRabbitConnectionDetails(this.properties);
		}

		@Bean
		@ConditionalOnMissingBean
		RabbitConnectionFactoryBeanConfigurer rabbitConnectionFactoryBeanConfigurer(ResourceLoader resourceLoader,
				RabbitConnectionDetails connectionDetails, ObjectProvider<CredentialsProvider> credentialsProvider,
				ObjectProvider<CredentialsRefreshService> credentialsRefreshService) {
			RabbitConnectionFactoryBeanConfigurer configurer = new RabbitConnectionFactoryBeanConfigurer(resourceLoader,
					this.properties, connectionDetails);
			configurer.setCredentialsProvider(credentialsProvider.getIfUnique());
			configurer.setCredentialsRefreshService(credentialsRefreshService.getIfUnique());
			return configurer;
		}

		@Bean
		@ConditionalOnMissingBean
		CachingConnectionFactoryConfigurer rabbitConnectionFactoryConfigurer(RabbitConnectionDetails connectionDetails,
				ObjectProvider<ConnectionNameStrategy> connectionNameStrategy) {
			CachingConnectionFactoryConfigurer configurer = new CachingConnectionFactoryConfigurer(this.properties,
					connectionDetails);
			configurer.setConnectionNameStrategy(connectionNameStrategy.getIfUnique());
			return configurer;
		}

		@Bean
		@ConditionalOnMissingBean(ConnectionFactory.class)
		CachingConnectionFactory rabbitConnectionFactory(
				RabbitConnectionFactoryBeanConfigurer rabbitConnectionFactoryBeanConfigurer,
				CachingConnectionFactoryConfigurer rabbitCachingConnectionFactoryConfigurer,
				ObjectProvider<ConnectionFactoryCustomizer> connectionFactoryCustomizers) throws Exception {

			RabbitConnectionFactoryBean connectionFactoryBean = new RabbitConnectionFactoryBean();
			rabbitConnectionFactoryBeanConfigurer.configure(connectionFactoryBean);
			connectionFactoryBean.afterPropertiesSet();
			com.rabbitmq.client.ConnectionFactory connectionFactory = connectionFactoryBean.getObject();
			connectionFactoryCustomizers.orderedStream()
				.forEach((customizer) -> customizer.customize(connectionFactory));

			CachingConnectionFactory factory = new CachingConnectionFactory(connectionFactory);
			rabbitCachingConnectionFactoryConfigurer.configure(factory);

			return factory;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(RabbitConnectionFactoryCreator.class)
	protected static class RabbitTemplateConfiguration {

		@Bean
		@ConditionalOnMissingBean
		public RabbitTemplateConfigurer rabbitTemplateConfigurer(RabbitProperties properties,
				ObjectProvider<MessageConverter> messageConverter,
				ObjectProvider<RabbitRetryTemplateCustomizer> retryTemplateCustomizers) {
			RabbitTemplateConfigurer configurer = new RabbitTemplateConfigurer(properties);
			configurer.setMessageConverter(messageConverter.getIfUnique());
			configurer.setRetryTemplateCustomizers(retryTemplateCustomizers.orderedStream().toList());
			return configurer;
		}

		@Bean
		@ConditionalOnSingleCandidate(ConnectionFactory.class)
		@ConditionalOnMissingBean(RabbitOperations.class)
		public RabbitTemplate rabbitTemplate(RabbitTemplateConfigurer configurer, ConnectionFactory connectionFactory,
				ObjectProvider<RabbitTemplateCustomizer> customizers) {
			RabbitTemplate template = new RabbitTemplate();
			configurer.configure(template, connectionFactory);
			customizers.orderedStream().forEach((customizer) -> customizer.customize(template));
			return template;
		}

		@Bean
		@ConditionalOnSingleCandidate(ConnectionFactory.class)
		@ConditionalOnProperty(prefix = "spring.rabbitmq", name = "dynamic", matchIfMissing = true)
		@ConditionalOnMissingBean
		public AmqpAdmin amqpAdmin(ConnectionFactory connectionFactory) {
			return new RabbitAdmin(connectionFactory);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(RabbitMessagingTemplate.class)
	@ConditionalOnMissingBean(RabbitMessagingTemplate.class)
	@Import(RabbitTemplateConfiguration.class)
	protected static class MessagingTemplateConfiguration {

		@Bean
		@ConditionalOnSingleCandidate(RabbitTemplate.class)
		public RabbitMessagingTemplate rabbitMessagingTemplate(RabbitTemplate rabbitTemplate) {
			return new RabbitMessagingTemplate(rabbitTemplate);
		}

	}

}
