/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics.export.signalfx;

import java.time.Duration;

import org.springframework.boot.actuate.autoconfigure.metrics.export.properties.StepRegistryProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.DeprecatedConfigurationProperty;

/**
 * {@link ConfigurationProperties @ConfigurationProperties} for configuring metrics export
 * to SignalFX.
 *
 * @author Jon Schneider
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @since 2.0.0
 * @deprecated since 3.5.0 for removal in 3.7.0
 */
@ConfigurationProperties("management.signalfx.metrics.export")
@Deprecated(since = "3.5.0", forRemoval = true)
public class SignalFxProperties extends StepRegistryProperties {

	/**
	 * Step size (i.e. reporting frequency) to use.
	 */
	private Duration step = Duration.ofSeconds(10);

	/**
	 * SignalFX access token.
	 */
	private String accessToken;

	/**
	 * URI to ship metrics to.
	 */
	private String uri = "https://ingest.signalfx.com";

	/**
	 * Uniquely identifies the app instance that is publishing metrics to SignalFx.
	 * Defaults to the local host name.
	 */
	private String source;

	/**
	 * Type of histogram to publish.
	 */
	private HistogramType publishedHistogramType = HistogramType.DEFAULT;

	@Override
	@DeprecatedConfigurationProperty(since = "3.5.0", reason = "Deprecated in Micrometer 1.15.0")
	@Deprecated(since = "3.5.0", forRemoval = true)
	public Duration getStep() {
		return this.step;
	}

	@Override
	@Deprecated(since = "3.5.0", forRemoval = true)
	public void setStep(Duration step) {
		this.step = step;
	}

	@DeprecatedConfigurationProperty(since = "3.5.0", reason = "Deprecated in Micrometer 1.15.0")
	@Deprecated(since = "3.5.0", forRemoval = true)
	public String getAccessToken() {
		return this.accessToken;
	}

	@Deprecated(since = "3.5.0", forRemoval = true)
	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	@DeprecatedConfigurationProperty(since = "3.5.0", reason = "Deprecated in Micrometer 1.15.0")
	@Deprecated(since = "3.5.0", forRemoval = true)
	public String getUri() {
		return this.uri;
	}

	@Deprecated(since = "3.5.0", forRemoval = true)
	public void setUri(String uri) {
		this.uri = uri;
	}

	@DeprecatedConfigurationProperty(since = "3.5.0", reason = "Deprecated in Micrometer 1.15.0")
	@Deprecated(since = "3.5.0", forRemoval = true)
	public String getSource() {
		return this.source;
	}

	@Deprecated(since = "3.5.0", forRemoval = true)
	public void setSource(String source) {
		this.source = source;
	}

	@DeprecatedConfigurationProperty(since = "3.5.0", reason = "Deprecated in Micrometer 1.15.0")
	@Deprecated(since = "3.5.0", forRemoval = true)
	public HistogramType getPublishedHistogramType() {
		return this.publishedHistogramType;
	}

	@Deprecated(since = "3.5.0", forRemoval = true)
	public void setPublishedHistogramType(HistogramType publishedHistogramType) {
		this.publishedHistogramType = publishedHistogramType;
	}

	@Override
	@DeprecatedConfigurationProperty(since = "3.5.0", reason = "Deprecated in Micrometer 1.15.0")
	@Deprecated(since = "3.5.0", forRemoval = true)
	public boolean isEnabled() {
		return super.isEnabled();
	}

	@Override
	@Deprecated(since = "3.5.0", forRemoval = true)
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
	}

	@Override
	@DeprecatedConfigurationProperty(since = "3.5.0", reason = "Deprecated in Micrometer 1.15.0")
	@Deprecated(since = "3.5.0", forRemoval = true)
	public Duration getConnectTimeout() {
		return super.getConnectTimeout();
	}

	@Override
	@Deprecated(since = "3.5.0", forRemoval = true)
	public void setConnectTimeout(Duration connectTimeout) {
		super.setConnectTimeout(connectTimeout);
	}

	@Override
	@DeprecatedConfigurationProperty(since = "3.5.0", reason = "Deprecated in Micrometer 1.15.0")
	@Deprecated(since = "3.5.0", forRemoval = true)
	public Duration getReadTimeout() {
		return super.getReadTimeout();
	}

	@Override
	@Deprecated(since = "3.5.0", forRemoval = true)
	public void setReadTimeout(Duration readTimeout) {
		super.setReadTimeout(readTimeout);
	}

	@Override
	@DeprecatedConfigurationProperty(since = "3.5.0", reason = "Deprecated in Micrometer 1.15.0")
	@Deprecated(since = "3.5.0", forRemoval = true)
	public Integer getBatchSize() {
		return super.getBatchSize();
	}

	@Override
	@Deprecated(since = "3.5.0", forRemoval = true)
	public void setBatchSize(Integer batchSize) {
		super.setBatchSize(batchSize);
	}

	@Deprecated(since = "3.5.0", forRemoval = true)
	public enum HistogramType {

		/**
		 * Default, time-based histogram.
		 */
		DEFAULT,

		/**
		 * Cumulative histogram.
		 */
		CUMULATIVE,

		/**
		 * Delta histogram.
		 */
		DELTA

	}

}
