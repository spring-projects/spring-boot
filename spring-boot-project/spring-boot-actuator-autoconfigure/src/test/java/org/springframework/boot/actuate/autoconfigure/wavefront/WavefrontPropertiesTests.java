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

import com.wavefront.sdk.common.clients.service.token.TokenService.Type;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import org.springframework.boot.actuate.autoconfigure.wavefront.WavefrontProperties.TokenType;
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
		WavefrontProperties properties = new WavefrontProperties();
		properties.setUri(URI.create("proxy://localhost:2878"));
		properties.setApiToken(null);
		assertThat(properties.getApiTokenOrThrow()).isNull();
		assertThat(properties.getEffectiveUri()).isEqualTo(URI.create("http://localhost:2878"));
	}

	@Test
	void apiTokenIsMandatoryWhenNotUsingProxy() {
		WavefrontProperties properties = new WavefrontProperties();
		properties.setUri(URI.create("http://localhost:2878"));
		properties.setApiToken(null);
		assertThat(properties.getEffectiveUri()).isEqualTo(URI.create("http://localhost:2878"));
		assertThatThrownBy(properties::getApiTokenOrThrow)
			.isInstanceOf(InvalidConfigurationPropertyValueException.class)
			.hasMessageContaining("management.wavefront.api-token");
	}

	@Test
	void shouldNotFailIfTokenTypeIsSetToNoToken() {
		WavefrontProperties properties = new WavefrontProperties();
		properties.setUri(URI.create("http://localhost:2878"));
		properties.setApiTokenType(TokenType.NO_TOKEN);
		properties.setApiToken(null);
		assertThat(properties.getApiTokenOrThrow()).isNull();
	}

	@Test
	void wavefrontApiTokenTypeWhenUsingProxy() {
		WavefrontProperties properties = new WavefrontProperties();
		properties.setUri(URI.create("proxy://localhost:2878"));
		assertThat(properties.getWavefrontApiTokenType()).isEqualTo(Type.NO_TOKEN);
	}

	@Test
	void wavefrontApiTokenTypeWhenNotUsingProxy() {
		WavefrontProperties properties = new WavefrontProperties();
		properties.setUri(URI.create("http://localhost:2878"));
		assertThat(properties.getWavefrontApiTokenType()).isEqualTo(Type.WAVEFRONT_API_TOKEN);
	}

	@ParameterizedTest
	@EnumSource(TokenType.class)
	void wavefrontApiTokenMapping(TokenType from) {
		WavefrontProperties properties = new WavefrontProperties();
		properties.setApiTokenType(from);
		Type expected = Type.valueOf(from.name());
		assertThat(properties.getWavefrontApiTokenType()).isEqualTo(expected);
	}

}
