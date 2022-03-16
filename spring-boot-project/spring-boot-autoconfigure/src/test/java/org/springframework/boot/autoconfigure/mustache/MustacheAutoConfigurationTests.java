/*
 * Copyright 2012-2022 the original author or authors.
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

import com.samskivert.mustache.Mustache;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.AbstractApplicationContextRunner;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.view.MustacheViewResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
	void registerCompilerForReactiveApp() {
		configure(new ReactiveWebApplicationContextRunner()).withUserConfiguration(CustomCompilerConfiguration.class)
				.run((context) -> {
					assertThat(context).hasSingleBean(Mustache.Compiler.class);
					assertThat(context).hasSingleBean(MustacheResourceTemplateLoader.class);
					assertThat(context).doesNotHaveBean(MustacheViewResolver.class);
					assertThat(context).hasSingleBean(
							org.springframework.boot.web.reactive.result.view.MustacheViewResolver.class);
					assertThat(context.getBean(Mustache.Compiler.class).standardsMode).isTrue();
				});
	}

	private <T extends AbstractApplicationContextRunner<T, ?, ?>> T configure(T runner) {
		return runner.withPropertyValues("spring.mustache.prefix=classpath:/mustache-templates/")
				.withConfiguration(AutoConfigurations.of(MustacheAutoConfiguration.class));
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomCompilerConfiguration {

		@Bean
		Mustache.Compiler compiler(Mustache.TemplateLoader mustacheTemplateLoader) {
			return Mustache.compiler().standardsMode(true).withLoader(mustacheTemplateLoader);
		}

	}

}
