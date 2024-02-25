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

package org.springframework.boot.actuate.autoconfigure.tracing.wavefront;

import brave.handler.SpanHandler;
import com.wavefront.sdk.common.WavefrontSender;
import com.wavefront.sdk.common.application.ApplicationTags;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.tracing.reporter.wavefront.SpanMetrics;
import io.micrometer.tracing.reporter.wavefront.WavefrontBraveSpanHandler;
import io.micrometer.tracing.reporter.wavefront.WavefrontOtelSpanExporter;
import io.micrometer.tracing.reporter.wavefront.WavefrontSpanHandler;
import io.opentelemetry.sdk.trace.export.SpanExporter;

import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.tracing.ConditionalOnEnabledTracing;
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
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Wavefront tracing.
 *
 * @author Moritz Halbritter
 * @author Glenn Oppegard
 * @since 3.0.0
 */
@AutoConfiguration(after = { MetricsAutoConfiguration.class, CompositeMeterRegistryAutoConfiguration.class,
		WavefrontAutoConfiguration.class })
@ConditionalOnClass({ WavefrontSender.class, WavefrontSpanHandler.class })
@EnableConfigurationProperties(WavefrontProperties.class)
@Import(WavefrontSenderConfiguration.class)
public class WavefrontTracingAutoConfiguration {

	/**
     * Creates a WavefrontSpanHandler bean if a WavefrontSender bean is present and tracing is enabled.
     * The WavefrontSpanHandler is responsible for sending spans to Wavefront.
     *
     * @param properties       the WavefrontProperties object containing configuration properties
     * @param wavefrontSender  the WavefrontSender bean used for sending spans to Wavefront
     * @param spanMetrics      the SpanMetrics bean used for tracking span metrics
     * @param applicationTags  the ApplicationTags bean containing application metadata
     * @return the WavefrontSpanHandler bean
     */
    @Bean
	@ConditionalOnMissingBean
	@ConditionalOnBean(WavefrontSender.class)
	@ConditionalOnEnabledTracing
	WavefrontSpanHandler wavefrontSpanHandler(WavefrontProperties properties, WavefrontSender wavefrontSender,
			SpanMetrics spanMetrics, ApplicationTags applicationTags) {
		return new WavefrontSpanHandler(properties.getSender().getMaxQueueSize(), wavefrontSender, spanMetrics,
				properties.getSourceOrDefault(), applicationTags, properties.getTraceDerivedCustomTagKeys());
	}

	/**
     * MeterRegistrySpanMetricsConfiguration class.
     */
    @Configuration(proxyBeanMethods = false)
	@ConditionalOnBean(MeterRegistry.class)
	static class MeterRegistrySpanMetricsConfiguration {

		/**
         * Creates a new instance of MeterRegistrySpanMetrics if no other bean of the same type is present.
         * 
         * @param meterRegistry the MeterRegistry to be used for creating the MeterRegistrySpanMetrics
         * @return a new instance of MeterRegistrySpanMetrics
         */
        @Bean
		@ConditionalOnMissingBean
		MeterRegistrySpanMetrics meterRegistrySpanMetrics(MeterRegistry meterRegistry) {
			return new MeterRegistrySpanMetrics(meterRegistry);
		}

	}

	/**
     * NoopSpanMetricsConfiguration class.
     */
    @Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean(MeterRegistry.class)
	static class NoopSpanMetricsConfiguration {

		/**
         * Returns a {@link SpanMetrics} bean if no other bean of the same type is present in the application context.
         * If a bean of the same type is already present, this method will not be executed.
         * 
         * The returned {@link SpanMetrics} bean is a {@link SpanMetrics.NOOP} instance, which is a no-op implementation
         * of the {@link SpanMetrics} interface. It does not perform any actual metric recording or reporting.
         * 
         * @return a {@link SpanMetrics} bean representing a no-op implementation of the {@link SpanMetrics} interface
         */
        @Bean
		@ConditionalOnMissingBean
		SpanMetrics meterRegistrySpanMetrics() {
			return SpanMetrics.NOOP;
		}

	}

	/**
     * WavefrontBrave class.
     */
    @Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(SpanHandler.class)
	static class WavefrontBrave {

		/**
         * Creates a new instance of WavefrontBraveSpanHandler.
         * This method is annotated with @Bean, @ConditionalOnMissingBean, and @ConditionalOnEnabledTracing.
         * It takes a WavefrontSpanHandler as a parameter and returns a new instance of WavefrontBraveSpanHandler.
         * 
         * @param wavefrontSpanHandler The WavefrontSpanHandler to be used by the WavefrontBraveSpanHandler.
         * @return A new instance of WavefrontBraveSpanHandler.
         */
        @Bean
		@ConditionalOnMissingBean
		@ConditionalOnEnabledTracing
		WavefrontBraveSpanHandler wavefrontBraveSpanHandler(WavefrontSpanHandler wavefrontSpanHandler) {
			return new WavefrontBraveSpanHandler(wavefrontSpanHandler);
		}

	}

	/**
     * WavefrontOpenTelemetry class.
     */
    @Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(SpanExporter.class)
	static class WavefrontOpenTelemetry {

		/**
         * Creates a new instance of WavefrontOtelSpanExporter with the provided WavefrontSpanHandler.
         * This method is annotated with @Bean, @ConditionalOnMissingBean, and @ConditionalOnEnabledTracing.
         * 
         * @param wavefrontSpanHandler The WavefrontSpanHandler to be used by the WavefrontOtelSpanExporter.
         * @return A new instance of WavefrontOtelSpanExporter.
         */
        @Bean
		@ConditionalOnMissingBean
		@ConditionalOnEnabledTracing
		WavefrontOtelSpanExporter wavefrontOtelSpanExporter(WavefrontSpanHandler wavefrontSpanHandler) {
			return new WavefrontOtelSpanExporter(wavefrontSpanHandler);
		}

	}

}
