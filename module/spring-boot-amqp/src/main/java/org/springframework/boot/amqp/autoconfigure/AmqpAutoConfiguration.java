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

import java.util.stream.Stream;

import org.apache.qpid.protonj2.client.Client;
import org.apache.qpid.protonj2.client.ClientOptions;
import org.apache.qpid.protonj2.client.ConnectionOptions;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.amqp.client.AmqpClient;
import org.springframework.amqp.client.AmqpConnectionFactory;
import org.springframework.amqp.client.SingleAmqpConnectionFactory;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.amqp.autoconfigure.AmqpConnectionDetails.Address;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuration for generic AMQP 1.0 support.
 *
 * @author Stephane Nicoll
 * @since 4.1.0
 */
@AutoConfiguration(afterName = "org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration")
@ConditionalOnClass({ Client.class, AmqpConnectionFactory.class })
@EnableConfigurationProperties(AmqpProperties.class)
public final class AmqpAutoConfiguration {

	@Configuration(proxyBeanMethods = false)
	static class AmqpConnectionFactoryConfiguration {

		@Bean
		@ConditionalOnMissingBean
		Client protonClient() {
			return Client.create(new ClientOptions());
		}

		@Bean
		@ConditionalOnMissingBean
		AmqpConnectionDetails amqpConnectionDetails(AmqpProperties properties) {
			return new PropertiesAmqpConnectionDetails(properties);
		}

		@Bean
		@ConditionalOnMissingBean
		AmqpConnectionFactory amqpConnectionFactory(AmqpConnectionDetails connectionDetails, Client protonClient,
				ObjectProvider<ConnectionOptionsCustomizer> connectionOptionsCustomizers) {
			ConnectionOptions connectionOptions = createConnectionOptions(connectionDetails,
					connectionOptionsCustomizers.orderedStream());
			return createAmqpConnectionFactory(connectionDetails, protonClient).setConnectionOptions(connectionOptions);
		}

		private SingleAmqpConnectionFactory createAmqpConnectionFactory(AmqpConnectionDetails connectionDetails,
				Client protonClient) {
			SingleAmqpConnectionFactory connectionFactory = new SingleAmqpConnectionFactory(protonClient);
			PropertyMapper map = PropertyMapper.get();
			Address address = connectionDetails.getAddress();
			map.from(address::host).to(connectionFactory::setHost);
			map.from(address::port).to(connectionFactory::setPort);
			return connectionFactory;
		}

		private ConnectionOptions createConnectionOptions(AmqpConnectionDetails connectionDetails,
				Stream<ConnectionOptionsCustomizer> customizers) {
			ConnectionOptions connectionOptions = new ConnectionOptions();
			PropertyMapper map = PropertyMapper.get();
			map.from(connectionDetails::getUsername).to(connectionOptions::user);
			map.from(connectionDetails::getPassword).to(connectionOptions::password);
			customizers.forEach((customizer) -> customizer.customize(connectionOptions));
			return connectionOptions;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnBean(JsonMapper.class)
	@ConditionalOnSingleCandidate(JsonMapper.class)
	static class JacksonMessageConverterConfiguration {

		@Bean
		@ConditionalOnMissingBean(MessageConverter.class)
		JacksonJsonMessageConverter jacksonJsonMessageConverter(JsonMapper jsonMapper) {
			return new JacksonJsonMessageConverter(jsonMapper);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class AmqpClientConfiguration {

		@Bean
		@ConditionalOnMissingBean
		AmqpClient amqpClient(AmqpConnectionFactory amqpConnectionFactory, AmqpProperties properties,
				ObjectProvider<MessageConverter> messageConverter, ObjectProvider<AmqpClientCustomizer> customizers) {
			AmqpClient.Builder builder = AmqpClient.builder(amqpConnectionFactory);
			AmqpProperties.Client client = properties.getClient();
			PropertyMapper map = PropertyMapper.get();
			map.from(client.getDefaultToAddress()).to(builder::defaultToAddress);
			map.from(client.getCompletionTimeout()).to(builder::completionTimeout);
			messageConverter.ifAvailable(builder::messageConverter);
			customizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
			return builder.build();
		}

	}

}
