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

package org.springframework.boot.micrometer.metrics.autoconfigure.export.properties;

import java.time.Duration;

import io.micrometer.core.instrument.push.PushRegistryConfig;
import org.jspecify.annotations.Nullable;

/**
 * Base class for {@link PushRegistryProperties} to {@link PushRegistryConfig} adapters.
 *
 * @param <T> the properties type
 * @author Jon Schneider
 * @author Phillip Webb
 * @author Artsiom Yudovin
 * @since 4.0.0
 */
public abstract class PushRegistryPropertiesConfigAdapter<T extends PushRegistryProperties>
		extends PropertiesConfigAdapter<T> implements PushRegistryConfig {

	public PushRegistryPropertiesConfigAdapter(T properties) {
		super(properties);
	}

	@Override
	public @Nullable String get(String k) {
		return null;
	}

	@Override
	public Duration step() {
		return obtain(T::getStep, PushRegistryConfig.super::step);
	}

	@Override
	public boolean enabled() {
		return obtain(T::isEnabled, PushRegistryConfig.super::enabled);
	}

	@Override
	public int batchSize() {
		return obtain(T::getBatchSize, PushRegistryConfig.super::batchSize);
	}

}
