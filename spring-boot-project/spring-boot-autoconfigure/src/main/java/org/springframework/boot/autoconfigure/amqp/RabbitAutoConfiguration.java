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
import org.springframework.boot.ssl.SslBundles;
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
 * @author Scott Frederick
 * @since 1.0.0
 */
@AutoConfiguration
@ConditionalOnClass({ RabbitTemplate.class, Channel.class })
@EnableConfigurationProperties(RabbitProperties.class)
@Import({ RabbitAnnotationDrivenConfiguration.class, RabbitStreamConfiguration.class })
public class RabbitAutoConfiguration {

	/**
     * RabbitConnectionFactoryCreator class.
     */
    @Configuration(proxyBeanMethods = false)
	protected static class RabbitConnectionFactoryCreator {

		private final RabbitProperties properties;

		/**
         * Constructs a new RabbitConnectionFactoryCreator with the specified RabbitProperties.
         *
         * @param properties the RabbitProperties to be used by the RabbitConnectionFactoryCreator
         */
        protected RabbitConnectionFactoryCreator(RabbitProperties properties) {
			this.properties = properties;
		}

		/**
         * Creates a RabbitConnectionDetails bean if no other bean of type RabbitConnectionDetails is present.
         * 
         * @return the RabbitConnectionDetails bean
         */
        @Bean
		@ConditionalOnMissingBean(RabbitConnectionDetails.class)
		RabbitConnectionDetails rabbitConnectionDetails() {
			return new PropertiesRabbitConnectionDetails(this.properties);
		}

		/**
         * Creates a RabbitConnectionFactoryBeanConfigurer object with the given parameters.
         * This method is annotated with @Bean and @ConditionalOnMissingBean, indicating that it will be used to create a bean if no other bean of the same type is present.
         * 
         * @param resourceLoader The resource loader used to load resources.
         * @param connectionDetails The RabbitConnectionDetails object containing the connection details.
         * @param credentialsProvider The CredentialsProvider object providing the credentials for the connection.
         * @param credentialsRefreshService The CredentialsRefreshService object used to refresh the credentials.
         * @param sslBundles The SslBundles object containing the SSL bundles for the connection.
         * @return The RabbitConnectionFactoryBeanConfigurer object created with the given parameters.
         */
        @Bean
		@ConditionalOnMissingBean
		RabbitConnectionFactoryBeanConfigurer rabbitConnectionFactoryBeanConfigurer(ResourceLoader resourceLoader,
				RabbitConnectionDetails connectionDetails, ObjectProvider<CredentialsProvider> credentialsProvider,
				ObjectProvider<CredentialsRefreshService> credentialsRefreshService,
				ObjectProvider<SslBundles> sslBundles) {
			RabbitConnectionFactoryBeanConfigurer configurer = new RabbitConnectionFactoryBeanConfigurer(resourceLoader,
					this.properties, connectionDetails, sslBundles.getIfAvailable());
			configurer.setCredentialsProvider(credentialsProvider.getIfUnique());
			configurer.setCredentialsRefreshService(credentialsRefreshService.getIfUnique());
			return configurer;
		}

		/**
         * Configures the CachingConnectionFactory with the provided RabbitConnectionDetails and ConnectionNameStrategy.
         * If a ConnectionNameStrategy is not provided, the default one will be used.
         * 
         * @param connectionDetails the RabbitConnectionDetails containing the connection details
         * @param connectionNameStrategy the ConnectionNameStrategy to be used for generating connection names
         * @return the CachingConnectionFactoryConfigurer configured with the provided details
         */
        @Bean
		@ConditionalOnMissingBean
		CachingConnectionFactoryConfigurer rabbitConnectionFactoryConfigurer(RabbitConnectionDetails connectionDetails,
				ObjectProvider<ConnectionNameStrategy> connectionNameStrategy) {
			CachingConnectionFactoryConfigurer configurer = new CachingConnectionFactoryConfigurer(this.properties,
					connectionDetails);
			configurer.setConnectionNameStrategy(connectionNameStrategy.getIfUnique());
			return configurer;
		}

