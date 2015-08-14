/*
 * Copyright 2012-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.amqp;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.RabbitConnectionFactoryBean;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitMessagingTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.rabbitmq.client.Channel;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link RabbitTemplate}.
 * <p>
 * This configuration class is active only when the RabbitMQ and Spring AMQP client
 * libraries are on the classpath.
 * <P>
 * Registers the following beans:
 * <ul>
 * <li>
 * {@link org.springframework.amqp.rabbit.core.RabbitTemplate RabbitTemplate} if there is
 * no other bean of the same type in the context.</li>
 * <li>
 * {@link org.springframework.amqp.rabbit.connection.CachingConnectionFactory
 * CachingConnectionFactory} instance if there is no other bean of the same type in the
 * context.</li>
 * <li>
 * {@link org.springframework.amqp.core.AmqpAdmin } instance as long as
 * {@literal spring.rabbitmq.dynamic=true}.</li>
 * </ul>
 * <p>
 * The {@link org.springframework.amqp.rabbit.connection.CachingConnectionFactory} honors
 * the following properties:
 * <ul>
 * <li>
 * {@literal spring.rabbitmq.port} is used to specify the port to which the client should
 * connect, and defaults to 5672.</li>
 * <li>
 * {@literal spring.rabbitmq.username} is used to specify the (optional) username.</li>
 * <li>
 * {@literal spring.rabbitmq.password} is used to specify the (optional) password.</li>
 * <li>
 * {@literal spring.rabbitmq.host} is used to specify the host, and defaults to
 * {@literal localhost}.</li>
 * <li>{@literal spring.rabbitmq.virtualHost} is used to specify the (optional) virtual
 * host to which the client should connect.</li>
 * </ul>
 * @author Greg Turnquist
 * @author Josh Long
 * @author Stephane Nicoll
 * @author Eddú Meléndez
 */
@Configuration
@ConditionalOnClass({ RabbitTemplate.class, Channel.class })
@EnableConfigurationProperties(RabbitProperties.class)
@Import(RabbitAnnotationDrivenConfiguration.class)
public class RabbitAutoConfiguration {

	@Bean
	@ConditionalOnProperty(prefix = "spring.rabbitmq", name = "dynamic", matchIfMissing = true)
	@ConditionalOnMissingBean(AmqpAdmin.class)
	public AmqpAdmin amqpAdmin(ConnectionFactory connectionFactory) {
		return new RabbitAdmin(connectionFactory);
	}

	@Autowired
	private ConnectionFactory connectionFactory;

	@Bean
	@ConditionalOnMissingBean(RabbitTemplate.class)
	public RabbitTemplate rabbitTemplate() {
		return new RabbitTemplate(this.connectionFactory);
	}

	@Configuration
	@ConditionalOnMissingBean(ConnectionFactory.class)
	protected static class RabbitConnectionFactoryCreator {

		@Autowired
		private RabbitProperties properties;

		@Bean
		public CachingConnectionFactory rabbitConnectionFactory()
				throws Exception {
			RabbitConnectionFactoryBean factory = new RabbitConnectionFactoryBean();
			applyProperties(factory);
			applySslProperties(factory);
			factory.afterPropertiesSet();
			CachingConnectionFactory connectionFactory = new CachingConnectionFactory(
					factory.getObject());
			connectionFactory.setAddresses(this.properties.getAddresses());
			return connectionFactory;
		}

		private void applyProperties(RabbitConnectionFactoryBean factory) {
			if (this.properties.getHost() != null) {
				factory.setHost(this.properties.getHost());
				factory.setPort(this.properties.getPort());
			}
			if (this.properties.getUsername() != null) {
				factory.setUsername(this.properties.getUsername());
			}
			if (this.properties.getPassword() != null) {
				factory.setPassword(this.properties.getPassword());
			}
			if (this.properties.getVirtualHost() != null) {
				factory.setVirtualHost(this.properties.getVirtualHost());
			}
			if (this.properties.getRequestedHeartbeat() != null) {
				factory.setRequestedHeartbeat(this.properties.getRequestedHeartbeat());
			}
		}

		private void applySslProperties(RabbitConnectionFactoryBean factory) {
			RabbitProperties.Ssl ssl = this.properties.getSsl();
			if (ssl.isEnabled()) {
				factory.setUseSSL(true);
				if (ssl.getPropertiesLocation() != null) {
					factory.setSslPropertiesLocation(ssl.getPropertiesLocation());
				} else {
					factory.setKeyStore(ssl.getKeyStore());
					factory.setTrustStore(ssl.getTrustStore());
					factory.setKeyStorePassphrase(ssl.getKeyStorePassword());
					factory.setTrustStorePassphrase(ssl.getTrustStorePassword());
				}
			}
		}

	}

	@ConditionalOnClass(RabbitMessagingTemplate.class)
	@ConditionalOnMissingBean(RabbitMessagingTemplate.class)
	protected static class MessagingTemplateConfiguration {

		@Bean
		public RabbitMessagingTemplate rabbitMessagingTemplate(
				RabbitTemplate rabbitTemplate) {
			return new RabbitMessagingTemplate(rabbitTemplate);
		}

	}

}
