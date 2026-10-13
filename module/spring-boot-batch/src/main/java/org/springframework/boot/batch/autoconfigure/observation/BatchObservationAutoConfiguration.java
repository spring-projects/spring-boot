/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.batch.autoconfigure.observation;

import io.micrometer.observation.ObservationRegistry;

import org.springframework.batch.core.configuration.annotation.BatchObservabilityBeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for instrumentation of Spring Batch
 * Jobs.
 *
 * @author Mark Bonnekessel
 * @since 4.0.0
 */
@AutoConfiguration(
		afterName = "org.springframework.boot.micrometer.observation.autoconfigure.ObservationAutoConfiguration")
@ConditionalOnBean(ObservationRegistry.class)
@ConditionalOnClass({ ObservationRegistry.class, BatchObservabilityBeanPostProcessor.class })
public final class BatchObservationAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	static BatchObservabilityBeanPostProcessor batchObservabilityBeanPostProcessor() {
		return new BatchObservabilityBeanPostProcessor();
	}

}
