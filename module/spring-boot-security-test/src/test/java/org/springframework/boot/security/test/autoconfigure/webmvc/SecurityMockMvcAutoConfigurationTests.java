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

package org.springframework.boot.security.test.autoconfigure.webmvc;

import java.util.concurrent.Executor;
import java.util.function.Function;

import jakarta.servlet.Filter;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.security.test.autoconfigure.webmvc.SecurityMockMvcAutoConfiguration.SecurityMockMvcBuilderCustomizer;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcHtmlUnitDriverCustomizer;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutor;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.web.context.AbstractSecurityWebApplicationInitializer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.ConfigurableMockMvcBuilder;
import org.springframework.test.web.servlet.setup.MockMvcConfigurer;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link SecurityMockMvcAutoConfiguration}.
 *
 * @author Dmytro Nosan
 */
class SecurityMockMvcAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(SecurityMockMvcAutoConfiguration.class));

	@Test
	void securityMockMvcBuilderCustomizerIsNotRegisteredWhenMockMvcIsNotOnTheClasspath() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(MockMvc.class))
			.with(securityFilterChain())
			.run((context) -> assertThat(context).doesNotHaveBean(MockMvcHtmlUnitDriverCustomizer.class)
				.doesNotHaveBean(SecurityMockMvcBuilderCustomizer.class));
	}

	@Test
	void securityMockMvcBuilderCustomizerIsNotRegisteredWhenSecurityFilterChainIsMissing() {
		this.contextRunner
			.run((context) -> assertThat(context).doesNotHaveBean(SecurityMockMvcBuilderCustomizer.class));
	}

	@Test
	void htmlUnitDriverCustomizerIsNotRegisteredWhenHtmlUnitIsNotOnTheClasspath() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(HtmlUnitDriver.class))
			.run((context) -> assertThat(context).doesNotHaveBean(MockMvcHtmlUnitDriverCustomizer.class));
	}

	@Test
	void registersSecurityMockMvcBuilderCustomizerWhenSecurityFilterChainIsPresent() {
		this.contextRunner.with(securityFilterChain())
			.run((context) -> assertThat(context).hasSingleBean(SecurityMockMvcBuilderCustomizer.class)
				.getBean(SecurityMockMvcBuilderCustomizer.class)
				.satisfies((customizer) -> {
					ConfigurableMockMvcBuilder<?> builder = mock(ConfigurableMockMvcBuilder.class);
					customizer.customize(builder);
					ArgumentCaptor<MockMvcConfigurer> configurerCaptor = ArgumentCaptor
						.forClass(MockMvcConfigurer.class);
					then(builder).should().apply(configurerCaptor.capture());
					RequestPostProcessor postProcessor = configurerCaptor.getValue()
						.beforeMockMvcCreated(builder, mock(WebApplicationContext.class));
					assertThat(postProcessor)
						.hasSameClassAs(SecurityMockMvcRequestPostProcessors.testSecurityContext());
				}));
	}

	@Test
	void registersSecurityMockMvcHtmlUnitDriverCustomizerWhenHtmlUnitIsPresent() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(MockMvcHtmlUnitDriverCustomizer.class)
			.getBean(MockMvcHtmlUnitDriverCustomizer.class)
			.satisfies((customizer) -> {
				HtmlUnitDriver htmlUnitDriver = mock(HtmlUnitDriver.class);
				customizer.customize(htmlUnitDriver);
				ArgumentCaptor<Executor> executorCaptor = ArgumentCaptor.forClass(Executor.class);
				then(htmlUnitDriver).should().setExecutor(executorCaptor.capture());
				assertThat(executorCaptor.getValue()).isInstanceOf(DelegatingSecurityContextExecutor.class);
			}));
	}

	private Function<ApplicationContextRunner, ApplicationContextRunner> securityFilterChain() {
		return (contextRunner) -> contextRunner.withBean(AbstractSecurityWebApplicationInitializer.DEFAULT_FILTER_NAME,
				Filter.class, () -> mock(Filter.class));
	}

}