		/**
         * Creates a CachingConnectionFactory for RabbitMQ if no ConnectionFactory bean is present.
         * 
         * @param rabbitConnectionFactoryBeanConfigurer The RabbitConnectionFactoryBeanConfigurer used to configure the RabbitConnectionFactoryBean.
         * @param rabbitCachingConnectionFactoryConfigurer The CachingConnectionFactoryConfigurer used to configure the CachingConnectionFactory.
         * @param connectionFactoryCustomizers The ObjectProvider of ConnectionFactoryCustomizer used to customize the ConnectionFactory.
         * @return The created CachingConnectionFactory.
         * @throws Exception if an error occurs during the creation of the CachingConnectionFactory.
         */
        @Bean
		@ConditionalOnMissingBean(ConnectionFactory.class)
		CachingConnectionFactory rabbitConnectionFactory(
				RabbitConnectionFactoryBeanConfigurer rabbitConnectionFactoryBeanConfigurer,
				CachingConnectionFactoryConfigurer rabbitCachingConnectionFactoryConfigurer,
				ObjectProvider<ConnectionFactoryCustomizer> connectionFactoryCustomizers) throws Exception {

			RabbitConnectionFactoryBean connectionFactoryBean = new SslBundleRabbitConnectionFactoryBean();
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

	/**
     * RabbitTemplateConfiguration class.
     */
    @Configuration(proxyBeanMethods = false)
	@Import(RabbitConnectionFactoryCreator.class)
	protected static class RabbitTemplateConfiguration {

		/**
         * Configures the RabbitTemplate with the provided RabbitProperties, MessageConverter, and RabbitRetryTemplateCustomizer.
         * If no MessageConverter is provided, the default one will be used.
         * If no RabbitRetryTemplateCustomizer is provided, no customizations will be applied to the RetryTemplate.
         * 
         * @param properties The RabbitProperties to configure the RabbitTemplate.
         * @param messageConverter The MessageConverter to be used by the RabbitTemplate. If not provided, the default one will be used.
         * @param retryTemplateCustomizers The RabbitRetryTemplateCustomizer(s) to customize the RetryTemplate used by the RabbitTemplate. If not provided, no customizations will be applied.
         * @return The configured RabbitTemplateConfigurer.
         */
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

		/**
         * Creates a RabbitTemplate bean if there is a single candidate ConnectionFactory bean and no RabbitOperations bean is present.
         * 
         * @param configurer The RabbitTemplateConfigurer used to configure the RabbitTemplate.
         * @param connectionFactory The ConnectionFactory used by the RabbitTemplate.
         * @param customizers The ObjectProvider of RabbitTemplateCustomizer used to customize the RabbitTemplate.
         * @return The RabbitTemplate bean.
         */
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

		/**
         * Creates an instance of AmqpAdmin if there is a single candidate for ConnectionFactory and the property "spring.rabbitmq.dynamic" is either not present or set to true.
         * If an instance of AmqpAdmin is already present, this method will not be executed.
         * 
         * @param connectionFactory the ConnectionFactory to be used for creating the AmqpAdmin instance
         * @return an instance of AmqpAdmin created using the provided ConnectionFactory
         */
        @Bean
		@ConditionalOnSingleCandidate(ConnectionFactory.class)
		@ConditionalOnProperty(prefix = "spring.rabbitmq", name = "dynamic", matchIfMissing = true)
		@ConditionalOnMissingBean
		public AmqpAdmin amqpAdmin(ConnectionFactory connectionFactory) {
			return new RabbitAdmin(connectionFactory);
		}

	}

	/**
     * MessagingTemplateConfiguration class.
     */
    @Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(RabbitMessagingTemplate.class)
	@ConditionalOnMissingBean(RabbitMessagingTemplate.class)
	@Import(RabbitTemplateConfiguration.class)
	protected static class MessagingTemplateConfiguration {

		/**
         * Creates a RabbitMessagingTemplate bean if a single candidate RabbitTemplate bean is available.
         * 
         * @param rabbitTemplate the RabbitTemplate bean to be used by the RabbitMessagingTemplate
         * @return the created RabbitMessagingTemplate bean
         */
        @Bean
		@ConditionalOnSingleCandidate(RabbitTemplate.class)
		public RabbitMessagingTemplate rabbitMessagingTemplate(RabbitTemplate rabbitTemplate) {
			return new RabbitMessagingTemplate(rabbitTemplate);
		}

	}

}
