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

import java.lang.reflect.Method;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.http.HttpProperties;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.codec.CodecCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.http.codec.CodecConfigurer;
import org.springframework.http.codec.support.DefaultClientCodecConfigurer;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CodecsAutoConfiguration}.
 *
 * @author Madhura Bhave
 * @author Andy Wilkinson
 */
class CodecsAutoConfigurationTests {

	private WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(CodecsAutoConfiguration.class));

	@Test
	void autoConfigShouldProvideALoggingRequestDetailsCustomizer() {
		this.contextRunner.run((context) -> {
			CodecCustomizer customizer = context.getBean(CodecCustomizer.class);
			CodecConfigurer configurer = new DefaultClientCodecConfigurer();
			customizer.customize(configurer);
			assertThat(configurer.defaultCodecs()).hasFieldOrPropertyWithValue("enableLoggingRequestDetails", false);
		});

	}

	@Test
	void loggingRequestDetailsCustomizerShouldUseHttpProperties() {
		this.contextRunner.withPropertyValues("spring.http.log-request-details=true").run((context) -> {
			CodecCustomizer customizer = context.getBean(CodecCustomizer.class);
			CodecConfigurer configurer = new DefaultClientCodecConfigurer();
			customizer.customize(configurer);
			assertThat(configurer.defaultCodecs()).hasFieldOrPropertyWithValue("enableLoggingRequestDetails", true);
		});
	}

	@Test
	void loggingRequestDetailsBeanShouldHaveOrderZero() {
		this.contextRunner.run((context) -> {
			Method customizerMethod = ReflectionUtils.findMethod(
					CodecsAutoConfiguration.LoggingCodecConfiguration.class, "loggingCodecCustomizer",
					HttpProperties.class);
			Integer order = new TestAnnotationAwareOrderComparator().findOrder(customizerMethod);
			assertThat(order).isEqualTo(0);
		});
	}

	@Test
	void jacksonCodecCustomizerBacksOffWhenThereIsNoObjectMapper() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean("jacksonCodecCustomizer"));
	}

	@Test
	void jacksonCodecCustomizerIsAutoConfiguredWhenObjectMapperIsPresent() {
		this.contextRunner.withUserConfiguration(ObjectMapperConfiguration.class)
				.run((context) -> assertThat(context).hasBean("jacksonCodecCustomizer"));
	}

	@Test
	void userProvidedCustomizerCanOverrideJacksonCodecCustomizer() {
		this.contextRunner.withUserConfiguration(ObjectMapperConfiguration.class, CodecCustomizerConfiguration.class)
				.run((context) -> {
					List<CodecCustomizer> codecCustomizers = context.getBean(CodecCustomizers.class).codecCustomizers;
					assertThat(codecCustomizers).hasSize(3);
					assertThat(codecCustomizers.get(2)).isInstanceOf(TestCodecCustomizer.class);
				});
	}

	static class TestAnnotationAwareOrderComparator extends AnnotationAwareOrderComparator {

		@Override
		public Integer findOrder(Object obj) {
			return super.findOrder(obj);
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
