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

import java.time.Duration;

import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.boot.context.properties.PropertyMapper;

/**
 * Configures Rabbit {@link CachingConnectionFactory} with sensible defaults.
 *
 * @author Chris Bono
 * @since 2.6.0
 */
public class CachingConnectionFactoryConfigurer extends AbstractConnectionFactoryConfigurer<CachingConnectionFactory> {

	public CachingConnectionFactoryConfigurer(RabbitProperties properties) {
		super(properties);
	}

	@Override
	public void configure(CachingConnectionFactory connectionFactory, RabbitProperties rabbitProperties) {
		PropertyMapper map = PropertyMapper.get();
		map.from(rabbitProperties::isPublisherReturns).to(connectionFactory::setPublisherReturns);
		map.from(rabbitProperties::getPublisherConfirmType).whenNonNull()
				.to(connectionFactory::setPublisherConfirmType);
		RabbitProperties.Cache.Channel channel = rabbitProperties.getCache().getChannel();
		map.from(channel::getSize).whenNonNull().to(connectionFactory::setChannelCacheSize);
		map.from(channel::getCheckoutTimeout).whenNonNull().as(Duration::toMillis)
				.to(connectionFactory::setChannelCheckoutTimeout);
		RabbitProperties.Cache.Connection connection = rabbitProperties.getCache().getConnection();
		map.from(connection::getMode).whenNonNull().to(connectionFactory::setCacheMode);
		map.from(connection::getSize).whenNonNull().to(connectionFactory::setConnectionCacheSize);
	}

}
