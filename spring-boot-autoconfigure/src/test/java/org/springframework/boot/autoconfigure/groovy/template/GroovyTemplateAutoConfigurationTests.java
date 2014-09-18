/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.autoconfigure.groovy.template;

import groovy.text.markup.MarkupTemplateEngine;

import java.io.File;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.support.RequestContext;
import org.springframework.web.servlet.view.groovy.GroovyMarkupConfig;
import org.springframework.web.servlet.view.groovy.GroovyMarkupViewResolver;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link GroovyTemplateAutoConfiguration}.
 *
 * @author Dave Syer
 */
public class GroovyTemplateAutoConfigurationTests {

	private AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();

	@Before
	public void setupContext() {
		this.context.setServletContext(new MockServletContext());
	}

	@After
	public void close() {
		LocaleContextHolder.resetLocaleContext();
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void defaultConfiguration() {
		registerAndRefreshContext();
		assertThat(this.context.getBean(GroovyMarkupViewResolver.class), notNullValue());
	}

	@Test
	public void emptyTemplateLocation() {
		new File("target/test-classes/templates/empty-directory").mkdir();
		registerAndRefreshContext("spring.groovy.template.prefix:"
				+ "classpath:/templates/empty-directory/");
	}

	@Test
	public void defaultViewResolution() throws Exception {
		registerAndRefreshContext();
		MockHttpServletResponse response = render("home");
		String result = response.getContentAsString();
		assertThat(result, containsString("home"));
		assertThat(response.getContentType(), equalTo("text/html;charset=UTF-8"));
	}

	@Test
	public void includesViewResolution() throws Exception {
		registerAndRefreshContext();
		MockHttpServletResponse response = render("includes");
		String result = response.getContentAsString();
		assertThat(result, containsString("here"));
		assertThat(response.getContentType(), equalTo("text/html;charset=UTF-8"));
	}

	@Test
	public void localeViewResolution() throws Exception {
		registerAndRefreshContext();
		MockHttpServletResponse response = render("includes", Locale.FRENCH);
		String result = response.getContentAsString();
		assertThat(result, containsString("voila"));
		assertThat(response.getContentType(), equalTo("text/html;charset=UTF-8"));
	}

	@Test
	public void customContentType() throws Exception {
		registerAndRefreshContext("spring.groovy.template.contentType:application/json");
		MockHttpServletResponse response = render("home");
		String result = response.getContentAsString();
		assertThat(result, containsString("home"));
		assertThat(response.getContentType(), equalTo("application/json;charset=UTF-8"));
	}

	@Test
	public void customPrefix() throws Exception {
		registerAndRefreshContext("spring.groovy.template.prefix:classpath:/templates/prefix/");
		MockHttpServletResponse response = render("prefixed");
		String result = response.getContentAsString();
		assertThat(result, containsString("prefixed"));
	}

	@Test
	public void customSuffix() throws Exception {
		registerAndRefreshContext("spring.groovy.template.suffix:.groovytemplate");
		MockHttpServletResponse response = render("suffixed");
		String result = response.getContentAsString();
		assertThat(result, containsString("suffixed"));
	}

	@Test
	public void customTemplateLoaderPath() throws Exception {
		registerAndRefreshContext("spring.groovy.template.prefix:classpath:/custom-templates/");
		MockHttpServletResponse response = render("custom");
		String result = response.getContentAsString();
		assertThat(result, containsString("custom"));
	}

	@Test
	public void disableCache() {
		registerAndRefreshContext("spring.groovy.template.cache:false");
		assertThat(this.context.getBean(GroovyMarkupViewResolver.class).getCacheLimit(),
				equalTo(0));
	}

	@Test
	public void renderTemplate() throws Exception {
		registerAndRefreshContext();
		GroovyMarkupConfig config = this.context.getBean(GroovyMarkupConfig.class);
		MarkupTemplateEngine engine = config.getTemplateEngine();
		Writer writer = new StringWriter();
		engine.createTemplate(new ClassPathResource("templates/message.tpl").getFile())
				.make(new HashMap<String, Object>(Collections.singletonMap("greeting",
						"Hello World"))).writeTo(writer);
		assertThat(writer.toString(), containsString("Hello World"));
	}

	private void registerAndRefreshContext(String... env) {
		EnvironmentTestUtils.addEnvironment(this.context, env);
		this.context.register(GroovyTemplateAutoConfiguration.class);
		this.context.refresh();
	}

	private MockHttpServletResponse render(String viewName) throws Exception {
		return render(viewName, Locale.UK);
	}

	private MockHttpServletResponse render(String viewName, Locale locale)
			throws Exception {
		LocaleContextHolder.setLocale(locale);
		GroovyMarkupViewResolver resolver = this.context
				.getBean(GroovyMarkupViewResolver.class);
		View view = resolver.resolveViewName(viewName, locale);
		assertThat(view, notNullValue());
		HttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute(RequestContext.WEB_APPLICATION_CONTEXT_ATTRIBUTE,
				this.context);
		MockHttpServletResponse response = new MockHttpServletResponse();
		view.render(null, request, response);
		return response;
	}
}
