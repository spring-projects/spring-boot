/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.web.servlet;

import java.util.Optional;

import org.junit.Test;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for {@link WelcomePageHandlerMapping}.
 *
 * @author Andy Wilkinson
 */
public class WelcomePageHandlerMappingTests {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
			.withUserConfiguration(HandlerMappingConfiguration.class).withConfiguration(
					AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class));

	@Test
	public void handlesRequestForStaticPageThatAcceptsTextHtml() {
		this.contextRunner.withUserConfiguration(StaticResourceConfiguration.class)
				.run((context) -> MockMvcBuilders.webAppContextSetup(context).build()
						.perform(get("/").accept(MediaType.TEXT_HTML))
						.andExpect(status().isOk())
						.andExpect(forwardedUrl("index.html")));
	}

	@Test
	public void handlesRequestForStaticPageThatAcceptsAll() {
		this.contextRunner.withUserConfiguration(StaticResourceConfiguration.class)
				.run((context) -> MockMvcBuilders.webAppContextSetup(context).build()
						.perform(get("/").accept("*/*")).andExpect(status().isOk())
						.andExpect(forwardedUrl("index.html")));
	}

	@Test
	public void doesNotHandleRequestThatDoesNotAcceptTextHtml() {
		this.contextRunner.withUserConfiguration(StaticResourceConfiguration.class)
				.run((context) -> MockMvcBuilders.webAppContextSetup(context).build()
						.perform(get("/").accept(MediaType.APPLICATION_JSON))
						.andExpect(status().isNotFound()));
	}

	@Test
	public void handlesRequestWithNoAcceptHeader() {
		this.contextRunner.withUserConfiguration(StaticResourceConfiguration.class)
				.run((context) -> MockMvcBuilders.webAppContextSetup(context).build()
						.perform(get("/")).andExpect(status().isOk())
						.andExpect(forwardedUrl("index.html")));
	}

	@Test
	public void handlesRequestWithEmptyAcceptHeader() throws Exception {
		this.contextRunner.withUserConfiguration(StaticResourceConfiguration.class)
				.run((context) -> MockMvcBuilders.webAppContextSetup(context).build()
						.perform(get("/").header(HttpHeaders.ACCEPT, ""))
						.andExpect(status().isOk())
						.andExpect(forwardedUrl("index.html")));

	}

	@Test
	public void rootHandlerIsNotRegisteredWhenStaticPathPatternIsNotSlashStarStar() {
		this.contextRunner.withUserConfiguration(StaticResourceConfiguration.class)
				.withPropertyValues("static-path-pattern=/foo/**")
				.run((context) -> assertThat(
						context.getBean(WelcomePageHandlerMapping.class).getRootHandler())
								.isNull());
	}

	@Test
	public void producesNotFoundResponseWhenThereIsNoWelcomePage() {
		this.contextRunner.run((context) -> MockMvcBuilders.webAppContextSetup(context)
				.build().perform(get("/").accept(MediaType.TEXT_HTML))
				.andExpect(status().isNotFound()));
	}

	@Configuration
	static class HandlerMappingConfiguration {

		@Bean
		public WelcomePageHandlerMapping handlerMapping(
				ApplicationContext applicationContext,
				ObjectProvider<Resource> staticIndexPage,
				@Value("${static-path-pattern:/**}") String staticPathPattern) {
			return new WelcomePageHandlerMapping(
					Optional.ofNullable(staticIndexPage.getIfAvailable()),
					staticPathPattern);
		}

	}

	@Configuration
	static class StaticResourceConfiguration {

		@Bean
		public Resource staticIndexPage() {
			return new FileSystemResource("src/test/resources/welcome-page/index.html");
		}

	}

}
