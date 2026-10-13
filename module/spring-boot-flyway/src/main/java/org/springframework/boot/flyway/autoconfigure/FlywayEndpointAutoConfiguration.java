/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.flyway.autoconfigure;

import org.flywaydb.core.Flyway;

import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.flyway.actuate.endpoint.FlywayEndpoint;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link FlywayEndpoint}.
 *
 * @author Phillip Webb
 * @since 4.0.0
 */
@AutoConfiguration(after = FlywayAutoConfiguration.class)
@ConditionalOnClass({ Flyway.class, ConditionalOnAvailableEndpoint.class })
@ConditionalOnAvailableEndpoint(FlywayEndpoint.class)
public final class FlywayEndpointAutoConfiguration {

	@Bean
	@ConditionalOnBean(Flyway.class)
	@ConditionalOnMissingBean
	FlywayEndpoint flywayEndpoint(ApplicationContext context) {
		return new FlywayEndpoint(context);
	}

}
