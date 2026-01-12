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
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import kotlinx.serialization.json.Json;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.http.codec.CodecCustomizer;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.ResolvableType;
import org.springframework.http.MediaType;
import org.springframework.http.codec.CodecConfigurer;
import org.springframework.http.codec.CodecConfigurer.DefaultCodecs;
import org.springframework.http.codec.EncoderHttpMessageWriter;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.codec.json.KotlinSerializationJsonEncoder;
import org.springframework.http.codec.support.DefaultClientCodecConfigurer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CodecsAutoConfiguration}.
 *
 * @author Madhura Bhave
 * @author Andy Wilkinson
 */
class CodecsAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(CodecsAutoConfiguration.class));

	@Test
	void autoConfigShouldProvideALoggingRequestDetailsCustomizer() {
		this.contextRunner.run((context) -> assertThat(defaultCodecs(context))
			.hasFieldOrPropertyWithValue("enableLoggingRequestDetails", false));
	}

	@Test
	void loggingRequestDetailsCustomizerShouldUseHttpCodecsProperties() {
		this.contextRunner.withPropertyValues("spring.http.codecs.log-request-details=true")
			.run((context) -> assertThat(defaultCodecs(context))
				.hasFieldOrPropertyWithValue("enableLoggingRequestDetails", true));
	}

	@Test
	void maxInMemorySizeShouldUseHttpCodecProperties() {
		this.contextRunner.withPropertyValues("spring.http.codecs.max-in-memory-size=64KB")
			.run((context) -> assertThat(defaultCodecs(context)).hasFieldOrPropertyWithValue("maxInMemorySize",
					64 * 1024));
	}

	@Test
	void defaultCodecCustomizerBeanShouldHaveOrderZero() {
		this.contextRunner
			.run((context) -> assertThat(context.getBean("defaultCodecCustomizer", Ordered.class).getOrder()).isZero());
	}

	@Test
	void jacksonCodecCustomizerBacksOffWhenThereIsNoJsonMapper() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean("jacksonCodecCustomizer"));
	}

	@Test
	void jacksonCodecCustomizerIsAutoConfiguredWhenJsonMapperIsPresent() {
		this.contextRunner.withUserConfiguration(JsonMapperConfiguration.class)
			.run((context) -> assertThat(context).hasBean("jacksonCodecCustomizer"));
	}

	@Test
	@Deprecated(since = "4.0.0", forRemoval = true)
	void jacksonCodecCustomizerBacksOffWhenJackson2IsPreferred() {
		this.contextRunner.withUserConfiguration(JsonMapperConfiguration.class)
			.withPropertyValues("spring.http.codecs.preferred-json-mapper=jackson2")
			.run((context) -> assertThat(context).doesNotHaveBean("jacksonCodecCustomizer"));
	}

	@Test
	@Deprecated(since = "4.0.0", forRemoval = true)
	void jackson2CodecCustomizerIsAutoConfiguredWhenObjectMapperIsPresentAndJackson2IsPreferred() {
		this.contextRunner.withUserConfiguration(ObjectMapperConfiguration.class)
			.withPropertyValues("spring.http.codecs.preferred-json-mapper=jackson2")
			.run((context) -> assertThat(context).hasBean("jackson2CodecCustomizer"));
	}

	@Test
	@Deprecated(since = "4.0.0", forRemoval = true)
	void jackson2CodecCustomizerIsAutoConfiguredWhenObjectMapperIsPresentAndJacksonIsMissing() {
		this.contextRunner.withUserConfiguration(ObjectMapperConfiguration.class)
			.withClassLoader(new FilteredClassLoader(JsonMapper.class.getPackage().getName()))
			.run((context) -> assertThat(context).hasBean("jackson2CodecCustomizer"));
	}

	@Test
	@Deprecated(since = "4.0.0", forRemoval = true)
	void jackson2CodecCustomizerBacksOffWhenJackson2IsPreferredButThereIsNoObjectMapper() {
		this.contextRunner.withPropertyValues("spring.http.codecs.preferred-json-mapper=jackson2")
			.run((context) -> assertThat(context).doesNotHaveBean("jackson2CodecCustomizer"));
	}

	@Test
	void userProvidedCustomizerCanOverrideJacksonCodecCustomizer() {
		this.contextRunner.withUserConfiguration(JsonMapperConfiguration.class, CodecCustomizerConfiguration.class)
			.run((context) -> {
				List<CodecCustomizer> codecCustomizers = context.getBean(CodecCustomizers.class).codecCustomizers;
				assertThat(codecCustomizers).hasSize(3);
				assertThat(codecCustomizers.get(2)).isInstanceOf(TestCodecCustomizer.class);
			});
	}

	@Test
	void maxInMemorySizeEnforcedInDefaultCodecs() {
		this.contextRunner.withPropertyValues("spring.http.codecs.max-in-memory-size=1MB")
			.run((context) -> assertThat(defaultCodecs(context)).hasFieldOrPropertyWithValue("maxInMemorySize",
					1048576));
	}

	@Test
	void kotlinSerializationUsesLimitedPredicateWhenOtherJsonConverterIsAvailable() {
		this.contextRunner.withUserConfiguration(KotlinxJsonConfiguration.class).run((context) -> {
			KotlinSerializationJsonEncoder encoder = findEncoder(context, KotlinSerializationJsonEncoder.class);
			assertThat(encoder.canEncode(ResolvableType.forClass(Map.class), MediaType.APPLICATION_JSON)).isFalse();
		});
	}

	@Test
	void kotlinSerializationUsesUnrestrictedPredicateWhenNoOtherJsonConverterIsAvailable() {
		FilteredClassLoader classLoader = new FilteredClassLoader(JsonMapper.class.getPackage().getName(),
				ObjectMapper.class.getPackage().getName());
		this.contextRunner.withClassLoader(classLoader)
			.withUserConfiguration(KotlinxJsonConfiguration.class)
			.run((context) -> {
				KotlinSerializationJsonEncoder encoder = findEncoder(context, KotlinSerializationJsonEncoder.class);
				assertThat(encoder.canEncode(ResolvableType.forClass(Map.class), MediaType.APPLICATION_JSON)).isTrue();
			});
	}

	@SuppressWarnings("unchecked")
	private <T> T findEncoder(AssertableApplicationContext context, Class<T> encoderClass) {
		ServerCodecConfigurer configurer = ServerCodecConfigurer.create();
		context.getBeansOfType(CodecCustomizer.class).values().forEach((codec) -> codec.customize(configurer));
		return (T) configurer.getWriters()
			.stream()
			.filter((writer) -> writer instanceof EncoderHttpMessageWriter<?>)
			.map((writer) -> (EncoderHttpMessageWriter<?>) writer)
			.map(EncoderHttpMessageWriter::getEncoder)
			.filter((encoder) -> encoderClass.isAssignableFrom(encoder.getClass()))
			.findFirst()
			.orElseThrow();
	}

	private DefaultCodecs defaultCodecs(AssertableApplicationContext context) {
		CodecCustomizer customizer = context.getBean(CodecCustomizer.class);
		CodecConfigurer configurer = new DefaultClientCodecConfigurer();
		customizer.customize(configurer);
		return configurer.defaultCodecs();
	}

	@Configuration(proxyBeanMethods = false)
	static class JsonMapperConfiguration {

		@Bean
		JsonMapper jsonMapper() {
			return new JsonMapper();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ObjectMapperConfiguration {

		@Bean
		ObjectMapper objectMapper() {
			return new ObjectMapper();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class KotlinxJsonConfiguration {

		@Bean
		Json kotlinxJson() {
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
