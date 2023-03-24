/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.wavefront;

import java.net.URI;

import org.junit.jupiter.api.Test;

import org.springframework.boot.context.properties.source.InvalidConfigurationPropertyValueException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link WavefrontProperties}.
 *
 * @author Moritz Halbritter
 */
class WavefrontPropertiesTests {

	@Test
	void apiTokenIsOptionalWhenUsingProxy() {
		WavefrontProperties sut = new WavefrontProperties();
		sut.setUri(URI.create("proxy://localhost:2878"));
		sut.setApiToken(null);
		assertThat(sut.getApiTokenOrThrow()).isNull();
		assertThat(sut.getEffectiveUri()).isEqualTo(URI.create("http://localhost:2878"));
	}

	@Test
	void apiTokenIsMandatoryWhenNotUsingProxy() {
		WavefrontProperties sut = new WavefrontProperties();
		sut.setUri(URI.create("http://localhost:2878"));
		sut.setApiToken(null);
		assertThat(sut.getEffectiveUri()).isEqualTo(URI.create("http://localhost:2878"));
		assertThatThrownBy(sut::getApiTokenOrThrow).isInstanceOf(InvalidConfigurationPropertyValueException.class)
			.hasMessageContaining("management.wavefront.api-token");
	}

}
