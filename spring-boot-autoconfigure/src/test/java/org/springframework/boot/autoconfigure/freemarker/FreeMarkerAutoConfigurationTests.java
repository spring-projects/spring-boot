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

package org.springframework.boot.autoconfigure.freemarker;

import java.io.File;
import java.io.StringWriter;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.support.RequestContext;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;
import org.springframework.web.servlet.view.freemarker.FreeMarkerViewResolver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link FreeMarkerAutoConfiguration}.
 * 
 * @author Andy Wilkinson
 */
public class FreeMarkerAutoConfigurationTests {

	private AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();

	@Before
	public void registerServletContext() {
		MockServletContext servletContext = new MockServletContext();
		this.context.setServletContext(servletContext);
	}

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void defaultConfiguration() {
		this.context.register(FreeMarkerAutoConfiguration.class);
		this.context.refresh();

		assertNotNull(this.context.getBean(FreeMarkerViewResolver.class));

		assertNotNull(this.context.getBean(FreeMarkerConfigurer.class));
	}

	@Test(expected = BeanCreationException.class)
	public void nonExistentTemplateLocation() {
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.freemarker.templateLoaderPath:classpath:/does-not-exist/");

		this.context.register(FreeMarkerAutoConfiguration.class);
		this.context.refresh();
	}

	@Test
	public void emptyTemplateLocation() {
		new File("target/test-classes/templates/empty-directory").mkdir();

		EnvironmentTestUtils
				.addEnvironment(this.context,
						"spring.freemarker.templateLoaderPath:classpath:/templates/empty-directory/");

		this.context.register(FreeMarkerAutoConfiguration.class);
		this.context.refresh();
	}

	@Test
	public void defaultViewResolution() throws Exception {
		this.context.register(FreeMarkerAutoConfiguration.class);
		this.context.refresh();

		MockHttpServletResponse response = render("home");
		String result = response.getContentAsString();

		assertTrue("Wrong output: " + result, result.contains("home"));
		assertEquals("text/html", response.getContentType());
	}

	@Test
	public void customContentType() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.freemarker.contentType:application/json");

		this.context.register(FreeMarkerAutoConfiguration.class);
		this.context.refresh();

		MockHttpServletResponse response = render("home");
		String result = response.getContentAsString();

		assertTrue("Wrong output: " + result, result.contains("home"));
		assertEquals("application/json", response.getContentType());
	}

	@Test
	public void customPrefix() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.freemarker.prefix:prefix/");

		this.context.register(FreeMarkerAutoConfiguration.class);
		this.context.refresh();

		MockHttpServletResponse response = render("prefixed");
		String result = response.getContentAsString();

		assertTrue("Wrong output: " + result, result.contains("prefixed"));
	}

	@Test
	public void customSuffix() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.freemarker.suffix:.freemarker");

		this.context.register(FreeMarkerAutoConfiguration.class);
		this.context.refresh();

		MockHttpServletResponse response = render("suffixed");
		String result = response.getContentAsString();

		assertTrue("Wrong output: " + result, result.contains("suffixed"));
	}

	@Test
	public void customTemplateLoaderPath() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.freemarker.templateLoaderPath:classpath:/custom-templates/");

		this.context.register(FreeMarkerAutoConfiguration.class);
		this.context.refresh();

		MockHttpServletResponse response = render("custom");
		String result = response.getContentAsString();

		assertTrue("Wrong output: " + result, result.contains("custom"));
	}

	@Test
	public void disableCache() {
		EnvironmentTestUtils
				.addEnvironment(this.context, "spring.freemarker.cache:false");

		this.context.register(FreeMarkerAutoConfiguration.class);
		this.context.refresh();

		assertEquals(0, this.context.getBean(FreeMarkerViewResolver.class)
				.getCacheLimit());
	}

	@SuppressWarnings("deprecation")
	@Test
	public void customFreeMarkerSettings() {
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.freemarker.settings.boolean_format:yup,nope");

		this.context.register(FreeMarkerAutoConfiguration.class);
		this.context.refresh();

		assertEquals("yup,nope", this.context.getBean(FreeMarkerConfigurer.class)
				.getConfiguration().getSetting("boolean_format"));
	}

	@Test
	public void renderTemplate() throws Exception {
		this.context.register(FreeMarkerAutoConfiguration.class);
		this.context.refresh();
		FreeMarkerConfigurer freemarker = this.context
				.getBean(FreeMarkerConfigurer.class);
		StringWriter writer = new StringWriter();
		freemarker.getConfiguration().getTemplate("message.ftl").process(this, writer);
		assertTrue("Wrong content: " + writer, writer.toString().contains("Hello World"));
	}

	public String getGreeting() {
		return "Hello World";
	}

	private MockHttpServletResponse render(String viewName) throws Exception {

		View view = this.context.getBean(FreeMarkerViewResolver.class).resolveViewName(
				viewName, Locale.UK);
		assertNotNull(view);

		HttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute(RequestContext.WEB_APPLICATION_CONTEXT_ATTRIBUTE,
				this.context);

		MockHttpServletResponse response = new MockHttpServletResponse();

		view.render(null, request, response);

		return response;
	}
}
