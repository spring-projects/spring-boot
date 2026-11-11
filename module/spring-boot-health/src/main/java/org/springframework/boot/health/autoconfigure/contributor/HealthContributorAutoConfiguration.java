/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.health.autoconfigure.contributor;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.health.contributor.HealthContributor;
import org.springframework.boot.health.contributor.PingHealthIndicator;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for default {@link HealthContributor
 * health contributors}.
 *
 * @author Phillip Webb
 * @since 4.0.0
 */
@AutoConfiguration
public final class HealthContributorAutoConfiguration {

	@Bean
	@ConditionalOnEnabledHealthIndicator("ping")
	PingHealthIndicator pingHealthContributor() {
		return new PingHealthIndicator();
	}

}
