/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.metrics.autoconfigure.export.properties;

import io.micrometer.core.instrument.step.StepRegistryConfig;

/**
 * Base class for {@link StepRegistryProperties} to {@link StepRegistryConfig} adapters.
 *
 * @param <T> the properties type
 * @author Jon Schneider
 * @author Phillip Webb
 * @author Artsiom Yudovin
 * @since 4.0.0
 */
public abstract class StepRegistryPropertiesConfigAdapter<T extends StepRegistryProperties>
		extends PushRegistryPropertiesConfigAdapter<T> {

	public StepRegistryPropertiesConfigAdapter(T properties) {
		super(properties);
	}

}
