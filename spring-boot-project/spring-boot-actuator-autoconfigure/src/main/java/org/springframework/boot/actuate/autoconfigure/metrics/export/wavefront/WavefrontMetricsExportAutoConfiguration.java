/*
 * Copyright 2012-2022 the original author or authors.
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

import com.wavefront.sdk.common.WavefrontSender;
import io.micrometer.core.instrument.Clock;
import io.micrometer.wavefront.WavefrontConfig;
import io.micrometer.wavefront.WavefrontMeterRegistry;

import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.ConditionalOnEnabledMetricsExport;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.wavefront.WavefrontProperties;
import org.springframework.boot.actuate.autoconfigure.wavefront.WavefrontSenderConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for exporting metrics to Wavefront.
 *
 * @author Jon Schneider
 * @author Artsiom Yudovin
 * @author Stephane Nicoll
 * @since 2.0.0
 */
@AutoConfiguration(
		before = { CompositeMeterRegistryAutoConfiguration.class, SimpleMetricsExportAutoConfiguration.class },
		after = MetricsAutoConfiguration.class)
@ConditionalOnBean({ Clock.class, WavefrontSender.class })
@ConditionalOnClass({ WavefrontMeterRegistry.class, WavefrontSender.class })
@ConditionalOnEnabledMetricsExport("wavefront")
@EnableConfigurationProperties(WavefrontProperties.class)
@Import(WavefrontSenderConfiguration.class)
public class WavefrontMetricsExportAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public WavefrontConfig wavefrontConfig(WavefrontProperties properties) {
		return new WavefrontPropertiesConfigAdapter(properties);
	}

	@Bean
	@ConditionalOnMissingBean
	public WavefrontMeterRegistry wavefrontMeterRegistry(WavefrontConfig wavefrontConfig, Clock clock,
			WavefrontSender wavefrontSender) {
		return WavefrontMeterRegistry.builder(wavefrontConfig).clock(clock).wavefrontSender(wavefrontSender).build();
	}

}
