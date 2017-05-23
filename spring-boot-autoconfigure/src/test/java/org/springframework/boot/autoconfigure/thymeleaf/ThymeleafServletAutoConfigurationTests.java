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
import org.thymeleaf.spring5.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring5.view.ThymeleafView;
import org.thymeleaf.spring5.view.ThymeleafViewResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.rule.OutputCapture;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.resource.ResourceUrlEncodingFilter;
import org.springframework.web.servlet.support.RequestContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;

/**
 * Tests for {@link ThymeleafAutoConfiguration} in Servlet-based applications.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 * @author Eddú Meléndez
 * @author Brian Clozel
 */
public class ThymeleafServletAutoConfigurationTests {

	@Rule
	public OutputCapture output = new OutputCapture();

	private AnnotationConfigWebApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void createFromConfigClass() throws Exception {
		load(BaseConfiguration.class, "spring.thymeleaf.mode:HTML",
				"spring.thymeleaf.suffix:");
		TemplateEngine engine = this.context.getBean(TemplateEngine.class);
		Context attrs = new Context(Locale.UK, Collections.singletonMap("foo", "bar"));
		String result = engine.process("template.html", attrs);
		assertThat(result).isEqualTo("<html>bar</html>");
	}

	@Test
	public void overrideCharacterEncoding() throws Exception {
		load(BaseConfiguration.class, "spring.thymeleaf.encoding:UTF-16");
		ITemplateResolver resolver = this.context.getBean(ITemplateResolver.class);
		assertThat(resolver instanceof SpringResourceTemplateResolver).isTrue();
		assertThat(((SpringResourceTemplateResolver) resolver).getCharacterEncoding())
				.isEqualTo("UTF-16");
		ThymeleafViewResolver views = this.context.getBean(ThymeleafViewResolver.class);
		assertThat(views.getCharacterEncoding()).isEqualTo("UTF-16");
		assertThat(views.getContentType()).isEqualTo("text/html;charset=UTF-16");
	}

	@Test
	public void overrideTemplateResolverOrder() throws Exception {
		load(BaseConfiguration.class, "spring.thymeleaf.templateResolverOrder:25");
		ITemplateResolver resolver = this.context.getBean(ITemplateResolver.class);
		assertThat(resolver.getOrder()).isEqualTo(Integer.valueOf(25));
	}

	@Test
	public void overrideViewNames() throws Exception {
		load(BaseConfiguration.class, "spring.thymeleaf.viewNames:foo,bar");
		ThymeleafViewResolver views = this.context.getBean(ThymeleafViewResolver.class);
		assertThat(views.getViewNames()).isEqualTo(new String[] { "foo", "bar" });
	}

	@Test
	public void templateLocationDoesNotExist() throws Exception {
		load(BaseConfiguration.class,
				"spring.thymeleaf.prefix:classpath:/no-such-directory/");
		this.output.expect(containsString("Cannot find template location"));
	}

	@Test
	public void templateLocationEmpty() throws Exception {
		new File("target/test-classes/templates/empty-directory").mkdir();
		load(BaseConfiguration.class,
				"spring.thymeleaf.prefix:classpath:/templates/empty-directory/");
	}

	@Test
	public void createLayoutFromConfigClass() throws Exception {
		AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
		context.register(ThymeleafAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		MockServletContext servletContext = new MockServletContext();
		context.setServletContext(servletContext);
		context.refresh();
		ThymeleafView view = (ThymeleafView) context.getBean(ThymeleafViewResolver.class)
				.resolveViewName("view", Locale.UK);
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute(RequestContext.WEB_APPLICATION_CONTEXT_ATTRIBUTE, context);
		view.render(Collections.singletonMap("foo", "bar"), request, response);
		String result = response.getContentAsString();
		assertThat(result).contains("<title>Content</title>");
		assertThat(result).contains("<span>bar</span>");
		context.close();
	}

	@Test
	public void useDataDialect() throws Exception {
		load(BaseConfiguration.class);
		TemplateEngine engine = this.context.getBean(TemplateEngine.class);
		Context attrs = new Context(Locale.UK, Collections.singletonMap("foo", "bar"));
		String result = engine.process("data-dialect", attrs);
		assertThat(result).isEqualTo("<html><body data-foo=\"bar\"></body></html>");
	}

	@Test
	public void useJava8TimeDialect() throws Exception {
		load(BaseConfiguration.class);
		TemplateEngine engine = this.context.getBean(TemplateEngine.class);
		Context attrs = new Context(Locale.UK);
		String result = engine.process("java8time-dialect", attrs);
		assertThat(result).isEqualTo("<html><body>2015-11-24</body></html>");
	}

	@Test
	public void renderTemplate() throws Exception {
		load(BaseConfiguration.class);
		TemplateEngine engine = this.context.getBean(TemplateEngine.class);
		Context attrs = new Context(Locale.UK, Collections.singletonMap("foo", "bar"));
		String result = engine.process("home", attrs);
		assertThat(result).isEqualTo("<html><body>bar</body></html>");
	}

	@Test
	public void renderNonWebAppTemplate() throws Exception {
		try (AnnotationConfigApplicationContext customContext = new AnnotationConfigApplicationContext(
				ThymeleafAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class)) {
			assertThat(customContext.getBeanNamesForType(ViewResolver.class).length)
					.isEqualTo(0);
			TemplateEngine engine = customContext.getBean(TemplateEngine.class);
			Context attrs = new Context(Locale.UK,
					Collections.singletonMap("greeting", "Hello World"));
			String result = engine.process("message", attrs);
			assertThat(result).contains("Hello World");
		}
	}

	@Test
	public void registerResourceHandlingFilterDisabledByDefault() throws Exception {
		load(BaseConfiguration.class);
		assertThat(this.context.getBeansOfType(ResourceUrlEncodingFilter.class))
				.isEmpty();
	}

	@Test
	public void registerResourceHandlingFilterOnlyIfResourceChainIsEnabled()
			throws Exception {
		load(BaseConfiguration.class, "spring.resources.chain.enabled:true");
		assertThat(this.context.getBean(ResourceUrlEncodingFilter.class)).isNotNull();
	}

	@Test
	public void layoutDialectCanBeCustomized() throws Exception {
		load(LayoutDialectConfiguration.class);
		LayoutDialect layoutDialect = this.context.getBean(LayoutDialect.class);
		assertThat(ReflectionTestUtils.getField(layoutDialect, "sortingStrategy"))
				.isInstanceOf(GroupingStrategy.class);
	}

	@Test
	public void cachingCanBeDisabled() {
		load(BaseConfiguration.class, "spring.thymeleaf.cache:false");
		assertThat(this.context.getBean(ThymeleafViewResolver.class).isCache()).isFalse();
		SpringResourceTemplateResolver templateResolver = this.context
				.getBean(SpringResourceTemplateResolver.class);
		assertThat(templateResolver.isCacheable()).isFalse();
	}

	private void load(Class<?> config, String... envVariables) {
		this.context = new AnnotationConfigWebApplicationContext();
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
	static class BaseConfiguration {

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
