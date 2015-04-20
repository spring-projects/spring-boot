/*
 * Copyright 2012-2014 the original author or authors.
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
 * <li>
 * {@literal spring.rabbitmq.requested-heartbeat} is used to specify the client
 * requested heartbeat timeout, in seconds. Defaults to 0, heartbeat disabled.</li>
 * </ul>
 * @author Greg Turnquist
 * @author Josh Long
 */
@Configuration
@ConditionalOnClass({ RabbitTemplate.class, Channel.class })
@EnableConfigurationProperties(RabbitProperties.class)
@Import(RabbitAnnotationDrivenConfiguration.class)
public class RabbitAutoConfiguration {

	@Bean
	@ConditionalOnProperty(prefix = "spring.rabbitmq", name = "dynamic", matchIfMissing = true)
	@ConditionalOnMissingBean(AmqpAdmin.class)
	public AmqpAdmin amqpAdmin(CachingConnectionFactory connectionFactory) {
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
		private RabbitProperties config;
		
		@Bean
		public ConnectionFactory rabbitConnectionFactory(RabbitConnectionFactoryBean factoryBean) {			
			com.rabbitmq.client.ConnectionFactory connectionFactory = null;
			try {
				connectionFactory = factoryBean.getObject();
			} catch (Exception e) {
				throw new IllegalStateException("Unable to create ConnectionFactory",e);
			}			
			CachingConnectionFactory factory = new CachingConnectionFactory(connectionFactory);			
			String addresses = config.getAddresses();
			factory.setAddresses(addresses);			
			return factory;
		}
		
		@Bean 
		public RabbitConnectionFactoryBean rabbitConnectionFactoryBean(){
			RabbitConnectionFactoryBean factoryBean = new RabbitConnectionFactoryBean();			
			if (config.getHost() != null) {
				factoryBean.setHost(config.getHost());
				factoryBean.setPort(config.getPort());
			}
			if (config.getUsername() != null) {
				factoryBean.setUsername(config.getUsername());
			}
			if (config.getPassword() != null) {
				factoryBean.setPassword(config.getPassword());
			}
			if (config.getVirtualHost() != null) {
				factoryBean.setVirtualHost(config.getVirtualHost());
			}
			factoryBean.setRequestedHeartbeat(config.getRequestedHeartbeat());
			return factoryBean;
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
