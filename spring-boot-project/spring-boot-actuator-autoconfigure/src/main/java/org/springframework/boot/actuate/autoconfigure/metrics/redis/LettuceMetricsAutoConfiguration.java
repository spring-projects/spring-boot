/*
 * Copyright 2012-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.autoconfigure.metrics.redis;

import io.lettuce.core.RedisClient;
import io.lettuce.core.metrics.MicrometerCommandLatencyRecorder;
import io.lettuce.core.metrics.MicrometerOptions;
import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.data.redis.ClientResourcesBuilderCustomizer;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuration for Lettuce metrics.
 *
 * @author Antonin Arquey
 * @author Yanming Zhou
 * @since 2.6.0
 */
@Configuration(proxyBeanMethods = false)
@AutoConfigureBefore(RedisAutoConfiguration.class)
@AutoConfigureAfter({ MetricsAutoConfiguration.class, CompositeMeterRegistryAutoConfiguration.class })
@ConditionalOnClass({ RedisClient.class, MicrometerCommandLatencyRecorder.class })
@ConditionalOnBean(MeterRegistry.class)
public class LettuceMetricsAutoConfiguration {

	@Bean
	public ClientResourcesBuilderCustomizer lettuceMetrics(MeterRegistry meterRegistry) {
		MicrometerOptions options = MicrometerOptions.builder().histogram(true).build();
		return (client) -> client.commandLatencyRecorder(new MicrometerCommandLatencyRecorder(meterRegistry, options));
	}

}
