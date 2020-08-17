/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics.export.wavefront;

import java.time.Duration;

import com.wavefront.sdk.common.WavefrontSender;
import com.wavefront.sdk.common.clients.WavefrontClient.Builder;
import io.micrometer.core.instrument.Clock;
import io.micrometer.wavefront.WavefrontConfig;
import io.micrometer.wavefront.WavefrontMeterRegistry;

import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.ConditionalOnEnabledMetricsExport;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.wavefront.WavefrontProperties.Sender;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for exporting metrics to Wavefront.
 *
 * @author Jon Schneider
 * @author Artsiom Yudovin
 * @author Stephane Nicoll
 * @since 2.0.0
 */
@Configuration(proxyBeanMethods = false)
@AutoConfigureBefore({ MeterRegistryAutoConfiguration.class, SimpleMetricsExportAutoConfiguration.class })
@AutoConfigureAfter(MetricsAutoConfiguration.class)
@ConditionalOnBean(Clock.class)
@ConditionalOnClass({ WavefrontMeterRegistry.class, WavefrontSender.class })
@ConditionalOnEnabledMetricsExport("wavefront")
@EnableConfigurationProperties(WavefrontProperties.class)
public class WavefrontMetricsExportAutoConfiguration {

	private final WavefrontProperties properties;

	public WavefrontMetricsExportAutoConfiguration(WavefrontProperties properties) {
		this.properties = properties;
	}

	@Bean
	@ConditionalOnMissingBean
	public WavefrontConfig wavefrontConfig() {
		return new WavefrontPropertiesConfigAdapter(this.properties);
	}

	@Bean
	@ConditionalOnMissingBean
	public WavefrontSender wavefrontSender(WavefrontConfig wavefrontConfig) {
		return createWavefrontSender(wavefrontConfig);
	}

	@Bean
	@ConditionalOnMissingBean
	public WavefrontMeterRegistry wavefrontMeterRegistry(WavefrontConfig wavefrontConfig, Clock clock,
			WavefrontSender wavefrontSender) {
		return WavefrontMeterRegistry.builder(wavefrontConfig).clock(clock).wavefrontSender(wavefrontSender).build();
	}

	private WavefrontSender createWavefrontSender(WavefrontConfig wavefrontConfig) {
		Builder builder = WavefrontMeterRegistry.getDefaultSenderBuilder(wavefrontConfig);
		PropertyMapper mapper = PropertyMapper.get().alwaysApplyingWhenNonNull();
		Sender sender = this.properties.getSender();
		mapper.from(sender.getMaxQueueSize()).to(builder::maxQueueSize);
		mapper.from(sender.getFlushInterval()).asInt(Duration::getSeconds).to(builder::flushIntervalSeconds);
		mapper.from(sender.getMessageSize()).asInt(DataSize::toBytes).to(builder::messageSizeBytes);
		return builder.build();
	}

}
