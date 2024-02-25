/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics.export.appoptics;

import io.micrometer.appoptics.AppOpticsConfig;

import org.springframework.boot.actuate.autoconfigure.metrics.export.properties.StepRegistryPropertiesConfigAdapter;

/**
 * Adapter to convert {@link AppOpticsProperties} to an {@link AppOpticsConfig}.
 *
 * @author Stephane Nicoll
 */
class AppOpticsPropertiesConfigAdapter extends StepRegistryPropertiesConfigAdapter<AppOpticsProperties>
		implements AppOpticsConfig {

	/**
	 * Constructs a new AppOpticsPropertiesConfigAdapter with the specified
	 * AppOpticsProperties.
	 * @param properties the AppOpticsProperties to be used for configuring the adapter
	 */
	AppOpticsPropertiesConfigAdapter(AppOpticsProperties properties) {
		super(properties);
	}

	/**
	 * Returns the prefix for the AppOptics metrics export configuration properties. The
	 * prefix is used to group the properties related to AppOptics metrics export.
	 * @return the prefix for the AppOptics metrics export configuration properties
	 */
	@Override
	public String prefix() {
		return "management.appoptics.metrics.export";
	}

	/**
	 * Returns the URI for the AppOptics properties.
	 * @return the URI for the AppOptics properties
	 */
	@Override
	public String uri() {
		return get(AppOpticsProperties::getUri, AppOpticsConfig.super::uri);
	}

	/**
	 * Returns the API token for the AppOptics configuration.
	 * @return the API token for the AppOptics configuration
	 */
	@Override
	public String apiToken() {
		return get(AppOpticsProperties::getApiToken, AppOpticsConfig.super::apiToken);
	}

	/**
	 * Returns the host tag value.
	 * @return the host tag value
	 */
	@Override
	public String hostTag() {
		return get(AppOpticsProperties::getHostTag, AppOpticsConfig.super::hostTag);
	}

	/**
	 * Returns the value of the floorTimes property from the
	 * AppOpticsPropertiesConfigAdapter. If the property is not set in the adapter, it
	 * falls back to the default value provided by the AppOpticsConfig interface.
	 * @return the value of the floorTimes property
	 */
	@Override
	public boolean floorTimes() {
		return get(AppOpticsProperties::isFloorTimes, AppOpticsConfig.super::floorTimes);
	}

}
