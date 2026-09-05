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

package org.springframework.boot.webmvc.autoconfigure;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.testsupport.system.CapturedOutput;
import org.springframework.boot.testsupport.system.OutputCaptureExtension;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;

/**
 * Tests for {@link NoServletContextFailureAnalyzer}.
 *
 * @author xoruddl
 */
@ExtendWith(OutputCaptureExtension.class)
class NoServletContextFailureAnalyzerTests {

	@Test
	void missingServletContextWithEnableWebMvcIsAnalyzed(CapturedOutput output) {
		assertThatException().isThrownBy(
				() -> new SpringApplicationBuilder(EnableWebMvcConfiguration.class).web(WebApplicationType.NONE).run());
		assertThat(output).contains("@EnableWebMvc was found on the following configuration class")
			.contains(EnableWebMvcConfiguration.class.getName())
			.contains("Spring MVC infrastructure requires a ServletContext")
			.contains("Remove @EnableWebMvc")
			.contains("@SpringBootTest or @WebMvcTest");
	}

	@Test
	void missingServletContextWithoutEnableWebMvcIsNotAnalyzed() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
			context.register(ConfigurationWithoutEnableWebMvc.class);
			assertThat(analyze(context, "resourceHandlerMapping", "No ServletContext set")).isNull();
		}
	}

	@Test
	void missingServletContextForAnotherBeanIsNotAnalyzed() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
			context.register(EnableWebMvcConfiguration.class);
			assertThat(analyze(context, "anotherBean", "No ServletContext set")).isNull();
		}
	}

	@Test
	void resourceHandlerMappingFailureWithAnotherMessageIsNotAnalyzed() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
			context.register(EnableWebMvcConfiguration.class);
			assertThat(analyze(context, "resourceHandlerMapping", "Another failure")).isNull();
		}
	}

	private @Nullable FailureAnalysis analyze(AnnotationConfigApplicationContext context, String beanName,
			String message) {
		NoServletContextFailureAnalyzer analyzer = new NoServletContextFailureAnalyzer(context.getBeanFactory());
		BeanCreationException failure = new BeanCreationException(beanName, "Failed to create bean",
				new IllegalStateException(message));
		return analyzer.analyze(failure);
	}

	@Configuration(proxyBeanMethods = false)
	@EnableWebMvc
	static class EnableWebMvcConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	static class ConfigurationWithoutEnableWebMvc {

	}

}
