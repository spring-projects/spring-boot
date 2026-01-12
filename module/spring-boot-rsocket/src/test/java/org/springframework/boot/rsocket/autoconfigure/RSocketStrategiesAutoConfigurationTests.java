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

package org.springframework.boot.rsocket.autoconfigure;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.cbor.CBORMapper;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.rsocket.messaging.RSocketStrategiesCustomizer;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.codec.CharSequenceEncoder;
import org.springframework.core.codec.Decoder;
import org.springframework.core.codec.Encoder;
import org.springframework.core.codec.StringDecoder;
import org.springframework.http.codec.cbor.JacksonCborDecoder;
import org.springframework.http.codec.cbor.JacksonCborEncoder;
import org.springframework.http.codec.json.JacksonJsonDecoder;
import org.springframework.http.codec.json.JacksonJsonEncoder;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.web.util.pattern.PathPatternRouteMatcher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link RSocketStrategiesAutoConfiguration}
 *
 * @author Brian Clozel
 */
class RSocketStrategiesAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(RSocketStrategiesAutoConfiguration.class))
		.withBean(JsonMapper.class, () -> JsonMapper.builder().build())
		.withBean(CBORMapper.class, () -> CBORMapper.builder().build());

	@Test
	void shouldCreateDefaultBeans() {
		this.contextRunner.run((context) -> {
			assertThat(context).getBeans(RSocketStrategies.class).hasSize(1);
			RSocketStrategies strategies = context.getBean(RSocketStrategies.class);
			assertThat(strategies.decoders()).hasAtLeastOneElementOfType(JacksonCborDecoder.class)
				.hasAtLeastOneElementOfType(JacksonJsonDecoder.class);
			assertThat(strategies.encoders()).hasAtLeastOneElementOfType(JacksonCborEncoder.class)
				.hasAtLeastOneElementOfType(JacksonJsonEncoder.class);
			assertThat(strategies.routeMatcher()).isInstanceOf(PathPatternRouteMatcher.class);
		});
	}

	@Test
	void shouldUseCustomStrategies() {
		this.contextRunner.withUserConfiguration(UserStrategies.class).run((context) -> {
			assertThat(context).getBeans(RSocketStrategies.class).hasSize(1);
			assertThat(context.getBeanNamesForType(RSocketStrategies.class)).contains("customRSocketStrategies");
		});
	}

	@Test
	void shouldUseStrategiesCustomizer() {
		this.contextRunner.withUserConfiguration(StrategiesCustomizer.class).run((context) -> {
			assertThat(context).getBeans(RSocketStrategies.class).hasSize(1);
			RSocketStrategies strategies = context.getBean(RSocketStrategies.class);
			assertThat(strategies.decoders()).hasAtLeastOneElementOfType(CustomDecoder.class);
			assertThat(strategies.encoders()).hasAtLeastOneElementOfType(CustomEncoder.class);
		});
	}

	@Test
	@Deprecated(since = "4.0.0", forRemoval = true)
	@SuppressWarnings("removal")
	void shouldUseJackson2WhenPreferred() {
		this.contextRunner
			.withConfiguration(AutoConfigurations
				.of(org.springframework.boot.jackson2.autoconfigure.Jackson2AutoConfiguration.class))
			.withPropertyValues("spring.rsocket.preferred-mapper=jackson2")
			.run((context) -> {
				RSocketStrategies strategies = context.getBean(RSocketStrategies.class);
				assertThat(strategies.decoders())
					.hasAtLeastOneElementOfType(org.springframework.http.codec.cbor.Jackson2CborDecoder.class)
					.hasAtLeastOneElementOfType(org.springframework.http.codec.json.Jackson2JsonDecoder.class);
				assertThat(strategies.encoders())
					.hasAtLeastOneElementOfType(org.springframework.http.codec.cbor.Jackson2CborEncoder.class)
					.hasAtLeastOneElementOfType(org.springframework.http.codec.json.Jackson2JsonEncoder.class);
			});
	}

	@Test
	@Deprecated(since = "4.0.0", forRemoval = true)
	@SuppressWarnings("removal")
	void shouldUseJackson2WhenJacksonIsAbsent() {
		this.contextRunner
			.withConfiguration(AutoConfigurations
				.of(org.springframework.boot.jackson2.autoconfigure.Jackson2AutoConfiguration.class))
			.withClassLoader(new FilteredClassLoader(JsonMapper.class, CBORMapper.class))
			.run((context) -> {
				RSocketStrategies strategies = context.getBean(RSocketStrategies.class);
				assertThat(strategies.decoders())
					.hasAtLeastOneElementOfType(org.springframework.http.codec.cbor.Jackson2CborDecoder.class)
					.hasAtLeastOneElementOfType(org.springframework.http.codec.json.Jackson2JsonDecoder.class);
				assertThat(strategies.encoders())
					.hasAtLeastOneElementOfType(org.springframework.http.codec.cbor.Jackson2CborEncoder.class)
					.hasAtLeastOneElementOfType(org.springframework.http.codec.json.Jackson2JsonEncoder.class);
			});
	}

	@Configuration(proxyBeanMethods = false)
	static class UserStrategies {

		@Bean
		RSocketStrategies customRSocketStrategies() {
			return RSocketStrategies.builder()
				.encoder(CharSequenceEncoder.textPlainOnly())
				.decoder(StringDecoder.textPlainOnly())
				.build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class StrategiesCustomizer {

		@Bean
		RSocketStrategiesCustomizer myCustomizer() {
			return (strategies) -> strategies.encoder(mock(CustomEncoder.class)).decoder(mock(CustomDecoder.class));
		}

	}

	interface CustomEncoder extends Encoder<String> {

	}

	interface CustomDecoder extends Decoder<String> {

	}

}
