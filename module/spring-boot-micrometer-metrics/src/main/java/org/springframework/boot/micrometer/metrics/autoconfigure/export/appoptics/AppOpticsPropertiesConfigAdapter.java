/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.metrics.autoconfigure.export.appoptics;

import io.micrometer.appoptics.AppOpticsConfig;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.micrometer.metrics.autoconfigure.export.properties.StepRegistryPropertiesConfigAdapter;

/**
 * Adapter to convert {@link AppOpticsProperties} to an {@link AppOpticsConfig}.
 *
 * @author Stephane Nicoll
 * @deprecated since 4.2.0 for removal in 4.4.0 with no replacement as AppOptics has
 * retired
 */
@Deprecated(since = "4.2.0", forRemoval = true)
@SuppressWarnings("removal")
class AppOpticsPropertiesConfigAdapter extends StepRegistryPropertiesConfigAdapter<AppOpticsProperties>
		implements AppOpticsConfig {

	AppOpticsPropertiesConfigAdapter(AppOpticsProperties properties) {
		super(properties);
	}

	@Override
	public String prefix() {
		return "management.appoptics.metrics.export";
	}

	@Override
	public String uri() {
		return obtain(AppOpticsProperties::getUri, AppOpticsConfig.super::uri);
	}

	@Override
	public String apiToken() {
		return obtain(AppOpticsProperties::getApiToken, AppOpticsConfig.super::apiToken);
	}

	@Override
	public @Nullable String hostTag() {
		return get(AppOpticsProperties::getHostTag, AppOpticsConfig.super::hostTag);
	}

	@Override
	public boolean floorTimes() {
		return obtain(AppOpticsProperties::isFloorTimes, AppOpticsConfig.super::floorTimes);
	}

}
