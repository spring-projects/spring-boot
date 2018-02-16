/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.autoconfigure.metrics.export.properties;

import java.time.Duration;

import io.micrometer.core.instrument.step.StepRegistryConfig;

/**
 * Base class for {@link StepRegistryProperties} to {@link StepRegistryConfig} adapters.
 *
 * @param <T> The properties type
 * @author Jon Schneider
 * @author Phillip Webb
 * @since 2.0.0
 */
public abstract class StepRegistryPropertiesConfigAdapter<T extends StepRegistryProperties>
		extends PropertiesConfigAdapter<T> implements StepRegistryConfig {

	public StepRegistryPropertiesConfigAdapter(T properties) {
		super(properties);
	}

	@Override
	public String prefix() {
		return null;
	}

	@Override
	public String get(String k) {
		return null;
	}

	@Override
	public Duration step() {
		return get(T::getStep, StepRegistryConfig.super::step);
	}

	@Override
	public boolean enabled() {
		return get(T::isEnabled, StepRegistryConfig.super::enabled);
	}

	@Override
	public Duration connectTimeout() {
		return get(T::getConnectTimeout, StepRegistryConfig.super::connectTimeout);
	}

	@Override
	public Duration readTimeout() {
		return get(T::getReadTimeout, StepRegistryConfig.super::readTimeout);
	}

	@Override
	public int numThreads() {
		return get(T::getNumThreads, StepRegistryConfig.super::numThreads);
	}

	@Override
	public int batchSize() {
		return get(T::getBatchSize, StepRegistryConfig.super::batchSize);
	}

}
