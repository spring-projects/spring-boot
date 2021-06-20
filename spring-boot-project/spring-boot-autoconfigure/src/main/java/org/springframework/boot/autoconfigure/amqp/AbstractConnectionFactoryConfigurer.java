package org.springframework.boot.autoconfigure.amqp;

import org.springframework.amqp.rabbit.connection.AbstractConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionNameStrategy;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.util.Assert;

/**
 * Configures {@link AbstractConnectionFactory Rabbit ConnectionFactory} with sensible
 * defaults.
 *
 * @param <T> the connection factory type.
 * @author Chris Bono
 * @since 2.6.0
 */
public abstract class AbstractConnectionFactoryConfigurer<T extends AbstractConnectionFactory> {

	private RabbitProperties rabbitProperties;

	private ConnectionNameStrategy connectionNameStrategy;

	public RabbitProperties getRabbitProperties() {
		return rabbitProperties;
	}

	public void setRabbitProperties(RabbitProperties rabbitProperties) {
		this.rabbitProperties = rabbitProperties;
	}

	public ConnectionNameStrategy getConnectionNameStrategy() {
		return connectionNameStrategy;
	}

	public void setConnectionNameStrategy(ConnectionNameStrategy connectionNameStrategy) {
		this.connectionNameStrategy = connectionNameStrategy;
	}

	/**
	 * Configure the specified Rabbit connection factory - delegating to
	 * {@link #configureSpecific} for the connection factory implementation specific
	 * settings. The factory can be further tuned and default settings can be overridden.
	 * @param connectionFactory the connection factory instance to configure
	 */
	public void configure(T connectionFactory) {
		Assert.notNull(connectionFactory, "ConnectionFactory must not be null");
		PropertyMapper map = PropertyMapper.get();
		map.from(this.rabbitProperties::determineAddresses).to(connectionFactory::setAddresses);
		map.from(this.rabbitProperties::getAddressShuffleMode).whenNonNull()
				.to(connectionFactory::setAddressShuffleMode);
		map.from(connectionNameStrategy).whenNonNull().to(connectionFactory::setConnectionNameStrategy);
		configureSpecific(connectionFactory);
	}

	/**
	 * Configure the specified Rabbit connection factory with implementation specific
	 * settings.
	 * @param connectionFactory the connection factory instance to configure
	 */
	protected abstract void configureSpecific(T connectionFactory);

}
