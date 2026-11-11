/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.activemq.autoconfigure;

import jakarta.jms.ConnectionFactory;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.activemq.autoconfigure.ActiveMQProperties.Ssl;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jms.autoconfigure.JmsAutoConfiguration;
import org.springframework.boot.jms.autoconfigure.JmsProperties;
import org.springframework.boot.jms.autoconfigure.JndiConnectionFactoryAutoConfiguration;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.transaction.jta.autoconfigure.JtaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} to integrate with an ActiveMQ
 * broker.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Eddú Meléndez
 * @author Armen Petrosyan
 * @since 4.0.0
 */
@AutoConfiguration(before = JmsAutoConfiguration.class,
		after = { JndiConnectionFactoryAutoConfiguration.class, JtaAutoConfiguration.class })
@ConditionalOnClass({ ConnectionFactory.class, ActiveMQConnectionFactory.class })
@ConditionalOnMissingBean(ConnectionFactory.class)
@EnableConfigurationProperties({ ActiveMQProperties.class, JmsProperties.class })
@Import({ ActiveMQXAConnectionFactoryConfiguration.class, ActiveMQConnectionFactoryConfiguration.class })
public final class ActiveMQAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	ActiveMQConnectionDetails activemqConnectionDetails(ActiveMQProperties properties,
			ObjectProvider<SslBundles> sslBundles) {
		return new PropertiesActiveMQConnectionDetails(properties, sslBundles.getIfAvailable());
	}

	/**
	 * Adapts {@link ActiveMQProperties} to {@link ActiveMQConnectionDetails}.
	 */
	static class PropertiesActiveMQConnectionDetails implements ActiveMQConnectionDetails {

		private final ActiveMQProperties properties;

		private final @Nullable SslBundles sslBundles;

		PropertiesActiveMQConnectionDetails(ActiveMQProperties properties, @Nullable SslBundles sslBundles) {
			this.properties = properties;
			this.sslBundles = sslBundles;
		}

		@Override
		public String getBrokerUrl() {
			return this.properties.determineBrokerUrl();
		}

		@Override
		public @Nullable String getUser() {
			return this.properties.getUser();
		}

		@Override
		public @Nullable String getPassword() {
			return this.properties.getPassword();
		}

		@Override
		public @Nullable SslBundle getSslBundle() {
			Ssl ssl = this.properties.getSsl();
			if (!ssl.isEnabled()) {
				return null;
			}
			if (StringUtils.hasLength(ssl.getBundle())) {
				Assert.notNull(this.sslBundles, "SSL bundle name has been set but no SSL bundles found in context");
				return this.sslBundles.getBundle(ssl.getBundle());
			}
			return SslBundle.systemDefault();
		}

	}

}
