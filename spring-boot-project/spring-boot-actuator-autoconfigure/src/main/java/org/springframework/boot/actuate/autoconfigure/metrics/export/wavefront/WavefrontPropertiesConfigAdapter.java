/*
 * Copyright 2012-2019 the original author or authors.
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

import io.micrometer.wavefront.WavefrontConfig;

import org.springframework.boot.actuate.autoconfigure.metrics.export.properties.PushRegistryPropertiesConfigAdapter;

/**
 * Adapter to convert {@link WavefrontProperties} to a {@link WavefrontConfig}.
 *
 * @author Jon Schneider
 * @since 2.0.0
 */
public class WavefrontPropertiesConfigAdapter extends PushRegistryPropertiesConfigAdapter<WavefrontProperties>
		implements WavefrontConfig {

	public WavefrontPropertiesConfigAdapter(WavefrontProperties properties) {
		super(properties);
	}

	@Override
	public String get(String k) {
		return null;
	}

	@Override
	public String uri() {
		return get(this::getUriAsString, WavefrontConfig.DEFAULT_DIRECT::uri);
	}

	@Override
	public String source() {
		return get(WavefrontProperties::getSource, WavefrontConfig.super::source);
	}

	@Override
	public String apiToken() {
		return get(WavefrontProperties::getApiToken, WavefrontConfig.super::apiToken);
	}

	@Override
	public String globalPrefix() {
		return get(WavefrontProperties::getGlobalPrefix, WavefrontConfig.super::globalPrefix);
	}

	private String getUriAsString(WavefrontProperties properties) {
		return (properties.getUri() != null) ? properties.getUri().toString() : null;
	}

}
