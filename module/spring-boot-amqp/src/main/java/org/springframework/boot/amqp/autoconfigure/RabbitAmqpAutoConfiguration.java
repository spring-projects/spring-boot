/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.amqp.autoconfigure;

import com.rabbitmq.client.amqp.Connection;
import com.rabbitmq.client.amqp.CredentialsProvider;
import com.rabbitmq.client.amqp.Environment;
import com.rabbitmq.client.amqp.impl.AmqpEnvironmentBuilder;
import com.rabbitmq.client.amqp.impl.AmqpEnvironmentBuilder.EnvironmentConnectionSettings;

import org.springframework.amqp.rabbit.config.ContainerCustomizer;
import org.springframework.amqp.rabbitmq.client.AmqpConnectionFactory;
import org.springframework.amqp.rabbitmq.client.RabbitAmqpAdmin;
import org.springframework.amqp.rabbitmq.client.RabbitAmqpTemplate;
import org.springframework.amqp.rabbitmq.client.SingleAmqpConnectionFactory;
import org.springframework.amqp.rabbitmq.client.config.RabbitAmqpListenerContainerFactory;
import org.springframework.amqp.rabbitmq.client.listener.RabbitAmqpListenerContainer;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.amqp.autoconfigure.RabbitConnectionDetails.Address;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link RabbitAmqpTemplate}.
 *
 * @author Eddú Meléndez
 * @since 4.1.0
 */
@AutoConfiguration
@ConditionalOnClass({ RabbitAmqpTemplate.class, Connection.class })
@EnableConfigurationProperties(RabbitProperties.class)
public final class RabbitAmqpAutoConfiguration {

	private final RabbitProperties properties;

	RabbitAmqpAutoConfiguration(RabbitProperties properties) {
		this.properties = properties;
	}

	@Bean
	@ConditionalOnMissingBean
	RabbitConnectionDetails rabbitConnectionDetails(ObjectProvider<SslBundles> sslBundles) {
		return new PropertiesRabbitConnectionDetails(this.properties, sslBundles.getIfAvailable());
	}

	@Bean(name = "rabbitListenerContainerFactory")
	@ConditionalOnMissingBean(name = "rabbitListenerContainerFactory")
	RabbitAmqpListenerContainerFactory rabbitAmqpListenerContainerFactory(AmqpConnectionFactory connectionFactory,
			ObjectProvider<ContainerCustomizer<RabbitAmqpListenerContainer>> amqpContainerCustomizer) {
		RabbitAmqpListenerContainerFactory factory = new RabbitAmqpListenerContainerFactory(connectionFactory);
		amqpContainerCustomizer.ifUnique(factory::setContainerCustomizer);

		RabbitProperties.AmqpContainer configuration = this.properties.getListener().getSimple();
		factory.setObservationEnabled(configuration.isObservationEnabled());
		return factory;
	}

	@Bean
	@ConditionalOnMissingBean
	Environment rabbitAmqpEnvironment(RabbitConnectionDetails connectionDetails,
			ObjectProvider<AmqpEnvironmentBuilderCustomizer> customizers,
			ObjectProvider<CredentialsProvider> credentialsProvider) {
		PropertyMapper map = PropertyMapper.get();
		EnvironmentConnectionSettings environmentConnectionSettings = new AmqpEnvironmentBuilder().connectionSettings();
		Address address = connectionDetails.getFirstAddress();
		map.from(address::host).to(environmentConnectionSettings::host);
		map.from(address::port).to(environmentConnectionSettings::port);
		map.from(connectionDetails::getUsername).to(environmentConnectionSettings::username);
		map.from(connectionDetails::getPassword).to(environmentConnectionSettings::password);
		map.from(connectionDetails::getVirtualHost).to(environmentConnectionSettings::virtualHost);
		map.from(credentialsProvider::getIfAvailable).to(environmentConnectionSettings::credentialsProvider);

		AmqpEnvironmentBuilder builder = environmentConnectionSettings.environmentBuilder();
		customizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
		return builder.build();
	}

	@Bean
	@ConditionalOnMissingBean
	AmqpConnectionFactory amqpConnectionFactory(Environment environment) {
		return new SingleAmqpConnectionFactory(environment);
	}

	@Bean
	@ConditionalOnMissingBean
	RabbitAmqpTemplate rabbitAmqpTemplate(AmqpConnectionFactory connectionFactory,
			ObjectProvider<RabbitAmqpTemplateCustomizer> customizers,
			ObjectProvider<MessageConverter> messageConverter) {
		RabbitAmqpTemplate rabbitAmqpTemplate = new RabbitAmqpTemplate(connectionFactory);
		if (messageConverter.getIfAvailable() != null) {
			rabbitAmqpTemplate.setMessageConverter(messageConverter.getIfAvailable());
		}
		RabbitProperties.Template templateProperties = this.properties.getTemplate();

		PropertyMapper map = PropertyMapper.get();
		map.from(templateProperties::getDefaultReceiveQueue).to(rabbitAmqpTemplate::setReceiveQueue);
		map.from(templateProperties::getExchange).to(rabbitAmqpTemplate::setExchange);
		map.from(templateProperties::getRoutingKey).to(rabbitAmqpTemplate::setRoutingKey);

		customizers.orderedStream().forEach((customizer) -> customizer.customize(rabbitAmqpTemplate));
		return rabbitAmqpTemplate;
	}

	@Bean
	@ConditionalOnMissingBean
	RabbitAmqpAdmin rabbitAmqpAdmin(AmqpConnectionFactory connectionFactory) {
		return new RabbitAmqpAdmin(connectionFactory);
	}

}
