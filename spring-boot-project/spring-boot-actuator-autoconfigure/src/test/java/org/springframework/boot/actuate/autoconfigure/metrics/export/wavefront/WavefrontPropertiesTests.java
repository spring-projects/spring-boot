/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics.export.wavefront;

import io.micrometer.wavefront.WavefrontConfig;

import org.springframework.boot.actuate.autoconfigure.metrics.export.properties.StepRegistryPropertiesTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WavefrontProperties}.
 *
 * @author Stephane Nicoll
 */
public class WavefrontPropertiesTests extends StepRegistryPropertiesTests {

	@Override
	public void defaultValuesAreConsistent() {
		WavefrontProperties properties = new WavefrontProperties();
		WavefrontConfig config = WavefrontConfig.DEFAULT_DIRECT;
		assertStepRegistryDefaultValues(properties, config);
		assertThat(properties.getUri().toString()).isEqualTo(config.uri());
		// source has no static default value
		assertThat(properties.getApiToken()).isEqualTo(config.apiToken());
		assertThat(properties.getGlobalPrefix()).isEqualTo(config.globalPrefix());
	}

}
