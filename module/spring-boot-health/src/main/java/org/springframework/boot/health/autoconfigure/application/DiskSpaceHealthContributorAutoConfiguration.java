/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.health.autoconfigure.application;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.health.application.DiskSpaceHealthIndicator;
import org.springframework.boot.health.autoconfigure.contributor.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.health.autoconfigure.contributor.HealthContributorAutoConfiguration;
import org.springframework.boot.health.contributor.Health;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for
 * {@link DiskSpaceHealthIndicator}.
 *
 * @author Mattias Severson
 * @author Andy Wilkinson
 * @since 4.0.0
 */
@AutoConfiguration(before = HealthContributorAutoConfiguration.class)
@ConditionalOnClass(Health.class)
@ConditionalOnEnabledHealthIndicator("diskspace")
@EnableConfigurationProperties(DiskSpaceHealthIndicatorProperties.class)
public final class DiskSpaceHealthContributorAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(name = "diskSpaceHealthIndicator")
	DiskSpaceHealthIndicator diskSpaceHealthIndicator(DiskSpaceHealthIndicatorProperties properties) {
		return new DiskSpaceHealthIndicator(properties.getPath(), properties.getThreshold());
	}

}
