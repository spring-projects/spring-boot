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

import io.micrometer.signalfx.SignalFxConfig;

import org.springframework.boot.actuate.autoconfigure.metrics.export.properties.StepRegistryPropertiesConfigAdapter;

/**
 * Adapter to convert {@link SignalFxProperties} to a {@link SignalFxConfig}.
 *
 * @author Jon Schneider
 * @since 2.0.0
 * @deprecated since 3.5.0 for removal in 3.7.0
 */
@Deprecated(since = "3.5.0", forRemoval = true)
@SuppressWarnings("removal")
public class SignalFxPropertiesConfigAdapter extends StepRegistryPropertiesConfigAdapter<SignalFxProperties>
		implements SignalFxConfig {

	public SignalFxPropertiesConfigAdapter(SignalFxProperties properties) {
		super(properties);
		accessToken(); // validate that an access token is set
	}

	@Override
	public String prefix() {
		return "management.signalfx.metrics.export";
	}

	@Override
	public String accessToken() {
		return get(SignalFxProperties::getAccessToken, SignalFxConfig.super::accessToken);
	}

	@Override
	public String uri() {
		return get(SignalFxProperties::getUri, SignalFxConfig.super::uri);
	}

	@Override
	public String source() {
		return get(SignalFxProperties::getSource, SignalFxConfig.super::source);
	}

	@Override
	public boolean publishCumulativeHistogram() {
		return get(this::isPublishCumulativeHistogram, SignalFxConfig.super::publishCumulativeHistogram);
	}

	private boolean isPublishCumulativeHistogram(SignalFxProperties properties) {
		return SignalFxProperties.HistogramType.CUMULATIVE == properties.getPublishedHistogramType();
	}

	@Override
	public boolean publishDeltaHistogram() {
		return get(this::isPublishDeltaHistogram, SignalFxConfig.super::publishDeltaHistogram);
	}

	private boolean isPublishDeltaHistogram(SignalFxProperties properties) {
		return SignalFxProperties.HistogramType.DELTA == properties.getPublishedHistogramType();
	}

}
