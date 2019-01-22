/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.autoconfigure.mustache;

import com.samskivert.mustache.Mustache;
import org.junit.Test;
import org.testcontainers.shaded.org.apache.commons.lang.StringUtils;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.web.reactive.context.AnnotationConfigReactiveWebApplicationContext;
import org.springframework.boot.web.servlet.view.MustacheViewResolver;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MustacheAutoConfiguration}.
 *
 * @author Brian Clozel
 * @author Artsiom Yudovin
 */
public class MustacheAutoConfigurationTests {

	private AnnotationConfigWebApplicationContext webContext;

	private AnnotationConfigReactiveWebApplicationContext reactiveWebContext;

	@Test
	public void registerBeansForServletApp() {
		loadWithServlet(null, "value", null);
		assertThat(this.webContext.getBeansOfType(Mustache.Compiler.class)).hasSize(1);
		assertThat(this.webContext.getBeansOfType(MustacheResourceTemplateLoader.class))
				.hasSize(1);
		assertThat(this.webContext.getBeansOfType(MustacheViewResolver.class)).hasSize(1);
		assertThat(this.webContext.getBeansOfType(Mustache.Formatter.class)).hasSize(1);
		assertThat(this.webContext.getBeansOfType(Mustache.Escaper.class)).hasSize(1);
		assertThat(this.webContext.getBean(Mustache.Compiler.class).nullValue)
				.isEqualTo("value");
	}

	@Test
	public void registerCompilerForServletApp() {
		loadWithServlet(CustomCompilerConfiguration.class, null, null);
		assertThat(this.webContext.getBeansOfType(MustacheResourceTemplateLoader.class))
				.hasSize(1);
		assertThat(this.webContext.getBeansOfType(MustacheViewResolver.class)).hasSize(1);
		assertThat(this.webContext.getBeansOfType(Mustache.Compiler.class)).hasSize(1);
		assertThat(this.webContext.getBean(Mustache.Compiler.class).standardsMode)
				.isTrue();
		assertThat(this.webContext.getBeansOfType(Mustache.Formatter.class)).hasSize(1);
		assertThat(this.webContext.getBeansOfType(Mustache.Escaper.class)).hasSize(1);
	}

	@Test
	public void registerBeansForReactiveApp() {
		loadWithReactive(null, null, "value");
		assertThat(this.reactiveWebContext.getBeansOfType(Mustache.Compiler.class))
				.hasSize(1);
		assertThat(this.reactiveWebContext
				.getBeansOfType(MustacheResourceTemplateLoader.class)).hasSize(1);
		assertThat(this.reactiveWebContext.getBeansOfType(MustacheViewResolver.class))
				.isEmpty();
		assertThat(this.reactiveWebContext.getBeansOfType(
				org.springframework.boot.web.reactive.result.view.MustacheViewResolver.class))
						.hasSize(1);
		assertThat(this.reactiveWebContext.getBeansOfType(Mustache.Formatter.class))
				.hasSize(1);
		assertThat(this.reactiveWebContext.getBeansOfType(Mustache.Escaper.class))
				.hasSize(1);
		assertThat(this.reactiveWebContext.getBean(Mustache.Compiler.class).nullValue)
				.isEqualTo("value");
	}

	@Test
	public void registerCompilerForReactiveApp() {
		loadWithReactive(CustomCompilerConfiguration.class, null, null);
		assertThat(this.reactiveWebContext.getBeansOfType(Mustache.Compiler.class))
				.hasSize(1);
		assertThat(this.reactiveWebContext
				.getBeansOfType(MustacheResourceTemplateLoader.class)).hasSize(1);
		assertThat(this.reactiveWebContext.getBeansOfType(MustacheViewResolver.class))
				.isEmpty();
		assertThat(this.reactiveWebContext.getBeansOfType(
				org.springframework.boot.web.reactive.result.view.MustacheViewResolver.class))
						.hasSize(1);
		assertThat(this.reactiveWebContext.getBean(Mustache.Compiler.class).standardsMode)
				.isTrue();
	}

	private void loadWithServlet(Class<?> config, String defaultValue, String nullValue) {
		this.webContext = new AnnotationConfigWebApplicationContext();
		TestPropertyValues.of("spring.mustache.prefix=classpath:/mustache-templates/")
				.applyTo(this.webContext);
		applyMustacheProperties(this.webContext, defaultValue, nullValue);
		applyHandlers(this.webContext, "formatter", "escaper");
		if (config != null) {
			this.webContext.register(config);
		}
		this.webContext.register(BaseConfiguration.class);
		this.webContext.refresh();
	}

	private void loadWithReactive(Class<?> config, String defaultValue,
			String nullValue) {
		this.reactiveWebContext = new AnnotationConfigReactiveWebApplicationContext();
		TestPropertyValues.of("spring.mustache.prefix=classpath:/mustache-templates/")
				.applyTo(this.reactiveWebContext);
		applyMustacheProperties(this.reactiveWebContext, defaultValue, nullValue);
		applyHandlers(this.reactiveWebContext, "formatter", "escaper");
		if (config != null) {
			this.reactiveWebContext.register(config);
		}
		this.reactiveWebContext.register(BaseConfiguration.class);
		this.reactiveWebContext.refresh();
	}

	private void applyMustacheProperties(ConfigurableApplicationContext context,
			String defaultValue, String nullValue) {
		if (StringUtils.isNotBlank(defaultValue)) {
			TestPropertyValues.of("spring.mustache.defaultValue=" + defaultValue)
					.applyTo(context);
		}

		if (StringUtils.isNotBlank(nullValue)) {
			TestPropertyValues.of("spring.mustache.nullValue=" + nullValue)
					.applyTo(context);
		}

	}

	private void applyHandlers(ConfigurableApplicationContext context, String formatter,
			String escaper) {
		if (StringUtils.isNotBlank(formatter)) {
			TestPropertyValues.of("spring.mustache.formatter.value=" + formatter)
					.applyTo(context);
		}

		if (StringUtils.isNotBlank(escaper)) {
			TestPropertyValues.of("spring.mustache.escaper.value=" + escaper)
					.applyTo(context);
		}
	}

	@Configuration
	@Import({ MustacheAutoConfiguration.class })
	protected static class BaseConfiguration {

	}

	@Configuration
	protected static class CustomCompilerConfiguration {

		@Bean
		public Mustache.Compiler compiler(
				Mustache.TemplateLoader mustacheTemplateLoader) {
			return Mustache.compiler().standardsMode(true)
					.withLoader(mustacheTemplateLoader);
		}

	}

}
