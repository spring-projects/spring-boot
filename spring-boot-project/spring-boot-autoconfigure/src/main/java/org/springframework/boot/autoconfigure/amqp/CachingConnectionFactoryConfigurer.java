package org.springframework.boot.autoconfigure.amqp;

import java.time.Duration;

import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.boot.context.properties.PropertyMapper;

/**
 * Configures {@link CachingConnectionFactory Rabbit CachingConnectionFactory} with
 * sensible defaults.
 *
 * @author Chris Bono
 * @since 2.6.0
 */
public class CachingConnectionFactoryConfigurer extends AbstractConnectionFactoryConfigurer<CachingConnectionFactory> {

	/**
	 * Configure the specified Rabbit caching connection factory with implementation
	 * specific settings.
	 * @param connectionFactory the {@link CachingConnectionFactory} instance to configure
	 */
	@Override
	public void configureSpecific(CachingConnectionFactory connectionFactory) {
		PropertyMapper map = PropertyMapper.get();
		RabbitProperties rabbitProperties = getRabbitProperties();
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
