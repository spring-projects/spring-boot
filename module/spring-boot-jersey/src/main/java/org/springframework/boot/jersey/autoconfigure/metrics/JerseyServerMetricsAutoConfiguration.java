/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jersey.autoconfigure.metrics;

import io.micrometer.observation.ObservationRegistry;
import org.glassfish.jersey.micrometer.server.JerseyObservationConvention;
import org.glassfish.jersey.micrometer.server.ObservationApplicationEventListener;
import org.glassfish.jersey.server.ResourceConfig;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jersey.autoconfigure.ResourceConfigCustomizer;
import org.springframework.boot.micrometer.metrics.MaximumAllowableTagsMeterFilter;
import org.springframework.boot.micrometer.metrics.autoconfigure.MetricsProperties;
import org.springframework.boot.micrometer.observation.autoconfigure.ObservationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Jersey server instrumentation.
 *
 * @author Michael Weirauch
 * @author Michael Simons
 * @author Andy Wilkinson
 * @author Moritz Halbritter
 * @since 4.0.0
 */
@AutoConfiguration(
		afterName = "org.springframework.boot.micrometer.observation.autoconfigure.ObservationAutoConfiguration")
@ConditionalOnWebApplication(type = Type.SERVLET)
@ConditionalOnClass({ ResourceConfig.class, ObservationApplicationEventListener.class })
@ConditionalOnBean({ ResourceConfig.class, ObservationRegistry.class })
@EnableConfigurationProperties({ MetricsProperties.class, ObservationProperties.class })
public final class JerseyServerMetricsAutoConfiguration {

	private final ObservationProperties observationProperties;

	JerseyServerMetricsAutoConfiguration(ObservationProperties observationProperties) {
		this.observationProperties = observationProperties;
	}

	@Bean
	ResourceConfigCustomizer jerseyServerObservationResourceConfigCustomizer(ObservationRegistry observationRegistry,
			ObjectProvider<JerseyObservationConvention> jerseyObservationConvention) {
		String metricName = this.observationProperties.getHttp().getServer().getRequests().getName();
		return (config) -> config.register(new ObservationApplicationEventListener(observationRegistry, metricName,
				jerseyObservationConvention.getIfAvailable()));
	}

	@Bean
	@Order(0)
	MaximumAllowableTagsMeterFilter jerseyMetricsUriTagFilter(MetricsProperties metricsProperties) {
		String meterNamePrefix = this.observationProperties.getHttp().getServer().getRequests().getName();
		int maxUriTags = metricsProperties.getWeb().getServer().getMaxUriTags();
		return new MaximumAllowableTagsMeterFilter(meterNamePrefix, "uri", maxUriTags);
	}

}
