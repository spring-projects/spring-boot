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

package org.springframework.boot.micrometer.metrics.autoconfigure.export.atlas;

import java.time.Duration;

import com.netflix.spectator.atlas.AtlasConfig;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.micrometer.metrics.autoconfigure.export.properties.PropertiesConfigAdapter;

/**
 * Adapter to convert {@link AtlasProperties} to an {@link AtlasConfig}.
 *
 * @author Jon Schneider
 * @author Phillip Webb
 */
class AtlasPropertiesConfigAdapter extends PropertiesConfigAdapter<AtlasProperties> implements AtlasConfig {

	AtlasPropertiesConfigAdapter(AtlasProperties properties) {
		super(properties);
	}

	@Override
	public @Nullable String get(String key) {
		return null;
	}

	@Override
	public Duration step() {
		return getRequired(AtlasProperties::getStep, AtlasConfig.super::step);
	}

	@Override
	public boolean enabled() {
		return getRequired(AtlasProperties::isEnabled, AtlasConfig.super::enabled);
	}

	@Override
	public Duration connectTimeout() {
		return getRequired(AtlasProperties::getConnectTimeout, AtlasConfig.super::connectTimeout);
	}

	@Override
	public Duration readTimeout() {
		return getRequired(AtlasProperties::getReadTimeout, AtlasConfig.super::readTimeout);
	}

	@Override
	public int numThreads() {
		return getRequired(AtlasProperties::getNumThreads, AtlasConfig.super::numThreads);
	}

	@Override
	public int batchSize() {
		return getRequired(AtlasProperties::getBatchSize, AtlasConfig.super::batchSize);
	}

	@Override
	public String uri() {
		return getRequired(AtlasProperties::getUri, AtlasConfig.super::uri);
	}

	@Override
	public Duration meterTTL() {
		return getRequired(AtlasProperties::getMeterTimeToLive, AtlasConfig.super::meterTTL);
	}

	@Override
	public boolean lwcEnabled() {
		return getRequired(AtlasProperties::isLwcEnabled, AtlasConfig.super::lwcEnabled);
	}

	@Override
	public Duration lwcStep() {
		return getRequired(AtlasProperties::getLwcStep, AtlasConfig.super::lwcStep);
	}

	@Override
	public boolean lwcIgnorePublishStep() {
		return getRequired(AtlasProperties::isLwcIgnorePublishStep, AtlasConfig.super::lwcIgnorePublishStep);
	}

	@Override
	public Duration configRefreshFrequency() {
		return getRequired(AtlasProperties::getConfigRefreshFrequency, AtlasConfig.super::configRefreshFrequency);
	}

	@Override
	public Duration configTTL() {
		return getRequired(AtlasProperties::getConfigTimeToLive, AtlasConfig.super::configTTL);
	}

	@Override
	public String configUri() {
		return getRequired(AtlasProperties::getConfigUri, AtlasConfig.super::configUri);
	}

	@Override
	public String evalUri() {
		return getRequired(AtlasProperties::getEvalUri, AtlasConfig.super::evalUri);
	}

}
