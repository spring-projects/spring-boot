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

package org.springframework.boot.autoconfigure.http.codec;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.web.codec.CodecCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.CodecConfigurer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CodecsAutoConfiguration}.
 *
 * @author Andy Wilkinson
 */
public class CodecsAutoConfigurationTests {

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(CodecsAutoConfiguration.class));

	@Test
	public void jacksonCodecCustomizerBacksOffWhenThereIsNoObjectMapper() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(CodecCustomizer.class));
	}

	@Test
	public void jacksonCodecCustomizerIsAutoConfiguredWhenObjectMapperIsPresent() {
		this.contextRunner.withUserConfiguration(ObjectMapperConfiguration.class)
				.run((context) -> assertThat(context).hasSingleBean(CodecCustomizer.class));
	}

	@Test
	public void userProvidedCustomizerCanOverrideJacksonCodecCustomizer() {
		this.contextRunner.withUserConfiguration(ObjectMapperConfiguration.class, CodecCustomizerConfiguration.class)
				.run((context) -> {
					List<CodecCustomizer> codecCustomizers = context.getBean(CodecCustomizers.class).codecCustomizers;
					assertThat(codecCustomizers).hasSize(2);
					assertThat(codecCustomizers.get(1)).isInstanceOf(TestCodecCustomizer.class);
				});
	}

	@Configuration
	static class ObjectMapperConfiguration {

		@Bean
		ObjectMapper objectMapper() {
			return new ObjectMapper();
		}

	}

	@Configuration
	static class CodecCustomizerConfiguration {

		@Bean
		CodecCustomizer codecCustomizer() {
			return new TestCodecCustomizer();
		}

		@Bean
		CodecCustomizers codecCustomizers(List<CodecCustomizer> customizers) {
			return new CodecCustomizers(customizers);
		}

	}

	private static final class TestCodecCustomizer implements CodecCustomizer {

		@Override
		public void customize(CodecConfigurer configurer) {
		}

	}

	private static final class CodecCustomizers {

		private final List<CodecCustomizer> codecCustomizers;

		private CodecCustomizers(List<CodecCustomizer> codecCustomizers) {
			this.codecCustomizers = codecCustomizers;
		}

	}

}
