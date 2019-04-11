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

package org.springframework.boot.autoconfigure.rsocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.rsocket.messaging.RSocketStrategiesCustomizer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.codec.CharSequenceEncoder;
import org.springframework.core.codec.StringDecoder;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.messaging.rsocket.RSocketStrategies;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link RSocketStrategiesAutoConfiguration}
 *
 * @author Brian Clozel
 */
public class RSocketStrategiesAutoConfigurationTests {

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(BaseConfiguration.class).withConfiguration(
					AutoConfigurations.of(RSocketStrategiesAutoConfiguration.class));

	@Test
	public void shouldCreateDefaultBeans() {
		this.contextRunner.run((context) -> {
			assertThat(context).getBeans(RSocketStrategies.class).hasSize(1);
			RSocketStrategies strategies = context.getBean(RSocketStrategies.class);
			assertThat(strategies.decoders()).hasSize(1)
					.hasOnlyElementsOfType(Jackson2JsonDecoder.class);
			assertThat(strategies.encoders()).hasSize(1)
					.hasOnlyElementsOfType(Jackson2JsonEncoder.class);
		});
	}

	@Test
	public void shouldUseCustomStrategies() {
		this.contextRunner.withUserConfiguration(UserStrategies.class).run((context) -> {
			assertThat(context).getBeans(RSocketStrategies.class).hasSize(1);
			assertThat(context.getBeanNamesForType(RSocketStrategies.class))
					.contains("customRSocketStrategies");
		});
	}

	@Test
	public void shouldUseStrategiesCustomizer() {
		this.contextRunner.withUserConfiguration(StrategiesCustomizer.class)
				.run((context) -> {
					assertThat(context).getBeans(RSocketStrategies.class).hasSize(1);
					RSocketStrategies strategies = context
							.getBean(RSocketStrategies.class);
					assertThat(strategies.decoders()).hasSize(2)
							.hasAtLeastOneElementOfType(StringDecoder.class);
					assertThat(strategies.encoders()).hasSize(2)
							.hasAtLeastOneElementOfType(CharSequenceEncoder.class);
				});
	}

	@Configuration
	static class BaseConfiguration {

		@Bean
		public ObjectMapper objectMapper() {
			return new ObjectMapper();
		}

	}

	@Configuration
	static class UserStrategies {

		@Bean
		public RSocketStrategies customRSocketStrategies() {
			return RSocketStrategies.builder()
					.encoder(CharSequenceEncoder.textPlainOnly())
					.decoder(StringDecoder.textPlainOnly()).build();
		}

	}

	@Configuration
	static class StrategiesCustomizer {

		@Bean
		public RSocketStrategiesCustomizer myCustomizer() {
			return (strategies) -> strategies.encoder(CharSequenceEncoder.textPlainOnly())
					.decoder(StringDecoder.textPlainOnly());
		}

	}

}
