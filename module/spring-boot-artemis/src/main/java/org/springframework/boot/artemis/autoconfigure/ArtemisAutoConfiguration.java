/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.artemis.autoconfigure;

import jakarta.jms.ConnectionFactory;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jms.autoconfigure.JmsAutoConfiguration;
import org.springframework.boot.jms.autoconfigure.JmsProperties;
import org.springframework.boot.jms.autoconfigure.JndiConnectionFactoryAutoConfiguration;
import org.springframework.boot.transaction.jta.autoconfigure.JtaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * {@link EnableAutoConfiguration Auto-configuration} to integrate with an Artemis broker.
 * If the necessary classes are present, embed the broker in the application by default.
 * Otherwise, connect to a broker available on the local machine with the default
 * settings.
 *
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 * @since 4.0.0
 * @see ArtemisProperties
 */
@AutoConfiguration(before = JmsAutoConfiguration.class,
		after = { JndiConnectionFactoryAutoConfiguration.class, JtaAutoConfiguration.class })
@ConditionalOnClass({ ConnectionFactory.class, ActiveMQConnectionFactory.class })
@ConditionalOnMissingBean(ConnectionFactory.class)
@EnableConfigurationProperties({ ArtemisProperties.class, JmsProperties.class })
@Import({ ArtemisEmbeddedServerConfiguration.class, ArtemisXAConnectionFactoryConfiguration.class,
		ArtemisConnectionFactoryConfiguration.class })
public final class ArtemisAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	ArtemisConnectionDetails artemisConnectionDetails(ArtemisProperties properties) {
		return new PropertiesArtemisConnectionDetails(properties);
	}

	/**
	 * Adapts {@link ArtemisProperties} to {@link ArtemisConnectionDetails}.
	 */
	static class PropertiesArtemisConnectionDetails implements ArtemisConnectionDetails {

		private final ArtemisProperties properties;

		PropertiesArtemisConnectionDetails(ArtemisProperties properties) {
			this.properties = properties;
		}

		@Override
		public @Nullable ArtemisMode getMode() {
			return this.properties.getMode();
		}

		@Override
		public @Nullable String getBrokerUrl() {
			return this.properties.getBrokerUrl();
		}

		@Override
		public @Nullable String getUser() {
			return this.properties.getUser();
		}

		@Override
		public @Nullable String getPassword() {
			return this.properties.getPassword();
		}

	}

}
