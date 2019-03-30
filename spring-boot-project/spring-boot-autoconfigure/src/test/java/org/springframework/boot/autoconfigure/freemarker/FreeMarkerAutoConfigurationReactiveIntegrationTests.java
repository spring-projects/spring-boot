/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.autoconfigure.freemarker;

import java.io.StringWriter;
import java.time.Duration;
import java.util.Locale;

import org.junit.Test;
import reactor.core.publisher.Mono;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.reactive.result.view.View;
import org.springframework.web.reactive.result.view.freemarker.FreeMarkerConfig;
import org.springframework.web.reactive.result.view.freemarker.FreeMarkerConfigurer;
import org.springframework.web.reactive.result.view.freemarker.FreeMarkerViewResolver;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link FreeMarkerAutoConfiguration} Reactive support.
 *
 * @author Brian Clozel
 */
public class FreeMarkerAutoConfigurationReactiveIntegrationTests {

	private final ReactiveWebApplicationContextRunner contextRunner = new ReactiveWebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(FreeMarkerAutoConfiguration.class));

	@Test
	public void defaultConfiguration() {
		this.contextRunner.run((context) -> {
			assertThat(context.getBean(FreeMarkerViewResolver.class)).isNotNull();
			assertThat(context.getBean(FreeMarkerConfigurer.class)).isNotNull();
			assertThat(context.getBean(FreeMarkerConfig.class)).isNotNull();
			assertThat(context.getBean(freemarker.template.Configuration.class))
					.isNotNull();
		});
	}

	@Test
	public void defaultViewResolution() {
		this.contextRunner.run((context) -> {
			MockServerWebExchange exchange = render(context, "home");
			String result = exchange.getResponse().getBodyAsString()
					.block(Duration.ofSeconds(30));
			assertThat(result).contains("home");
			assertThat(exchange.getResponse().getHeaders().getContentType())
					.isEqualTo(MediaType.TEXT_HTML);
		});
	}

	@Test
	public void customPrefix() {
		this.contextRunner.withPropertyValues("spring.freemarker.prefix:prefix/")
				.run((context) -> {
					MockServerWebExchange exchange = render(context, "prefixed");
					String result = exchange.getResponse().getBodyAsString()
							.block(Duration.ofSeconds(30));
					assertThat(result).contains("prefixed");
				});
	}

	@Test
	public void customSuffix() {
		this.contextRunner.withPropertyValues("spring.freemarker.suffix:.freemarker")
				.run((context) -> {
					MockServerWebExchange exchange = render(context, "suffixed");
					String result = exchange.getResponse().getBodyAsString()
							.block(Duration.ofSeconds(30));
					assertThat(result).contains("suffixed");
				});
	}

	@Test
	public void customTemplateLoaderPath() {
		this.contextRunner.withPropertyValues(
				"spring.freemarker.templateLoaderPath:classpath:/custom-templates/")
				.run((context) -> {
					MockServerWebExchange exchange = render(context, "custom");
					String result = exchange.getResponse().getBodyAsString()
							.block(Duration.ofSeconds(30));
					assertThat(result).contains("custom");
				});
	}

	@SuppressWarnings("deprecation")
	@Test
	public void customFreeMarkerSettings() {
		this.contextRunner
				.withPropertyValues("spring.freemarker.settings.boolean_format:yup,nope")
				.run((context) -> assertThat(context.getBean(FreeMarkerConfigurer.class)
						.getConfiguration().getSetting("boolean_format"))
								.isEqualTo("yup,nope"));
	}

	@Test
	public void renderTemplate() {
		this.contextRunner.withPropertyValues().run((context) -> {
			FreeMarkerConfigurer freemarker = context.getBean(FreeMarkerConfigurer.class);
			StringWriter writer = new StringWriter();
			freemarker.getConfiguration().getTemplate("message.ftl").process(this,
					writer);
			assertThat(writer.toString()).contains("Hello World");
		});
	}

	public String getGreeting() {
		return "Hello World";
	}

	private MockServerWebExchange render(ApplicationContext context, String viewName) {
		FreeMarkerViewResolver resolver = context.getBean(FreeMarkerViewResolver.class);
		Mono<View> view = resolver.resolveViewName(viewName, Locale.UK);
		MockServerWebExchange exchange = MockServerWebExchange
				.from(MockServerHttpRequest.get("/path"));
		view.flatMap((v) -> v.render(null, MediaType.TEXT_HTML, exchange))
				.block(Duration.ofSeconds(30));
		return exchange;
	}

}
