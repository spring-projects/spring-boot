/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.cache.autoconfigure.metrics;

import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.cache.autoconfigure.CacheAutoConfiguration;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for metrics on all available
 * {@link Cache caches}.
 *
 * @author Stephane Nicoll
 * @since 4.0.0
 */
@AutoConfiguration(after = { CacheAutoConfiguration.class },
		afterName = "org.springframework.boot.micrometer.metrics.autoconfigure.CompositeMeterRegistryAutoConfiguration")
@ConditionalOnBean(CacheManager.class)
@ConditionalOnClass(MeterRegistry.class)
@Import({ CacheMeterBinderProvidersConfiguration.class, CacheMetricsRegistrarConfiguration.class })
public final class CacheMetricsAutoConfiguration {

}
