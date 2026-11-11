/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.r2dbc.autoconfigure;

import io.r2dbc.proxy.ProxyConnectionFactory;
import io.r2dbc.spi.ConnectionFactory;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.r2dbc.ConnectionFactoryDecorator;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link ProxyConnectionFactory}.
 *
 * @author Tadaya Tsuyukubo
 * @author Moritz Halbritter
 * @since 4.0.0
 */
@AutoConfiguration
@ConditionalOnClass({ ConnectionFactory.class, ProxyConnectionFactory.class })
public final class R2dbcProxyAutoConfiguration {

	@Bean
	ConnectionFactoryDecorator connectionFactoryDecorator(
			ObjectProvider<ProxyConnectionFactoryCustomizer> customizers) {
		return (connectionFactory) -> {
			ProxyConnectionFactory.Builder builder = ProxyConnectionFactory.builder(connectionFactory);
			customizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
			return builder.build();
		};
	}

}
