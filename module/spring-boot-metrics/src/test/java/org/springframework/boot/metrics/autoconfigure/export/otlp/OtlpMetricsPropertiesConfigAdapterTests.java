/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.metrics.autoconfigure.export.otlp;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.micrometer.registry.otlp.AggregationTemporality;
import io.micrometer.registry.otlp.HistogramFlavor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.metrics.autoconfigure.export.otlp.OtlpMetricsExportAutoConfiguration.PropertiesOtlpMetricsConnectionDetails;
import org.springframework.boot.metrics.autoconfigure.export.otlp.OtlpMetricsProperties.Meter;
import org.springframework.boot.opentelemetry.autoconfigure.OpenTelemetryProperties;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.spy;

/**
 * Tests for {@link OtlpMetricsPropertiesConfigAdapter}.
 *
 * @author Eddú Meléndez
 * @author Moritz Halbritter
 */
class OtlpMetricsPropertiesConfigAdapterTests {

	private OtlpMetricsProperties properties;

	private OpenTelemetryProperties openTelemetryProperties;

	private MockEnvironment environment;

	private OtlpMetricsConnectionDetails connectionDetails;

	@BeforeEach
	void setUp() {
		this.properties = new OtlpMetricsProperties();
		this.openTelemetryProperties = new OpenTelemetryProperties();
		this.environment = new MockEnvironment();
		this.connectionDetails = new PropertiesOtlpMetricsConnectionDetails(this.properties);
	}

	@Test
	void whenPropertiesUrlIsNotSetAdapterUrlReturnsDefault() {
		assertThat(this.properties.getUrl()).isNull();
		assertThat(createAdapter().url()).isEqualTo("http://localhost:4318/v1/metrics");
	}

	@Test
	void whenPropertiesUrlIsNotSetThenUseOtlpConfigUrlAsFallback() {
		assertThat(this.properties.getUrl()).isNull();
		OtlpMetricsPropertiesConfigAdapter adapter = spy(createAdapter());
		given(adapter.get("management.otlp.metrics.export.url")).willReturn("https://my-endpoint/v1/metrics");
		assertThat(adapter.url()).isEqualTo("https://my-endpoint/v1/metrics");
	}

	@Test
	void whenPropertiesUrlIsSetAdapterUrlReturnsIt() {
		this.properties.setUrl("http://another-url:4318/v1/metrics");
		assertThat(createAdapter().url()).isEqualTo("http://another-url:4318/v1/metrics");
	}

	@Test
	void whenPropertiesAggregationTemporalityIsNotSetAdapterAggregationTemporalityReturnsCumulative() {
		assertThat(createAdapter().aggregationTemporality()).isSameAs(AggregationTemporality.CUMULATIVE);
	}

	@Test
	void whenPropertiesAggregationTemporalityIsSetAdapterAggregationTemporalityReturnsIt() {
		this.properties.setAggregationTemporality(AggregationTemporality.DELTA);
		assertThat(createAdapter().aggregationTemporality()).isSameAs(AggregationTemporality.DELTA);
	}

	@Test
	void whenOpenTelemetryPropertiesResourceAttributesIsSetAdapterResourceAttributesReturnsIt() {
		this.openTelemetryProperties.setResourceAttributes(Map.of("service.name", "boot-service"));
		assertThat(createAdapter().resourceAttributes()).containsEntry("service.name", "boot-service");
	}

	@Test
	void whenPropertiesHeadersIsSetAdapterHeadersReturnsIt() {
		this.properties.setHeaders(Map.of("header", "value"));
		assertThat(createAdapter().headers()).containsEntry("header", "value");
	}

	@Test
	void whenPropertiesHistogramFlavorIsNotSetAdapterHistogramFlavorReturnsExplicitBucketHistogram() {
		assertThat(createAdapter().histogramFlavor()).isSameAs(HistogramFlavor.EXPLICIT_BUCKET_HISTOGRAM);
	}

	@Test
	void whenPropertiesHistogramFlavorIsSetAdapterHistogramFlavorReturnsIt() {
		this.properties.setHistogramFlavor(HistogramFlavor.BASE2_EXPONENTIAL_BUCKET_HISTOGRAM);
		assertThat(createAdapter().histogramFlavor()).isSameAs(HistogramFlavor.BASE2_EXPONENTIAL_BUCKET_HISTOGRAM);
	}

	@Test
	void whenPropertiesHistogramFlavorPerMeterIsNotSetAdapterHistogramFlavorReturnsEmptyMap() {
		assertThat(createAdapter().histogramFlavorPerMeter()).isEmpty();
	}

