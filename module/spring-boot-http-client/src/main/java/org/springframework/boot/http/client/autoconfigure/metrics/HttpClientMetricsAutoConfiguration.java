/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.http.client.autoconfigure.metrics;

import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.micrometer.metrics.MaximumAllowableTagsMeterFilter;
import org.springframework.boot.micrometer.metrics.autoconfigure.MetricsProperties;
import org.springframework.boot.micrometer.metrics.autoconfigure.MetricsProperties.Web.Client;
import org.springframework.boot.micrometer.observation.autoconfigure.ObservationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for HTTP client-related metrics.
 *
 * @author Jon Schneider
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Raheela Aslam
 * @author Brian Clozel
 * @author Moritz Halbritter
 * @since 4.0.0
 */
@AutoConfiguration(
		afterName = "org.springframework.boot.micrometer.metrics.autoconfigure.CompositeMeterRegistryAutoConfiguration")
@ConditionalOnClass({ ObservationProperties.class, MeterRegistry.class, MetricsProperties.class })
@ConditionalOnBean(MeterRegistry.class)
@EnableConfigurationProperties({ MetricsProperties.class, ObservationProperties.class })
public final class HttpClientMetricsAutoConfiguration {

	@Bean
	@Order(0)
	MaximumAllowableTagsMeterFilter metricsHttpClientUriTagFilter(ObservationProperties observationProperties,
			MetricsProperties metricsProperties) {
		Client clientProperties = metricsProperties.getWeb().getClient();
		String meterNamePrefix = observationProperties.getHttp().getClient().getRequests().getName();
		int maxUriTags = clientProperties.getMaxUriTags();
		return new MaximumAllowableTagsMeterFilter(meterNamePrefix, "uri", maxUriTags, "Are you using 'uriVariables'?");
	}

}
