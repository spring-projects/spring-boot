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

package org.springframework.boot.autoconfigure.thymeleaf;

import java.io.File;
import java.util.Collections;
import java.util.Locale;

import nz.net.ultraq.thymeleaf.LayoutDialect;
import nz.net.ultraq.thymeleaf.decorators.strategies.GroupingStrategy;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.ISpringWebFluxTemplateEngine;
import org.thymeleaf.spring5.SpringWebFluxTemplateEngine;
import org.thymeleaf.spring5.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring5.view.reactive.ThymeleafReactiveViewResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.rule.OutputCapture;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.web.reactive.context.AnnotationConfigReactiveWebApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

/**
 * Tests for {@link ThymeleafAutoConfiguration} in Reactive applications.
 *
 * @author Brian Clozel
 * @author Kazuki Shimizu
 */
public class ThymeleafReactiveAutoConfigurationTests {

	@Rule
	public OutputCapture output = new OutputCapture();

	private AnnotationConfigReactiveWebApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void createFromConfigClass() {
		load(BaseConfiguration.class, "spring.thymeleaf.suffix:.html");
		TemplateEngine engine = this.context.getBean(TemplateEngine.class);
		Context attrs = new Context(Locale.UK, Collections.singletonMap("foo", "bar"));
		String result = engine.process("template", attrs);
		assertThat(result).isEqualTo("<html>bar</html>");
	}

	@Test
	public void overrideCharacterEncoding() {
		load(BaseConfiguration.class, "spring.thymeleaf.encoding:UTF-16");
		ITemplateResolver resolver = this.context.getBean(ITemplateResolver.class);
		assertThat(resolver instanceof SpringResourceTemplateResolver).isTrue();
		assertThat(((SpringResourceTemplateResolver) resolver).getCharacterEncoding())
				.isEqualTo("UTF-16");
		ThymeleafReactiveViewResolver views = this.context
				.getBean(ThymeleafReactiveViewResolver.class);
		assertThat(views.getDefaultCharset().name()).isEqualTo("UTF-16");
	}

	@Test
	public void overrideMediaTypes() {
		load(BaseConfiguration.class,
				"spring.thymeleaf.reactive.media-types:text/html,text/plain");
		ThymeleafReactiveViewResolver views = this.context
				.getBean(ThymeleafReactiveViewResolver.class);
		assertThat(views.getSupportedMediaTypes()).contains(MediaType.TEXT_HTML,
				MediaType.TEXT_PLAIN);
	}

	@Test
	public void overrideTemplateResolverOrder() {
		load(BaseConfiguration.class, "spring.thymeleaf.templateResolverOrder:25");
		ITemplateResolver resolver = this.context.getBean(ITemplateResolver.class);
		assertThat(resolver.getOrder()).isEqualTo(Integer.valueOf(25));
	}

	@Test
	public void overrideViewNames() {
		load(BaseConfiguration.class, "spring.thymeleaf.viewNames:foo,bar");
		ThymeleafReactiveViewResolver views = this.context
				.getBean(ThymeleafReactiveViewResolver.class);
		assertThat(views.getViewNames()).isEqualTo(new String[] { "foo", "bar" });
	}

	@Test
	public void overrideMaxChunkSize() {
		load(BaseConfiguration.class, "spring.thymeleaf.reactive.maxChunkSize:8192");
		ThymeleafReactiveViewResolver views = this.context
				.getBean(ThymeleafReactiveViewResolver.class);
		assertThat(views.getResponseMaxChunkSizeBytes()).isEqualTo(Integer.valueOf(8192));
	}

	@Test
	public void overrideFullModeViewNames() {
		load(BaseConfiguration.class,
				"spring.thymeleaf.reactive.fullModeViewNames:foo,bar");
		ThymeleafReactiveViewResolver views = this.context
				.getBean(ThymeleafReactiveViewResolver.class);
		assertThat(views.getFullModeViewNames()).isEqualTo(new String[] { "foo", "bar" });
	}

