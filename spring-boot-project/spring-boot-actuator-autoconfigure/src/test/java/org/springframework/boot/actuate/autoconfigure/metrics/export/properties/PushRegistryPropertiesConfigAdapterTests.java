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

package org.springframework.boot.actuate.autoconfigure.metrics.export.properties;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base test for {@link PushRegistryPropertiesConfigAdapter} implementations.
 *
 * @param <P> properties used by the tests
 * @param <A> adapter used by the tests
 * @author Stephane Nicoll
 * @author Artsiom Yudovin
 */
public abstract class PushRegistryPropertiesConfigAdapterTests<P extends PushRegistryProperties, A extends PushRegistryPropertiesConfigAdapter<P>> {

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
