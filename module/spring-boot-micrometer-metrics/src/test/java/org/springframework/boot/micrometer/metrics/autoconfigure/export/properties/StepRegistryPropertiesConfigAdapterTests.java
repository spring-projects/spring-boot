/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.metrics.autoconfigure.export.properties;

import org.springframework.boot.actuate.autoconfigure.metrics.export.properties.PushRegistryPropertiesConfigAdapterTests;

/**
 * Base test for {@link StepRegistryPropertiesConfigAdapter} implementations.
 *
 * @param <P> properties used by the tests
 * @param <A> adapter used by the tests
 * @author Stephane Nicoll
 * @author Artsiom Yudovin
 */
public abstract class StepRegistryPropertiesConfigAdapterTests<P extends StepRegistryProperties, A extends StepRegistryPropertiesConfigAdapter<P>>
		extends PushRegistryPropertiesConfigAdapterTests<P, A> {

	protected StepRegistryPropertiesConfigAdapterTests(Class<A> adapter) {
		super(adapter);
	}

}
