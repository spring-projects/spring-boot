/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.metrics.autoconfigure;

import io.micrometer.core.instrument.composite.CompositeMeterRegistry;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Import;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for a
 * {@link CompositeMeterRegistry}.
 *
 * @author Andy Wilkinson
 * @since 4.0.0
 */
@AutoConfiguration
@Import({ NoOpMeterRegistryConfiguration.class, CompositeMeterRegistryConfiguration.class })
@ConditionalOnClass(CompositeMeterRegistry.class)
public final class CompositeMeterRegistryAutoConfiguration {

}
