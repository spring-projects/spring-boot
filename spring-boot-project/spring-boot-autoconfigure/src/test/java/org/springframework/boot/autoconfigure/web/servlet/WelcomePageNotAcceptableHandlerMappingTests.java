/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.autoconfigure.web.servlet;

import org.assertj.core.api.ThrowingConsumer;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.template.TemplateAvailabilityProviders;
import org.springframework.boot.test.context.assertj.AssertableWebApplicationContext;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WelcomePageNotAcceptableHandlerMapping}.
 *
 * @author Phillip Webb
 */
class WelcomePageNotAcceptableHandlerMappingTests {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
		.withUserConfiguration(HandlerMappingConfiguration.class)
		.withConfiguration(AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class));

	@Test
	void isOrderedAtLowPriorityButAboveResourceHandlerRegistry() {
		this.contextRunner.withUserConfiguration(StaticResourceConfiguration.class).run((context) -> {
			WelcomePageNotAcceptableHandlerMapping handler = context
				.getBean(WelcomePageNotAcceptableHandlerMapping.class);
			ResourceHandlerRegistry registry = new ResourceHandlerRegistry(context, null);
			Integer resourceOrder = (Integer) ReflectionTestUtils.getField(registry, "order");
			assertThat(handler.getOrder()).isEqualTo(Ordered.LOWEST_PRECEDENCE - 10);
			assertThat(handler.getOrder()).isLessThan(resourceOrder);
		});
	}

	@Test
	void handlesRequestForStaticPageThatAcceptsTextHtml() {
		this.contextRunner.withUserConfiguration(StaticResourceConfiguration.class)
			.run(testWith((mvc) -> assertThat(mvc.get().uri("/").accept(MediaType.TEXT_HTML))
				.hasStatus(HttpStatus.NOT_ACCEPTABLE)));
	}

	@Test
	void handlesRequestForStaticPageThatDoesNotAcceptTextHtml() {
		this.contextRunner.withUserConfiguration(StaticResourceConfiguration.class)
			.run(testWith((mvc) -> assertThat(mvc.get().uri("/").accept(MediaType.APPLICATION_JSON))
				.hasStatus(HttpStatus.NOT_ACCEPTABLE)));
	}

	@Test
	void handlesRequestWithNoAcceptHeader() {
		this.contextRunner.withUserConfiguration(StaticResourceConfiguration.class)
			.run(testWith((mvc) -> assertThat(mvc.get().uri("/")).hasStatus(HttpStatus.NOT_ACCEPTABLE)));
	}

	@Test
	void handlesRequestWithEmptyAcceptHeader() {
		this.contextRunner.withUserConfiguration(StaticResourceConfiguration.class)
			.run(testWith((mvc) -> assertThat(mvc.get().uri("/").header(HttpHeaders.ACCEPT, ""))
				.hasStatus(HttpStatus.NOT_ACCEPTABLE)));
	}

	@Test
	void rootHandlerIsNotRegisteredWhenStaticPathPatternIsNotSlashStarStar() {
		this.contextRunner.withUserConfiguration(StaticResourceConfiguration.class)
			.withPropertyValues("static-path-pattern=/foo/**")
			.run((context) -> assertThat(context.getBean(WelcomePageNotAcceptableHandlerMapping.class).getRootHandler())
				.isNull());
	}

	@Test
	void producesNotFoundResponseWhenThereIsNoWelcomePage() {
		this.contextRunner.run(testWith(
				(mvc) -> assertThat(mvc.get().uri("/").accept(MediaType.TEXT_HTML)).hasStatus(HttpStatus.NOT_FOUND)));
	}

	private ContextConsumer<AssertableWebApplicationContext> testWith(ThrowingConsumer<MockMvcTester> mvc) {
		return (context) -> mvc.accept(MockMvcTester.from(context));
	}

	@Configuration(proxyBeanMethods = false)
	static class HandlerMappingConfiguration {

		@Bean
		WelcomePageNotAcceptableHandlerMapping handlerMapping(ApplicationContext applicationContext,
				ObjectProvider<TemplateAvailabilityProviders> templateAvailabilityProviders,
				ObjectProvider<Resource> staticIndexPage,
				@Value("${static-path-pattern:/**}") String staticPathPattern) {
			return new WelcomePageNotAcceptableHandlerMapping(
					templateAvailabilityProviders
						.getIfAvailable(() -> new TemplateAvailabilityProviders(applicationContext)),
					applicationContext, staticIndexPage.getIfAvailable(), staticPathPattern);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class StaticResourceConfiguration {

		@Bean
		Resource staticIndexPage() {
			return new FileSystemResource("src/test/resources/welcome-page/index.html");
		}

	}

}
