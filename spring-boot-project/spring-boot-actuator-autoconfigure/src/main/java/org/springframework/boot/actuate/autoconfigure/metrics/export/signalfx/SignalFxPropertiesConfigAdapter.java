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

package org.springframework.boot.actuate.autoconfigure.metrics.export.signalfx;

import io.micrometer.signalfx.SignalFxConfig;

import org.springframework.boot.actuate.autoconfigure.metrics.export.properties.StepRegistryPropertiesConfigAdapter;
import org.springframework.boot.actuate.autoconfigure.metrics.export.signalfx.SignalFxProperties.HistogramType;

/**
 * Adapter to convert {@link SignalFxProperties} to a {@link SignalFxConfig}.
 *
 * @author Jon Schneider
 * @since 2.0.0
 */
public class SignalFxPropertiesConfigAdapter extends StepRegistryPropertiesConfigAdapter<SignalFxProperties>
		implements SignalFxConfig {

	/**
	 * Constructs a new SignalFxPropertiesConfigAdapter with the specified
	 * SignalFxProperties.
	 * @param properties the SignalFxProperties to be used for configuration
	 * @throws IllegalArgumentException if the access token is not set in the properties
	 */
	public SignalFxPropertiesConfigAdapter(SignalFxProperties properties) {
		super(properties);
		accessToken(); // validate that an access token is set
	}

	/**
	 * Returns the prefix for SignalFx metrics export configuration properties.
	 * @return the prefix for SignalFx metrics export configuration properties
	 */
	@Override
	public String prefix() {
		return "management.signalfx.metrics.export";
	}

	/**
	 * Returns the access token for SignalFx.
	 * @return the access token
	 */
	@Override
	public String accessToken() {
		return get(SignalFxProperties::getAccessToken, SignalFxConfig.super::accessToken);
	}

	/**
	 * Returns the URI for the SignalFx properties.
	 * @return the URI for the SignalFx properties
	 */
	@Override
	public String uri() {
		return get(SignalFxProperties::getUri, SignalFxConfig.super::uri);
	}

	/**
	 * Returns the source value from the SignalFxPropertiesConfigAdapter class.
	 * @return the source value
	 */
	@Override
	public String source() {
		return get(SignalFxProperties::getSource, SignalFxConfig.super::source);
	}

	/**
	 * Returns a boolean value indicating whether to publish the cumulative histogram.
	 * @return true if the cumulative histogram should be published, false otherwise
	 */
	@Override
	public boolean publishCumulativeHistogram() {
		return get(this::isPublishCumulativeHistogram, SignalFxConfig.super::publishCumulativeHistogram);
	}

	/**
	 * Checks if the published histogram type is cumulative.
	 * @param properties the SignalFxProperties object containing the histogram type
	 * @return true if the published histogram type is cumulative, false otherwise
	 */
	private boolean isPublishCumulativeHistogram(SignalFxProperties properties) {
		return HistogramType.CUMULATIVE == properties.getPublishedHistogramType();
	}

	/**
	 * Returns a boolean value indicating whether to publish delta histogram.
	 * @return true if delta histogram should be published, false otherwise
	 */
	@Override
	public boolean publishDeltaHistogram() {
		return get(this::isPublishDeltaHistogram, SignalFxConfig.super::publishDeltaHistogram);
	}

	/**
	 * Determines if the published histogram type is a delta histogram.
	 * @param properties the SignalFx properties
	 * @return true if the published histogram type is a delta histogram, false otherwise
	 */
	private boolean isPublishDeltaHistogram(SignalFxProperties properties) {
		return HistogramType.DELTA == properties.getPublishedHistogramType();
	}

}
