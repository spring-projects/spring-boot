/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.reactor.netty.autoconfigure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ReactorResourceFactory;

/**
 * Configurations for Reactor Netty. Those should be {@code @Import} in a regular
 * auto-configuration class.
 *
 * @author Moritz Halbritter
 * @since 4.0.0
 */
public final class ReactorNettyConfigurations {

	private ReactorNettyConfigurations() {
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(ReactorNettyProperties.class)
	public static class ReactorResourceFactoryConfiguration {

		@Bean
		@ConditionalOnMissingBean
		ReactorResourceFactory reactorResourceFactory(ReactorNettyProperties configurationProperties) {
			ReactorResourceFactory reactorResourceFactory = new ReactorResourceFactory();
			if (configurationProperties.getShutdownQuietPeriod() != null) {
				reactorResourceFactory.setShutdownQuietPeriod(configurationProperties.getShutdownQuietPeriod());
			}
			return reactorResourceFactory;
		}

	}

}
