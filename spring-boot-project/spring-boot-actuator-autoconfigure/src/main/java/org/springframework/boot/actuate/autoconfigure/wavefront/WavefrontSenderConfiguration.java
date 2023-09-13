/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.wavefront;

import java.time.Duration;

import com.wavefront.sdk.common.WavefrontSender;
import com.wavefront.sdk.common.clients.WavefrontClient.Builder;

import org.springframework.boot.actuate.autoconfigure.metrics.export.ConditionalOnEnabledMetricsExport;
import org.springframework.boot.actuate.autoconfigure.metrics.export.wavefront.WavefrontMetricsExportAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.tracing.ConditionalOnEnabledTracing;
import org.springframework.boot.actuate.autoconfigure.tracing.wavefront.WavefrontTracingAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

/**
 * Configuration for {@link WavefrontSender}. This configuration is imported from
 * {@link WavefrontMetricsExportAutoConfiguration} and
 * {@link WavefrontTracingAutoConfiguration}.
 *
 * @author Moritz Halbritter
 * @since 3.0.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(WavefrontSender.class)
@EnableConfigurationProperties(WavefrontProperties.class)
public class WavefrontSenderConfiguration {

	@Bean
	@ConditionalOnMissingBean
	@Conditional(WavefrontTracingOrMetricsCondition.class)
	public WavefrontSender wavefrontSender(WavefrontProperties properties) {
		Builder builder = new Builder(properties.getEffectiveUri().toString(), properties.getApiTokenOrThrow());
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		WavefrontProperties.Sender sender = properties.getSender();
		map.from(sender.getMaxQueueSize()).to(builder::maxQueueSize);
		map.from(sender.getFlushInterval()).asInt(Duration::getSeconds).to(builder::flushIntervalSeconds);
		map.from(sender.getMessageSize()).asInt(DataSize::toBytes).to(builder::messageSizeBytes);
		map.from(sender.getBatchSize()).to(builder::batchSize);
		return builder.build();
	}

	static final class WavefrontTracingOrMetricsCondition extends AnyNestedCondition {

		WavefrontTracingOrMetricsCondition() {
			super(ConfigurationPhase.REGISTER_BEAN);
		}

		@ConditionalOnEnabledTracing
		static class TracingCondition {

		}

		@ConditionalOnEnabledMetricsExport("wavefront")
		static class MetricsCondition {

		}

	}

}
