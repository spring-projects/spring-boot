/*
 * Copyright 2012-present the original author or authors.
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
		return obtain(AtlasProperties::getStep, AtlasConfig.super::step);
	}

	@Override
	public boolean enabled() {
		return obtain(AtlasProperties::isEnabled, AtlasConfig.super::enabled);
	}

	@Override
	public Duration connectTimeout() {
		return obtain(AtlasProperties::getConnectTimeout, AtlasConfig.super::connectTimeout);
	}

	@Override
	public Duration readTimeout() {
		return obtain(AtlasProperties::getReadTimeout, AtlasConfig.super::readTimeout);
	}

	@Override
	public int numThreads() {
		return obtain(AtlasProperties::getNumThreads, AtlasConfig.super::numThreads);
	}

	@Override
	public int batchSize() {
		return obtain(AtlasProperties::getBatchSize, AtlasConfig.super::batchSize);
	}

	@Override
	public String uri() {
		return obtain(AtlasProperties::getUri, AtlasConfig.super::uri);
	}

	@Override
	public Duration meterTTL() {
		return obtain(AtlasProperties::getMeterTimeToLive, AtlasConfig.super::meterTTL);
	}

	@Override
	public boolean lwcEnabled() {
		return obtain(AtlasProperties::isLwcEnabled, AtlasConfig.super::lwcEnabled);
	}

	@Override
	public Duration lwcStep() {
		return obtain(AtlasProperties::getLwcStep, AtlasConfig.super::lwcStep);
	}

	@Override
	public boolean lwcIgnorePublishStep() {
		return obtain(AtlasProperties::isLwcIgnorePublishStep, AtlasConfig.super::lwcIgnorePublishStep);
	}

	@Override
	public Duration configRefreshFrequency() {
		return obtain(AtlasProperties::getConfigRefreshFrequency, AtlasConfig.super::configRefreshFrequency);
	}

	@Override
	public Duration configTTL() {
		return obtain(AtlasProperties::getConfigTimeToLive, AtlasConfig.super::configTTL);
	}

	@Override
	public String configUri() {
		return obtain(AtlasProperties::getConfigUri, AtlasConfig.super::configUri);
	}

	@Override
	public String evalUri() {
		return obtain(AtlasProperties::getEvalUri, AtlasConfig.super::evalUri);
	}

}
