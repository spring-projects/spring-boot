/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.autoconfigure.groovy.template;

import java.io.File;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import groovy.text.markup.BaseTemplate;
import groovy.text.markup.MarkupTemplateEngine;
import groovy.text.markup.TemplateConfiguration;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.testsupport.BuildOutput;
import org.springframework.boot.testsupport.classpath.resources.WithResource;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebApplicationContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.support.RequestContext;
import org.springframework.web.servlet.view.groovy.GroovyMarkupConfig;
import org.springframework.web.servlet.view.groovy.GroovyMarkupConfigurer;
import org.springframework.web.servlet.view.groovy.GroovyMarkupViewResolver;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link GroovyTemplateAutoConfiguration}.
 *
 * @author Dave Syer
 */
class GroovyTemplateAutoConfigurationTests {

	private final BuildOutput buildOutput = new BuildOutput(getClass());

	private final AnnotationConfigServletWebApplicationContext context = new AnnotationConfigServletWebApplicationContext();

	@BeforeEach
	void setupContext() {
		this.context.setServletContext(new MockServletContext());
	}

	@AfterEach
	void close() {
		LocaleContextHolder.resetLocaleContext();
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	void defaultConfiguration() {
		registerAndRefreshContext();
		assertThat(this.context.getBean(GroovyMarkupViewResolver.class)).isNotNull();
	}

	@Test
	void emptyTemplateLocation() {
		new File(this.buildOutput.getTestResourcesLocation(), "empty-templates/empty-directory").mkdirs();
		registerAndRefreshContext("spring.groovy.template.resource-loader-path:classpath:/templates/empty-directory/");
	}

	@Test
	@WithResource(name = "templates/home.tpl", content = "yield 'home'")
	void defaultViewResolution() throws Exception {
		registerAndRefreshContext();
		MockHttpServletResponse response = render("home");
		String result = response.getContentAsString();
		assertThat(result).contains("home");
		assertThat(response.getContentType()).isEqualTo("text/html;charset=UTF-8");
	}

	@Test
	@WithResource(name = "templates/includes.tpl", content = """
			yield 'include'
			include template: 'included.tpl'
			""")
	@WithResource(name = "templates/included.tpl", content = "yield 'here'")
	void includesViewResolution() throws Exception {
		registerAndRefreshContext();
		MockHttpServletResponse response = render("includes");
		String result = response.getContentAsString();
		assertThat(result).contains("here");
		assertThat(response.getContentType()).isEqualTo("text/html;charset=UTF-8");
	}

	@Test
	void disableViewResolution() {
		TestPropertyValues.of("spring.groovy.template.enabled:false").applyTo(this.context);
		registerAndRefreshContext();
		assertThat(this.context.getBeanNamesForType(ViewResolver.class)).isEmpty();
	}

	@Test
	@WithResource(name = "templates/includes.tpl", content = """
			yield 'include'
			include template: 'included.tpl'
			""")
	@WithResource(name = "templates/included_fr.tpl", content = "yield 'voila'")
	void localeViewResolution() throws Exception {
		registerAndRefreshContext();
		MockHttpServletResponse response = render("includes", Locale.FRENCH);
		String result = response.getContentAsString();
		assertThat(result).contains("voila");
		assertThat(response.getContentType()).isEqualTo("text/html;charset=UTF-8");
	}

	@Test
	@WithResource(name = "templates/home.tpl", content = "yield 'home'")
	void customContentType() throws Exception {
		registerAndRefreshContext("spring.groovy.template.contentType:application/json");
		MockHttpServletResponse response = render("home");
		String result = response.getContentAsString();
		assertThat(result).contains("home");
		assertThat(response.getContentType()).isEqualTo("application/json;charset=UTF-8");
	}

	@Test
	@WithResource(name = "templates/prefix/prefixed.tpl", content = "yield \"prefixed\"")
	void customPrefix() throws Exception {
		registerAndRefreshContext("spring.groovy.template.prefix:prefix/");
		MockHttpServletResponse response = render("prefixed");
		String result = response.getContentAsString();
		assertThat(result).contains("prefixed");
	}

	@Test
	@WithResource(name = "templates/suffixed.groovytemplate", content = "yield \"suffixed\"")
	void customSuffix() throws Exception {
		registerAndRefreshContext("spring.groovy.template.suffix:.groovytemplate");
		MockHttpServletResponse response = render("suffixed");
		String result = response.getContentAsString();
		assertThat(result).contains("suffixed");
	}

	@Test
	@WithResource(name = "custom-templates/custom.tpl", content = "yield \"custom\"")
	void customTemplateLoaderPath() throws Exception {
		registerAndRefreshContext("spring.groovy.template.resource-loader-path:classpath:/custom-templates/");
		MockHttpServletResponse response = render("custom");
		String result = response.getContentAsString();
		assertThat(result).contains("custom");
	}

	@Test
	void disableCache() {
		registerAndRefreshContext("spring.groovy.template.cache:false");
		assertThat(this.context.getBean(GroovyMarkupViewResolver.class).getCacheLimit()).isZero();
	}

	@Test
	@WithResource(name = "templates/message.tpl", content = "yield \"Message: ${greeting}\"")
	void renderTemplate() throws Exception {
		registerAndRefreshContext();
		GroovyMarkupConfig config = this.context.getBean(GroovyMarkupConfig.class);
		MarkupTemplateEngine engine = config.getTemplateEngine();
		Writer writer = new StringWriter();
		engine.createTemplate(new ClassPathResource("templates/message.tpl").getFile())
			.make(new HashMap<>(Collections.singletonMap("greeting", "Hello World")))
			.writeTo(writer);
		assertThat(writer.toString()).contains("Hello World");
	}

	@Test
	@Deprecated(since = "3.5.0", forRemoval = true)
	void customConfiguration() {
		registerAndRefreshContext("spring.groovy.template.configuration.auto-indent:true");
		assertThat(this.context.getBean(GroovyMarkupConfigurer.class).isAutoIndent()).isTrue();
	}

	@Test
	void enableAutoEscape() {
		registerAndRefreshContext("spring.groovy.template.auto-escape:true");
		assertThat(this.context.getBean(GroovyMarkupConfigurer.class).isAutoEscape()).isTrue();
	}

	@Test
	void enableAutoIndent() {
		registerAndRefreshContext("spring.groovy.template.auto-indent:true");
		assertThat(this.context.getBean(GroovyMarkupConfigurer.class).isAutoIndent()).isTrue();
	}

	@Test
	void customAutoIndentString() {
		registerAndRefreshContext("spring.groovy.template.auto-indent-string:\\t");
		assertThat(this.context.getBean(GroovyMarkupConfigurer.class).getAutoIndentString()).isEqualTo("\\t");
	}

	@Test
	void enableAutoNewLine() {
		registerAndRefreshContext("spring.groovy.template.auto-new-line:true");
		assertThat(this.context.getBean(GroovyMarkupConfigurer.class).isAutoNewLine()).isTrue();
	}

	@Test
	void customBaseTemplateClass() {
		registerAndRefreshContext("spring.groovy.template.base-template-class:" + CustomBaseTemplate.class.getName());
		assertThat(this.context.getBean(GroovyMarkupConfigurer.class).getBaseTemplateClass())
			.isEqualTo(CustomBaseTemplate.class);
	}

	@Test
	void customDeclarationEncoding() {
		registerAndRefreshContext("spring.groovy.template.declaration-encoding:UTF-8");
		assertThat(this.context.getBean(GroovyMarkupConfigurer.class).getDeclarationEncoding()).isEqualTo("UTF-8");
	}

	@Test
	void enableExpandEmptyElements() {
		registerAndRefreshContext("spring.groovy.template.expand-empty-elements:true");
		assertThat(this.context.getBean(GroovyMarkupConfigurer.class).isExpandEmptyElements()).isTrue();
	}

	@Test
	void customLocale() {
		registerAndRefreshContext("spring.groovy.template.locale:en_US");
		assertThat(this.context.getBean(GroovyMarkupConfigurer.class).getLocale()).isEqualTo(Locale.US);
	}

	@Test
	void customNewLineString() {
		registerAndRefreshContext("spring.groovy.template.new-line-string:\\r\\n");
		assertThat(this.context.getBean(GroovyMarkupConfigurer.class).getNewLineString()).isEqualTo("\\r\\n");
	}

	@Test
	void enableUseDoubleQuotes() {
		registerAndRefreshContext("spring.groovy.template.use-double-quotes:true");
		assertThat(this.context.getBean(GroovyMarkupConfigurer.class).isUseDoubleQuotes()).isTrue();
	}

	private void registerAndRefreshContext(String... env) {
		TestPropertyValues.of(env).applyTo(this.context);
		this.context.register(GroovyTemplateAutoConfiguration.class);
		this.context.refresh();
	}

	private MockHttpServletResponse render(String viewName) throws Exception {
		return render(viewName, Locale.UK);
	}

	private MockHttpServletResponse render(String viewName, Locale locale) throws Exception {
		LocaleContextHolder.setLocale(locale);
		GroovyMarkupViewResolver resolver = this.context.getBean(GroovyMarkupViewResolver.class);
		View view = resolver.resolveViewName(viewName, locale);
		assertThat(view).isNotNull();
		HttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute(RequestContext.WEB_APPLICATION_CONTEXT_ATTRIBUTE, this.context);
		MockHttpServletResponse response = new MockHttpServletResponse();
		view.render(null, request, response);
		return response;
	}

	static class CustomBaseTemplate extends BaseTemplate {

		@SuppressWarnings("rawtypes")
		CustomBaseTemplate(MarkupTemplateEngine templateEngine, Map model, Map<String, String> modelTypes,
				TemplateConfiguration configuration) {
			super(templateEngine, model, modelTypes, configuration);
		}

		@Override
		public Object run() {
			return null;
		}

	}

}
