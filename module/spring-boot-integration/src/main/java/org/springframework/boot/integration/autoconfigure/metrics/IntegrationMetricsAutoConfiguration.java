/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.integration.autoconfigure.metrics;

import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.integration.autoconfigure.IntegrationAutoConfiguration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Integration's metrics.
 * Orders auto-configuration classes to ensure that the {@link MeterRegistry} bean has
 * been defined before Spring Integration's Micrometer support queries the bean factory
 * for it.
 *
 * @author Andy Wilkinson
 * @since 4.0.0
 */
@AutoConfiguration(before = IntegrationAutoConfiguration.class,
		afterName = "org.springframework.boot.micrometer.metrics.autoconfigure.CompositeMeterRegistryAutoConfiguration")
public final class IntegrationMetricsAutoConfiguration {

}
