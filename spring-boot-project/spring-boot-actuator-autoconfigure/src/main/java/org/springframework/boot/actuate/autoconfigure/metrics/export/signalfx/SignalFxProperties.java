/*
 * Copyright 2012-2024 the original author or authors.
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

/**
 * {@link ConfigurationProperties @ConfigurationProperties} for configuring metrics export
 * to SignalFX.
 *
 * @author Jon Schneider
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @since 2.0.0
 */
@ConfigurationProperties(prefix = "management.signalfx.metrics.export")
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

	/**
	 * Returns the step duration for the SignalFxProperties class.
	 * @return the step duration
	 */
	@Override
	public Duration getStep() {
		return this.step;
	}

	/**
	 * Sets the step duration for the SignalFxProperties.
	 * @param step the duration of each step
	 */
	@Override
	public void setStep(Duration step) {
		this.step = step;
	}

	/**
	 * Returns the access token.
	 * @return the access token
	 */
	public String getAccessToken() {
		return this.accessToken;
	}

	/**
	 * Sets the access token for SignalFx.
	 * @param accessToken the access token to set
	 */
	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	/**
	 * Returns the URI of the SignalFxProperties.
	 * @return the URI of the SignalFxProperties
	 */
	public String getUri() {
		return this.uri;
	}

	/**
	 * Sets the URI for the SignalFxProperties.
	 * @param uri the URI to be set
	 */
	public void setUri(String uri) {
		this.uri = uri;
	}

	/**
	 * Returns the source of the SignalFxProperties.
	 * @return the source of the SignalFxProperties
	 */
	public String getSource() {
		return this.source;
	}

	/**
	 * Sets the source of the SignalFxProperties.
	 * @param source the source to set
	 */
	public void setSource(String source) {
		this.source = source;
	}

	/**
	 * Returns the published histogram type.
	 * @return the published histogram type
	 */
	public HistogramType getPublishedHistogramType() {
		return this.publishedHistogramType;
	}

	/**
	 * Sets the type of histogram to be published.
	 * @param publishedHistogramType the type of histogram to be published
	 */
	public void setPublishedHistogramType(HistogramType publishedHistogramType) {
		this.publishedHistogramType = publishedHistogramType;
	}

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
