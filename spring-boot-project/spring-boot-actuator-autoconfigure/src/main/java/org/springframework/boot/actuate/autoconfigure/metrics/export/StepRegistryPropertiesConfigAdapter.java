/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics.export;

import java.time.Duration;

import io.micrometer.core.instrument.step.StepRegistryConfig;

/**
 * Base class for {@link StepRegistryProperties} to {@link StepRegistryConfig} adapters.
 *
 * @param <T> The properties type
 * @param <C> The config type
 * @author Jon Schneider
 * @author Phillip Webb
 * @since 2.0.0
 */
public abstract class StepRegistryPropertiesConfigAdapter<T extends StepRegistryProperties, C extends StepRegistryConfig>
		extends PropertiesConfigAdapter<T, C> implements StepRegistryConfig {

	public StepRegistryPropertiesConfigAdapter(T properties, C defaults) {
		super(properties, defaults);
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
		return get(T::getStep, C::step);
	}

	@Override
	public boolean enabled() {
		return get(T::getEnabled, C::enabled);
	}

	@Override
	public Duration connectTimeout() {
		return get(T::getConnectTimeout, C::connectTimeout);
	}

	@Override
	public Duration readTimeout() {
		return get(T::getReadTimeout, C::readTimeout);
	}

	@Override
	public int numThreads() {
		return get(T::getNumThreads, C::numThreads);
	}

	@Override
	public int batchSize() {
		return get(T::getBatchSize, C::batchSize);
	}

}
