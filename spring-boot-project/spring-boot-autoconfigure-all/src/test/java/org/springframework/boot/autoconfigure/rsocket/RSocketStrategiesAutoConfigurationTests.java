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

package org.springframework.boot.autoconfigure.rsocket;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.rsocket.messaging.RSocketStrategiesCustomizer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.codec.CharSequenceEncoder;
import org.springframework.core.codec.Decoder;
import org.springframework.core.codec.Encoder;
import org.springframework.core.codec.StringDecoder;
import org.springframework.http.codec.cbor.Jackson2CborDecoder;
import org.springframework.http.codec.cbor.Jackson2CborEncoder;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
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

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner().withConfiguration(
			AutoConfigurations.of(JacksonAutoConfiguration.class, RSocketStrategiesAutoConfiguration.class));

	@Test
	void shouldCreateDefaultBeans() {
		this.contextRunner.run((context) -> {
			assertThat(context).getBeans(RSocketStrategies.class).hasSize(1);
			RSocketStrategies strategies = context.getBean(RSocketStrategies.class);
			assertThat(strategies.decoders()).hasAtLeastOneElementOfType(Jackson2CborDecoder.class)
				.hasAtLeastOneElementOfType(Jackson2JsonDecoder.class);
			assertThat(strategies.encoders()).hasAtLeastOneElementOfType(Jackson2CborEncoder.class)
				.hasAtLeastOneElementOfType(Jackson2JsonEncoder.class);
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
