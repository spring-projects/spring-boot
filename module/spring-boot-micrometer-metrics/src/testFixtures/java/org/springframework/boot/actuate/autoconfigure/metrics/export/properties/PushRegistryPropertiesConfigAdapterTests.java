/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.autoconfigure.metrics.export.properties;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import org.springframework.boot.micrometer.metrics.autoconfigure.export.properties.PropertiesConfigAdapter;
import org.springframework.boot.micrometer.metrics.autoconfigure.export.properties.PushRegistryProperties;
import org.springframework.boot.micrometer.metrics.autoconfigure.export.properties.PushRegistryPropertiesConfigAdapter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base test for {@link PushRegistryPropertiesConfigAdapter} implementations.
 *
 * @param <P> properties used by the tests
 * @param <A> adapter used by the tests
 * @author Stephane Nicoll
 * @author Artsiom Yudovin
 */
public abstract class PushRegistryPropertiesConfigAdapterTests<P extends PushRegistryProperties, A extends PushRegistryPropertiesConfigAdapter<P>>
		extends AbstractPropertiesConfigAdapterTests<P, PropertiesConfigAdapter<P>> {

	protected PushRegistryPropertiesConfigAdapterTests(Class<A> adapter) {
		super(adapter);
	}

	protected abstract P createProperties();

	protected abstract A createConfigAdapter(P properties);

	@Test
	void whenPropertiesStepIsSetAdapterStepReturnsIt() {
		P properties = createProperties();
		properties.setStep(Duration.ofSeconds(42));
		assertThat(createConfigAdapter(properties).step()).hasSeconds(42);
	}

	@Test
	void whenPropertiesEnabledIsSetAdapterEnabledReturnsIt() {
		P properties = createProperties();
		properties.setEnabled(false);
		assertThat(createConfigAdapter(properties).enabled()).isFalse();
	}

	@Test
	protected void whenPropertiesBatchSizeIsSetAdapterBatchSizeReturnsIt() {
		P properties = createProperties();
		properties.setBatchSize(10042);
		assertThat(createConfigAdapter(properties).batchSize()).isEqualTo(10042);
	}

}
