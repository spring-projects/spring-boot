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

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.http.codec.CodecCustomizer;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.Ordered;
import org.springframework.http.codec.CodecConfigurer;
import org.springframework.http.codec.CodecConfigurer.DefaultCodecs;
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
	void maxInMemorySizeEnforcedInDefaultCodecs() {
		this.contextRunner.withPropertyValues("spring.http.codecs.max-in-memory-size=1MB")
			.run((context) -> assertThat(defaultCodecs(context)).hasFieldOrPropertyWithValue("maxInMemorySize",
					1048576));
	}

	private DefaultCodecs defaultCodecs(AssertableApplicationContext context) {
		CodecCustomizer customizer = context.getBean(CodecCustomizer.class);
		CodecConfigurer configurer = new DefaultClientCodecConfigurer();
		customizer.customize(configurer);
		return configurer.defaultCodecs();
	}

}
