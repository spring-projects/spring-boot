/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.metrics.autoconfigure.ssl;

import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.ssl.SslAutoConfiguration;
import org.springframework.boot.info.SslInfo;
import org.springframework.boot.micrometer.metrics.autoconfigure.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.micrometer.metrics.autoconfigure.MetricsAutoConfiguration;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for SSL metrics.
 *
 * @author Moritz Halbritter
 * @since 4.0.0
 */
@AutoConfiguration(after = { CompositeMeterRegistryAutoConfiguration.class, MetricsAutoConfiguration.class,
		SslAutoConfiguration.class })
@ConditionalOnClass(MeterRegistry.class)
@ConditionalOnBean({ MeterRegistry.class, SslBundles.class })
public final class SslMetricsAutoConfiguration {

	@Bean
	SslMeterBinder sslMeterBinder(SslBundles sslBundles, ObjectProvider<SslInfo> sslInfo) {
		return new SslMeterBinder(sslInfo.getIfAvailable(() -> new SslInfo(sslBundles)), sslBundles);
	}

}
