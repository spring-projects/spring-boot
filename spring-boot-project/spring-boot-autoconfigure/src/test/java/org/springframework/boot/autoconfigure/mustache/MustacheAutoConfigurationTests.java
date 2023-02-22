/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.autoconfigure.mustache;

import java.util.Arrays;
import java.util.function.Supplier;

import com.samskivert.mustache.Mustache;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.AbstractApplicationContextRunner;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.view.MustacheViewResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MustacheAutoConfiguration}.
 *
 * @author Brian Clozel
 * @author Andy Wilkinson
 */
class MustacheAutoConfigurationTests {

	@Test
	void registerBeansForServletApp() {
		configure(new WebApplicationContextRunner()).run((context) -> {
			assertThat(context).hasSingleBean(Mustache.Compiler.class);
			assertThat(context).hasSingleBean(MustacheResourceTemplateLoader.class);
			assertThat(context).hasSingleBean(MustacheViewResolver.class);
		});
	}

	@Test
	void servletViewResolverCanBeDisabled() {
		configure(new WebApplicationContextRunner()).withPropertyValues("spring.mustache.enabled=false")
			.run((context) -> {
				assertThat(context).hasSingleBean(Mustache.Compiler.class);
				assertThat(context).hasSingleBean(MustacheResourceTemplateLoader.class);
				assertThat(context).doesNotHaveBean(MustacheViewResolver.class);
			});
	}

	@Test
	void registerCompilerForServletApp() {
		configure(new WebApplicationContextRunner()).withUserConfiguration(CustomCompilerConfiguration.class)
			.run((context) -> {
				assertThat(context).hasSingleBean(Mustache.Compiler.class);
				assertThat(context).hasSingleBean(MustacheResourceTemplateLoader.class);
				assertThat(context).hasSingleBean(MustacheViewResolver.class);
				assertThat(context.getBean(Mustache.Compiler.class).standardsMode).isTrue();
			});
	}

	@Test
	void registerBeansForReactiveApp() {
		configure(new ReactiveWebApplicationContextRunner()).run((context) -> {
			assertThat(context).hasSingleBean(Mustache.Compiler.class);
			assertThat(context).hasSingleBean(MustacheResourceTemplateLoader.class);
			assertThat(context).doesNotHaveBean(MustacheViewResolver.class);
			assertThat(context)
				.hasSingleBean(org.springframework.boot.web.reactive.result.view.MustacheViewResolver.class);
		});
	}

	@Test
	void reactiveViewResolverCanBeDisabled() {
		configure(new ReactiveWebApplicationContextRunner()).withPropertyValues("spring.mustache.enabled=false")
			.run((context) -> {
				assertThat(context).hasSingleBean(Mustache.Compiler.class);
				assertThat(context).hasSingleBean(MustacheResourceTemplateLoader.class);
				assertThat(context)
					.doesNotHaveBean(org.springframework.boot.web.reactive.result.view.MustacheViewResolver.class);
			});
	}

	@Test
	void registerCompilerForReactiveApp() {
		configure(new ReactiveWebApplicationContextRunner()).withUserConfiguration(CustomCompilerConfiguration.class)
			.run((context) -> {
				assertThat(context).hasSingleBean(Mustache.Compiler.class);
				assertThat(context).hasSingleBean(MustacheResourceTemplateLoader.class);
				assertThat(context).doesNotHaveBean(MustacheViewResolver.class);
				assertThat(context)
					.hasSingleBean(org.springframework.boot.web.reactive.result.view.MustacheViewResolver.class);
				assertThat(context.getBean(Mustache.Compiler.class).standardsMode).isTrue();
			});
	}

	@Test
	void defaultServletViewResolverConfiguration() {
		configure(new WebApplicationContextRunner()).run((context) -> {
			MustacheViewResolver viewResolver = context.getBean(MustacheViewResolver.class);
			assertThat(viewResolver).extracting("allowRequestOverride", InstanceOfAssertFactories.BOOLEAN).isFalse();
			assertThat(viewResolver).extracting("allowSessionOverride", InstanceOfAssertFactories.BOOLEAN).isFalse();
			assertThat(viewResolver).extracting("cache", InstanceOfAssertFactories.BOOLEAN).isFalse();
			assertThat(viewResolver).extracting("charset").isEqualTo("UTF-8");
			assertThat(viewResolver).extracting("exposeRequestAttributes", InstanceOfAssertFactories.BOOLEAN).isFalse();
			assertThat(viewResolver).extracting("exposeSessionAttributes", InstanceOfAssertFactories.BOOLEAN).isFalse();
			assertThat(viewResolver).extracting("exposeSpringMacroHelpers", InstanceOfAssertFactories.BOOLEAN).isTrue();
			assertThat(viewResolver).extracting("prefix").isEqualTo("classpath:/templates/");
			assertThat(viewResolver).extracting("requestContextAttribute").isNull();
			assertThat(viewResolver).extracting("suffix").isEqualTo(".mustache");
		});
	}

	@Test
	void defaultReactiveViewResolverConfiguration() {
		configure(new ReactiveWebApplicationContextRunner()).run((context) -> {
			org.springframework.boot.web.reactive.result.view.MustacheViewResolver viewResolver = context
				.getBean(org.springframework.boot.web.reactive.result.view.MustacheViewResolver.class);
			assertThat(viewResolver).extracting("charset").isEqualTo("UTF-8");
			assertThat(viewResolver).extracting("prefix").isEqualTo("classpath:/templates/");
			assertThat(viewResolver).extracting("requestContextAttribute").isNull();
			assertThat(viewResolver).extracting("suffix").isEqualTo(".mustache");
			assertThat(viewResolver.getSupportedMediaTypes())
				.containsExactly(MediaType.parseMediaType("text/html;charset=UTF-8"));
		});
	}

