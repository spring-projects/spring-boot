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

package org.springframework.boot.http.codec.autoconfigure;

import java.util.List;

import com.google.gson.Gson;
import kotlinx.serialization.json.Json;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.http.codec.CodecCustomizer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.CodecConfigurer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CodecsAutoConfiguration} with
 * 'spring.http.codecs.preferred-json-mapper' property.
 *
 * @author Vasily Pelikh
 */
class CodecsAutoConfigurationWithPreferredJsonMapperTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(CodecsAutoConfiguration.class));

	@Test
	void noJsonCodecCustomizerAutoConfiguredWhenThereIsNoJsonMappers() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean("jacksonCodecCustomizer")
			.doesNotHaveBean("gsonCodecCustomizer")
			.doesNotHaveBean("kotlinSerializationCodecCustomizer"));
	}

	@Test
	void jacksonCodecCustomizerIsAutoConfiguredWhenJsonMapperIsPresentAndPreferredJsonMapperIsNotSet() {
		this.contextRunner.withUserConfiguration(JsonMapperConfiguration.class)
			.run((context) -> assertThat(context).hasBean("jacksonCodecCustomizer")
				.doesNotHaveBean("gsonCodecCustomizer")
				.doesNotHaveBean("kotlinSerializationCodecCustomizer"));
	}

	@Test
	void jacksonCodecCustomizerIsAutoConfiguredWhenJsonMapperIsPresentAndPreferredJsonMapperCorresponds() {
		this.contextRunner.withUserConfiguration(JsonMapperConfiguration.class)
			.withPropertyValues("spring.http.codecs.preferred-json-mapper=jackson")
			.run((context) -> assertThat(context).hasBean("jacksonCodecCustomizer")
				.doesNotHaveBean("gsonCodecCustomizer")
				.doesNotHaveBean("kotlinSerializationCodecCustomizer"));
	}

	@Test
	void gsonCodecCustomizerIsAutoConfiguredWhenGsonIsPresentAndPreferredJsonMapperCorresponds() {
		this.contextRunner.withUserConfiguration(GsonConfiguration.class)
			.withPropertyValues("spring.http.codecs.preferred-json-mapper=gson")
			.run((context) -> assertThat(context).hasBean("gsonCodecCustomizer")
				.doesNotHaveBean("jacksonCodecCustomizer")
				.doesNotHaveBean("kotlinSerializationCodecCustomizer"));
	}

	@Test
	void kotlinSerializationCodecCustomizerIsAutoConfiguredWhenJsonIsPresentAndPreferredJsonMapperCorresponds() {
		this.contextRunner.withUserConfiguration(KotlinSerializationConfiguration.class)
			.withPropertyValues("spring.http.codecs.preferred-json-mapper=kotlin-serialization")
			.run((context) -> assertThat(context).hasBean("kotlinSerializationCodecCustomizer")
				.doesNotHaveBean("jacksonCodecCustomizer")
				.doesNotHaveBean("gsonCodecCustomizer"));
	}

	@Test
	void jacksonCodecCustomizerIsAutoConfiguredWhenEverythingIsPresentOnClasspathAndPreferredJsonMapperIsNotSet() {
		this.contextRunner
			.withUserConfiguration(JsonMapperConfiguration.class, GsonConfiguration.class,
					KotlinSerializationConfiguration.class)
			.run((context) -> assertThat(context).hasBean("jacksonCodecCustomizer")
				.doesNotHaveBean("gsonCodecCustomizer")
				.doesNotHaveBean("kotlinSerializationCodecCustomizer"));
	}

	@Test
	void jacksonCodecCustomizerIsAutoConfiguredWhenEverythingIsPresentOnClasspathAndPreferredJsonMapperCorresponds() {
		this.contextRunner
			.withUserConfiguration(JsonMapperConfiguration.class, GsonConfiguration.class,
					KotlinSerializationConfiguration.class)
			.withPropertyValues("spring.http.codecs.preferred-json-mapper=jackson")
			.run((context) -> assertThat(context).hasBean("jacksonCodecCustomizer")
				.doesNotHaveBean("gsonCodecCustomizer")
				.doesNotHaveBean("kotlinSerializationCodecCustomizer"));
	}

	@Test
	void gsonCodecCustomizerIsAutoConfiguredWhenEverythingIsPresentOnClasspathAndPreferredJsonMapperCorresponds() {
		this.contextRunner
			.withUserConfiguration(JsonMapperConfiguration.class, GsonConfiguration.class,
					KotlinSerializationConfiguration.class)
			.withPropertyValues("spring.http.codecs.preferred-json-mapper=gson")
			.run((context) -> assertThat(context).hasBean("gsonCodecCustomizer")
				.doesNotHaveBean("jacksonCodecCustomizer")
				.doesNotHaveBean("kotlinSerializationCodecCustomizer"));
	}

	@Test
	void kotlinSerializationCodecCustomizerIsAutoConfiguredWhenEverythingIsPresentOnClasspathAndPreferredJsonMapperCorresponds() {
		this.contextRunner
			.withUserConfiguration(JsonMapperConfiguration.class, GsonConfiguration.class,
					KotlinSerializationConfiguration.class)
			.withPropertyValues("spring.http.codecs.preferred-json-mapper=kotlin-serialization")
			.run((context) -> assertThat(context).hasBean("kotlinSerializationCodecCustomizer")
				.doesNotHaveBean("jacksonCodecCustomizer")
				.doesNotHaveBean("gsonCodecCustomizer"));
	}

	@Test
	void jacksonCodecCustomizerIsAutoConfiguredWhenEverythingIsPresentOnClasspathAndPreferredJsonMapperNotSet() {
		this.contextRunner
			.withUserConfiguration(JsonMapperConfiguration.class, GsonConfiguration.class,
					KotlinSerializationConfiguration.class)
			.run((context) -> assertThat(context).hasBean("jacksonCodecCustomizer")
				.doesNotHaveBean("gsonCodecCustomizer")
				.doesNotHaveBean("kotlinSerializationCodecCustomizer"));
	}

	@Test
	void userProvidedCustomizerCanOverrideJacksonCodecCustomizer() {
		this.contextRunner.withUserConfiguration(JsonMapperConfiguration.class, CodecCustomizerConfiguration.class)
			.run((context) -> {
				List<CodecCustomizer> codecCustomizers = context.getBean(CodecCustomizers.class).codecCustomizers;
				assertThat(codecCustomizers).hasSize(3).element(2).isInstanceOf(TestCodecCustomizer.class);
			});
	}

	@Configuration(proxyBeanMethods = false)
	static class JsonMapperConfiguration {

		@Bean
		JsonMapper jsonMapper() {
			return new JsonMapper();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class GsonConfiguration {

		@Bean
		Gson gson() {
			return new Gson();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class KotlinSerializationConfiguration {

		@Bean
		Json json() {
			return Json.Default;
		}

	}

	@Configuration(proxyBeanMethods = false)
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
