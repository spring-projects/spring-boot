/*
 * Copyright 2012-present the original author or authors.
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
