/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.autoconfigure.kafka;

import java.time.Duration;
import java.util.Map;

import org.springframework.boot.actuate.autoconfigure.health.CompositeHealthIndicatorConfiguration;
import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.actuate.autoconfigure.health.HealthIndicatorAutoConfiguration;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.kafka.KafkaHealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaAdmin;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link KafkaHealthIndicator}.
 *
 * @author Juan Rada
 * @since 2.0.0
 */
@Configuration
@ConditionalOnClass(KafkaAdmin.class)
@ConditionalOnBean(KafkaAdmin.class)
@ConditionalOnEnabledHealthIndicator("kafka")
@AutoConfigureBefore(HealthIndicatorAutoConfiguration.class)
@AutoConfigureAfter(KafkaAutoConfiguration.class)
@EnableConfigurationProperties(KafkaHealthIndicatorProperties.class)
public class KafkaHealthIndicatorAutoConfiguration
		extends CompositeHealthIndicatorConfiguration<KafkaHealthIndicator, KafkaAdmin> {

	private final Map<String, KafkaAdmin> admins;

	private final KafkaHealthIndicatorProperties properties;

	public KafkaHealthIndicatorAutoConfiguration(Map<String, KafkaAdmin> admins,
			KafkaHealthIndicatorProperties properties) {
		this.admins = admins;
		this.properties = properties;
	}

	@Bean
	@ConditionalOnMissingBean(name = "kafkaHealthIndicator")
	public HealthIndicator kafkaHealthIndicator() {
		return createHealthIndicator(this.admins);
	}

	@Override
	protected KafkaHealthIndicator createHealthIndicator(KafkaAdmin source) {
		Duration responseTimeout = this.properties.getResponseTimeout();
		return new KafkaHealthIndicator(source, responseTimeout.toMillis());
	}

}
