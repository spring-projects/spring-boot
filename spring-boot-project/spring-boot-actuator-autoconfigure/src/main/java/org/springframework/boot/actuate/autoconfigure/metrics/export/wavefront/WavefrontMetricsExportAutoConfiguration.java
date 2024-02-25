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

import java.util.Map;

import com.wavefront.sdk.common.WavefrontSender;
import com.wavefront.sdk.common.application.ApplicationTags;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.wavefront.WavefrontConfig;
import io.micrometer.wavefront.WavefrontMeterRegistry;

import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.ConditionalOnEnabledMetricsExport;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.wavefront.WavefrontAutoConfiguration;
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
 * @author Glenn Oppegard
 * @since 2.0.0
 */
@AutoConfiguration(
		before = { CompositeMeterRegistryAutoConfiguration.class, SimpleMetricsExportAutoConfiguration.class },
		after = { MetricsAutoConfiguration.class, WavefrontAutoConfiguration.class })
@ConditionalOnBean(Clock.class)
@ConditionalOnClass({ WavefrontMeterRegistry.class, WavefrontSender.class })
@ConditionalOnEnabledMetricsExport("wavefront")
@EnableConfigurationProperties(WavefrontProperties.class)
@Import(WavefrontSenderConfiguration.class)
public class WavefrontMetricsExportAutoConfiguration {

	/**
	 * Generates a WavefrontConfig bean if no other bean of the same type is present. Uses
	 * the provided WavefrontProperties to create a WavefrontPropertiesConfigAdapter.
	 * @param properties The WavefrontProperties used to create the WavefrontConfig bean.
	 * @return The WavefrontConfig bean created using the provided properties.
	 */
	@Bean
	@ConditionalOnMissingBean
	public WavefrontConfig wavefrontConfig(WavefrontProperties properties) {
		return new WavefrontPropertiesConfigAdapter(properties);
	}

	/**
	 * Creates a WavefrontMeterRegistry bean if there is no existing bean of the same type
	 * and a WavefrontSender bean is present.
	 * @param wavefrontConfig the WavefrontConfig bean used for configuring the
	 * WavefrontMeterRegistry
	 * @param clock the Clock bean used for providing the current time
	 * @param wavefrontSender the WavefrontSender bean used for sending metrics to
	 * Wavefront
	 * @return the created WavefrontMeterRegistry bean
	 */
	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnBean(WavefrontSender.class)
	public WavefrontMeterRegistry wavefrontMeterRegistry(WavefrontConfig wavefrontConfig, Clock clock,
			WavefrontSender wavefrontSender) {
		return WavefrontMeterRegistry.builder(wavefrontConfig).clock(clock).wavefrontSender(wavefrontSender).build();
	}

	/**
	 * Returns a MeterRegistryCustomizer for WavefrontMeterRegistry, customized with
	 * common tags from the provided wavefrontApplicationTags. This method is
	 * conditionally executed only if an instance of ApplicationTags is present.
	 * @param wavefrontApplicationTags The ApplicationTags instance containing the
	 * wavefront application tags.
	 * @return The MeterRegistryCustomizer for WavefrontMeterRegistry with common tags
	 * configured.
	 */
	@Bean
	@ConditionalOnBean(ApplicationTags.class)
	MeterRegistryCustomizer<WavefrontMeterRegistry> wavefrontApplicationTagsCustomizer(
			ApplicationTags wavefrontApplicationTags) {
		Tags commonTags = Tags.of(wavefrontApplicationTags.toPointTags().entrySet().stream().map(this::asTag).toList());
		return (registry) -> registry.config().commonTags(commonTags);
	}

	/**
	 * Converts a Map.Entry object to a Tag object.
	 * @param entry the Map.Entry object to be converted
	 * @return the converted Tag object
	 */
	private Tag asTag(Map.Entry<String, String> entry) {
		return Tag.of(entry.getKey(), entry.getValue());
	}

}
