/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.redis.autoconfigure.observation;

import io.lettuce.core.RedisClient;
import io.lettuce.core.tracing.MicrometerTracing;
import io.micrometer.observation.ObservationRegistry;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.data.redis.autoconfigure.ClientResourcesBuilderCustomizer;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for Lettuce observability.
 *
 * @author Antonin Arquey
 * @author Yanming Zhou
 * @author Dũng Đăng Minh
 * @since 4.0.0
 */
@AutoConfiguration(before = DataRedisAutoConfiguration.class,
		afterName = "org.springframework.boot.micrometer.observation.autoconfigure.ObservationAutoConfiguration")
@ConditionalOnClass({ RedisClient.class, MicrometerTracing.class, ObservationRegistry.class })
@ConditionalOnBean(ObservationRegistry.class)
public final class LettuceObservationAutoConfiguration {

	@Bean
	ClientResourcesBuilderCustomizer lettuceObservation(ObservationRegistry observationRegistry) {
		return (client) -> client.tracing(new MicrometerTracing(observationRegistry, "Redis"));
	}

}
