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
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.support.RequestContext;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;
import org.springframework.web.servlet.view.freemarker.FreeMarkerViewResolver;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link FreeMarkerAutoConfiguration}.
 * 
 * @author Andy Wilkinson
 */
public class FreeMarkerAutoConfigurationTests {

	private AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();

	@Before
	public void setupContext() {
		this.context.setServletContext(new MockServletContext());
	}

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void defaultConfiguration() {
		registerAndRefreshContext();
		assertThat(this.context.getBean(FreeMarkerViewResolver.class), notNullValue());
		assertThat(this.context.getBean(FreeMarkerConfigurer.class), notNullValue());
	}

	@Test(expected = BeanCreationException.class)
	public void nonExistentTemplateLocation() {
		registerAndRefreshContext("spring.freemarker.path:"
				+ "classpath:/does-not-exist/");
	}

	@Test
	public void emptyTemplateLocation() {
		new File("target/test-classes/templates/empty-directory").mkdir();
		registerAndRefreshContext("spring.freemarker.path:"
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
	public void customContentType() throws Exception {
		registerAndRefreshContext("spring.freemarker.contentType:application/json");
		MockHttpServletResponse response = render("home");
		String result = response.getContentAsString();
		assertThat(result, containsString("home"));
		assertThat(response.getContentType(), equalTo("application/json;charset=UTF-8"));
	}

	@Test
	public void customPrefix() throws Exception {
		registerAndRefreshContext("spring.freemarker.prefix:prefix/");
		MockHttpServletResponse response = render("prefixed");
		String result = response.getContentAsString();
		assertThat(result, containsString("prefixed"));
	}

	@Test
	public void customSuffix() throws Exception {
		registerAndRefreshContext("spring.freemarker.suffix:.freemarker");
		MockHttpServletResponse response = render("suffixed");
		String result = response.getContentAsString();
		assertThat(result, containsString("suffixed"));
	}

	@Test
	public void customTemplateLoaderPath() throws Exception {
		registerAndRefreshContext("spring.freemarker.path:classpath:/custom-templates/");
		MockHttpServletResponse response = render("custom");
		String result = response.getContentAsString();
		assertThat(result, containsString("custom"));
	}

	@Test
	public void disableCache() {
		registerAndRefreshContext("spring.freemarker.cache:false");
		assertThat(this.context.getBean(FreeMarkerViewResolver.class).getCacheLimit(),
				equalTo(0));
	}

	@SuppressWarnings("deprecation")
	@Test
	public void customFreeMarkerSettings() {
		registerAndRefreshContext("spring.freemarker.settings.boolean_format:yup,nope");
		assertThat(this.context.getBean(FreeMarkerConfigurer.class).getConfiguration()
				.getSetting("boolean_format"), equalTo("yup,nope"));
	}

	@Test
	public void renderTemplate() throws Exception {
		registerAndRefreshContext();
		FreeMarkerConfigurer freemarker = this.context
				.getBean(FreeMarkerConfigurer.class);
		StringWriter writer = new StringWriter();
		freemarker.getConfiguration().getTemplate("message.ftl").process(this, writer);
		assertThat(writer.toString(), containsString("Hello World"));
	}

	@Test
	public void renderNonWebAppTemplate() throws Exception {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				FreeMarkerAutoConfiguration.class);
		try {
			freemarker.template.Configuration freemarker = context
					.getBean(freemarker.template.Configuration.class);
			StringWriter writer = new StringWriter();
			freemarker.getTemplate("message.ftl").process(this, writer);
			assertThat(writer.toString(), containsString("Hello World"));
		}
		finally {
			context.close();
		}
	}

	private void registerAndRefreshContext(String... env) {
		EnvironmentTestUtils.addEnvironment(this.context, env);
		this.context.register(FreeMarkerAutoConfiguration.class);
		this.context.refresh();
	}

	public String getGreeting() {
		return "Hello World";
	}

	private MockHttpServletResponse render(String viewName) throws Exception {
		FreeMarkerViewResolver resolver = this.context
				.getBean(FreeMarkerViewResolver.class);
		View view = resolver.resolveViewName(viewName, Locale.UK);
		assertThat(view, notNullValue());
		HttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute(RequestContext.WEB_APPLICATION_CONTEXT_ATTRIBUTE,
				this.context);
		MockHttpServletResponse response = new MockHttpServletResponse();
		view.render(null, request, response);
		return response;
	}
}