	@Test
	void whenPropertiesHistogramFlavorPerMeterIsSetAdapterHistogramFlavorPerMeterReturnsIt() {
		Meter meterProperties = new Meter();
		meterProperties.setHistogramFlavor(HistogramFlavor.BASE2_EXPONENTIAL_BUCKET_HISTOGRAM);
		this.properties.getMeter().put("my.histograms", meterProperties);
		assertThat(createAdapter().histogramFlavorPerMeter()).containsEntry("my.histograms",
				HistogramFlavor.BASE2_EXPONENTIAL_BUCKET_HISTOGRAM);
	}

	@Test
	void whenPropertiesMaxScaleIsNotSetAdapterMaxScaleReturns20() {
		assertThat(createAdapter().maxScale()).isEqualTo(20);
	}

	@Test
	void whenPropertiesMaxScaleIsSetAdapterMaxScaleReturnsIt() {
		this.properties.setMaxScale(5);
		assertThat(createAdapter().maxScale()).isEqualTo(5);
	}

	@Test
	void whenPropertiesMaxBucketCountIsNotSetAdapterMaxBucketCountReturns160() {
		assertThat(createAdapter().maxBucketCount()).isEqualTo(160);
	}

	@Test
	void whenPropertiesMaxBucketCountIsSetAdapterMaxBucketCountReturnsIt() {
		this.properties.setMaxBucketCount(6);
		assertThat(createAdapter().maxBucketCount()).isEqualTo(6);
	}

	@Test
	void whenPropertiesMaxBucketsPerMeterIsNotSetAdapterMaxBucketsPerMeterReturnsEmptyMap() {
		assertThat(createAdapter().maxBucketsPerMeter()).isEmpty();
	}

	@Test
	void whenPropertiesMaxBucketsPerMeterIsSetAdapterMaxBucketsPerMeterReturnsIt() {
		Meter meterProperties = new Meter();
		meterProperties.setMaxBucketCount(111);
		this.properties.getMeter().put("my.histograms", meterProperties);
		assertThat(createAdapter().maxBucketsPerMeter()).containsEntry("my.histograms", 111);
	}

	@Test
	void whenPropertiesBaseTimeUnitIsNotSetAdapterBaseTimeUnitReturnsMillis() {
		assertThat(createAdapter().baseTimeUnit()).isSameAs(TimeUnit.MILLISECONDS);
	}

	@Test
	void whenPropertiesBaseTimeUnitIsSetAdapterBaseTimeUnitReturnsIt() {
		this.properties.setBaseTimeUnit(TimeUnit.SECONDS);
		assertThat(createAdapter().baseTimeUnit()).isSameAs(TimeUnit.SECONDS);
	}

	@Test
	void serviceNameOverridesApplicationName() {
		this.environment.setProperty("spring.application.name", "alpha");
		this.openTelemetryProperties.setResourceAttributes(Map.of("service.name", "beta"));
		assertThat(createAdapter().resourceAttributes()).containsEntry("service.name", "beta");
	}

	@Test
	void shouldUseApplicationNameIfServiceNameIsNotSet() {
		this.environment.setProperty("spring.application.name", "alpha");
		assertThat(createAdapter().resourceAttributes()).containsEntry("service.name", "alpha");
	}

	@Test
	void shouldUseDefaultApplicationNameIfApplicationNameIsNotSet() {
		assertThat(createAdapter().resourceAttributes()).containsEntry("service.name", "unknown_service");
	}

	@Test
	void serviceNamespaceOverridesApplicationGroup() {
		this.environment.setProperty("spring.application.group", "alpha");
		this.openTelemetryProperties.setResourceAttributes(Map.of("service.namespace", "beta"));
		assertThat(createAdapter().resourceAttributes()).containsEntry("service.namespace", "beta");
	}

	@Test
	void shouldUseApplicationGroupIfServiceNamspaceIsNotSet() {
		this.environment.setProperty("spring.application.group", "alpha");
		assertThat(createAdapter().resourceAttributes()).containsEntry("service.namespace", "alpha");
	}

	@Test
	void shouldUseDefaultApplicationGroupIfApplicationGroupIsNotSet() {
		assertThat(createAdapter().resourceAttributes()).doesNotContainKey("service.namespace");
	}

	private OtlpMetricsPropertiesConfigAdapter createAdapter() {
		return new OtlpMetricsPropertiesConfigAdapter(this.properties, this.openTelemetryProperties,
				this.connectionDetails, this.environment);
	}

}