	@Test
	void allowRequestOverrideCanBeCustomizedOnServletViewResolver() {
		assertViewResolverProperty(ViewResolverKind.SERVLET, "spring.mustache.servlet.allow-request-override=true",
				"allowRequestOverride", true);
	}

	@Test
	void allowSessionOverrideCanBeCustomizedOnServletViewResolver() {
		assertViewResolverProperty(ViewResolverKind.SERVLET, "spring.mustache.servlet.allow-session-override=true",
				"allowSessionOverride", true);
	}

	@Test
	void cacheCanBeCustomizedOnServletViewResolver() {
		assertViewResolverProperty(ViewResolverKind.SERVLET, "spring.mustache.servlet.cache=true", "cache", true);
	}

	@ParameterizedTest
	@EnumSource(ViewResolverKind.class)
	void charsetCanBeCustomizedOnViewResolver(ViewResolverKind kind) {
		assertViewResolverProperty(kind, "spring.mustache.charset=UTF-16", "charset", "UTF-16");
	}

	@Test
	void exposeRequestAttributesCanBeCustomizedOnServletViewResolver() {
		assertViewResolverProperty(ViewResolverKind.SERVLET, "spring.mustache.servlet.expose-request-attributes=true",
				"exposeRequestAttributes", true);
	}

	@Test
	void exposeSessionAttributesCanBeCustomizedOnServletViewResolver() {
		assertViewResolverProperty(ViewResolverKind.SERVLET, "spring.mustache.servlet.expose-session-attributes=true",
				"exposeSessionAttributes", true);
	}

	@Test
	void exposeSpringMacroHelpersCanBeCustomizedOnServletViewResolver() {
		assertViewResolverProperty(ViewResolverKind.SERVLET, "spring.mustache.servlet.expose-spring-macro-helpers=true",
				"exposeSpringMacroHelpers", true);
	}

	@ParameterizedTest
	@EnumSource(ViewResolverKind.class)
	void prefixCanBeCustomizedOnViewResolver(ViewResolverKind kind) {
		assertViewResolverProperty(kind, "spring.mustache.prefix=classpath:/mustache-templates/", "prefix",
				"classpath:/mustache-templates/");
	}

	@ParameterizedTest
	@EnumSource(ViewResolverKind.class)
	void requestContextAttributeCanBeCustomizedOnViewResolver(ViewResolverKind kind) {
		assertViewResolverProperty(kind, "spring.mustache.request-context-attribute=test", "requestContextAttribute",
				"test");
	}

	@ParameterizedTest
	@EnumSource(ViewResolverKind.class)
	void suffixCanBeCustomizedOnViewResolver(ViewResolverKind kind) {
		assertViewResolverProperty(kind, "spring.mustache.suffix=.tache", "suffix", ".tache");
	}

	@Test
	void mediaTypesCanBeCustomizedOnReactiveViewResolver() {
		assertViewResolverProperty(ViewResolverKind.REACTIVE,
				"spring.mustache.reactive.media-types=text/xml;charset=UTF-8,text/plain;charset=UTF-16", "mediaTypes",
				Arrays.asList(MediaType.parseMediaType("text/xml;charset=UTF-8"),
						MediaType.parseMediaType("text/plain;charset=UTF-16")));
	}

	private void assertViewResolverProperty(ViewResolverKind kind, String property, String field,
			Object expectedValue) {
		kind.runner()
			.withConfiguration(AutoConfigurations.of(MustacheAutoConfiguration.class))
			.withPropertyValues(property)
			.run((context) -> assertThat(context.getBean(kind.viewResolverClass())).extracting(field)
				.isEqualTo(expectedValue));
	}

	private <T extends AbstractApplicationContextRunner<T, ?, ?>> T configure(T runner) {
		return runner.withConfiguration(AutoConfigurations.of(MustacheAutoConfiguration.class));
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomCompilerConfiguration {

		@Bean
		Mustache.Compiler compiler(Mustache.TemplateLoader mustacheTemplateLoader) {
			return Mustache.compiler().standardsMode(true).withLoader(mustacheTemplateLoader);
		}

	}

	private enum ViewResolverKind {

		/**
		 * Servlet MustacheViewResolver
		 */
		SERVLET(WebApplicationContextRunner::new, MustacheViewResolver.class),

		/**
		 * Reactive MustacheViewResolver
		 */
		REACTIVE(ReactiveWebApplicationContextRunner::new,
				org.springframework.boot.web.reactive.result.view.MustacheViewResolver.class);

		private final Supplier<AbstractApplicationContextRunner<?, ?, ?>> runner;

		private final Class<?> viewResolverClass;

		ViewResolverKind(Supplier<AbstractApplicationContextRunner<?, ?, ?>> runner, Class<?> viewResolverClass) {
			this.runner = runner;
			this.viewResolverClass = viewResolverClass;
		}

		private AbstractApplicationContextRunner<?, ?, ?> runner() {
			return this.runner.get();
		}

		private Class<?> viewResolverClass() {
			return this.viewResolverClass;
		}

	}

}