	@Test
	public void overrideChunkedModeViewNames() {
		load(BaseConfiguration.class,
				"spring.thymeleaf.reactive.chunkedModeViewNames:foo,bar");
		ThymeleafReactiveViewResolver views = this.context
				.getBean(ThymeleafReactiveViewResolver.class);
		assertThat(views.getChunkedModeViewNames())
				.isEqualTo(new String[] { "foo", "bar" });
	}

	@Test
	public void overrideEnableSpringElCompiler() {
		load(BaseConfiguration.class, "spring.thymeleaf.enable-spring-el-compiler:true");
		assertThat(this.context.getBean(SpringWebFluxTemplateEngine.class)
				.getEnableSpringELCompiler()).isTrue();
	}

	@Test
	public void enableSpringElCompilerIsDisabledByDefault() {
		load(BaseConfiguration.class);
		assertThat(this.context.getBean(SpringWebFluxTemplateEngine.class)
				.getEnableSpringELCompiler()).isFalse();
	}

	@Test
	public void templateLocationDoesNotExist() {
		load(BaseConfiguration.class,
				"spring.thymeleaf.prefix:classpath:/no-such-directory/");
		this.output.expect(containsString("Cannot find template location"));
	}

	@Test
	public void templateLocationEmpty() {
		new File("target/test-classes/templates/empty-directory").mkdir();
		load(BaseConfiguration.class,
				"spring.thymeleaf.prefix:classpath:/templates/empty-directory/");
		this.output.expect(not(containsString("Cannot find template location")));
	}

	@Test
	public void useDataDialect() {
		load(BaseConfiguration.class);
		ISpringWebFluxTemplateEngine engine = this.context
				.getBean(ISpringWebFluxTemplateEngine.class);
		Context attrs = new Context(Locale.UK, Collections.singletonMap("foo", "bar"));
		String result = engine.process("data-dialect", attrs);
		assertThat(result).isEqualTo("<html><body data-foo=\"bar\"></body></html>");
	}

	@Test
	public void useJava8TimeDialect() {
		load(BaseConfiguration.class);
		ISpringWebFluxTemplateEngine engine = this.context
				.getBean(ISpringWebFluxTemplateEngine.class);
		Context attrs = new Context(Locale.UK);
		String result = engine.process("java8time-dialect", attrs);
		assertThat(result).isEqualTo("<html><body>2015-11-24</body></html>");
	}

	@Test
	public void renderTemplate() {
		load(BaseConfiguration.class);
		ISpringWebFluxTemplateEngine engine = this.context
				.getBean(ISpringWebFluxTemplateEngine.class);
		Context attrs = new Context(Locale.UK, Collections.singletonMap("foo", "bar"));
		String result = engine.process("home", attrs);
		assertThat(result).isEqualTo("<html><body>bar</body></html>");
	}

	@Test
	public void layoutDialectCanBeCustomized() {
		load(LayoutDialectConfiguration.class);
		LayoutDialect layoutDialect = this.context.getBean(LayoutDialect.class);
		assertThat(ReflectionTestUtils.getField(layoutDialect, "sortingStrategy"))
				.isInstanceOf(GroupingStrategy.class);
	}

	private void load(Class<?> config, String... envVariables) {
		this.context = new AnnotationConfigReactiveWebApplicationContext();
		TestPropertyValues.of(envVariables).applyTo(this.context);
		if (config != null) {
			this.context.register(config);
		}
		this.context.register(config);
		this.context.refresh();
	}

	@Configuration
	@ImportAutoConfiguration({ ThymeleafAutoConfiguration.class,
			PropertyPlaceholderAutoConfiguration.class })
	protected static class BaseConfiguration {

	}

	@Configuration
	@Import(BaseConfiguration.class)
	static class LayoutDialectConfiguration {

		@Bean
		public LayoutDialect layoutDialect() {
			return new LayoutDialect(new GroupingStrategy());
		}

	}

}
