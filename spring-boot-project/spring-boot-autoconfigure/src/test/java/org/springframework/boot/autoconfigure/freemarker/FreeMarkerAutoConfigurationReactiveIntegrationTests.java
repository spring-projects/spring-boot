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

package org.springframework.boot.autoconfigure.freemarker;

import java.io.StringWriter;
import java.util.Locale;

import org.junit.After;
import org.junit.Test;
import reactor.core.publisher.Mono;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.web.reactive.context.AnnotationConfigReactiveWebApplicationContext;
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

	private AnnotationConfigReactiveWebApplicationContext context = new AnnotationConfigReactiveWebApplicationContext();

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void defaultConfiguration() {
		registerAndRefreshContext();
		assertThat(this.context.getBean(FreeMarkerViewResolver.class)).isNotNull();
		assertThat(this.context.getBean(FreeMarkerConfigurer.class)).isNotNull();
		assertThat(this.context.getBean(FreeMarkerConfig.class)).isNotNull();
		assertThat(this.context.getBean(freemarker.template.Configuration.class))
				.isNotNull();
	}

	@Test
	public void defaultViewResolution() {
		registerAndRefreshContext();
		MockServerWebExchange exchange = render("home");
		String result = exchange.getResponse().getBodyAsString().block();
		assertThat(result).contains("home");
		assertThat(exchange.getResponse().getHeaders().getContentType())
				.isEqualTo(MediaType.TEXT_HTML);
	}

	@Test
	public void customPrefix() {
		registerAndRefreshContext("spring.freemarker.prefix:prefix/");
		MockServerWebExchange exchange = render("prefixed");
		String result = exchange.getResponse().getBodyAsString().block();
		assertThat(result).contains("prefixed");
	}

	@Test
	public void customSuffix() {
		registerAndRefreshContext("spring.freemarker.suffix:.freemarker");
		MockServerWebExchange exchange = render("suffixed");
		String result = exchange.getResponse().getBodyAsString().block();
		assertThat(result).contains("suffixed");
	}

	@Test
	public void customTemplateLoaderPath() {
		registerAndRefreshContext(
				"spring.freemarker.templateLoaderPath:classpath:/custom-templates/");
		MockServerWebExchange exchange = render("custom");
		String result = exchange.getResponse().getBodyAsString().block();
		assertThat(result).contains("custom");
	}

	@SuppressWarnings("deprecation")
	@Test
	public void customFreeMarkerSettings() {
		registerAndRefreshContext("spring.freemarker.settings.boolean_format:yup,nope");
		assertThat(this.context.getBean(FreeMarkerConfigurer.class).getConfiguration()
				.getSetting("boolean_format")).isEqualTo("yup,nope");
	}

	@Test
	public void renderTemplate() throws Exception {
		registerAndRefreshContext();
		FreeMarkerConfigurer freemarker = this.context
				.getBean(FreeMarkerConfigurer.class);
		StringWriter writer = new StringWriter();
		freemarker.getConfiguration().getTemplate("message.ftl").process(this, writer);
		assertThat(writer.toString()).contains("Hello World");
	}

	private void registerAndRefreshContext(String... env) {
		TestPropertyValues.of(env).applyTo(this.context);
		this.context.register(FreeMarkerAutoConfiguration.class);
		this.context.refresh();
	}

	public String getGreeting() {
		return "Hello World";
	}

	private MockServerWebExchange render(String viewName) {
		FreeMarkerViewResolver resolver = this.context
				.getBean(FreeMarkerViewResolver.class);
		Mono<View> view = resolver.resolveViewName(viewName, Locale.UK);
		MockServerWebExchange exchange = MockServerWebExchange
				.from(MockServerHttpRequest.get("/path"));
		view.flatMap((v) -> v.render(null, MediaType.TEXT_HTML, exchange)).block();
		return exchange;
	}

}
