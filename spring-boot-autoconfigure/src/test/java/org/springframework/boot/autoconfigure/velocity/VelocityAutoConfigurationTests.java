/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.autoconfigure.velocity;

import java.io.File;
import java.io.StringWriter;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.springframework.boot.test.rule.OutputCapture;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.boot.web.servlet.view.velocity.EmbeddedVelocityViewResolver;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.resource.ResourceUrlEncodingFilter;
import org.springframework.web.servlet.support.RequestContext;
import org.springframework.web.servlet.view.AbstractTemplateViewResolver;
import org.springframework.web.servlet.view.velocity.VelocityConfigurer;
import org.springframework.web.servlet.view.velocity.VelocityViewResolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;

/**
 * Tests for {@link VelocityAutoConfiguration}.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 */
@SuppressWarnings("deprecation")
public class VelocityAutoConfigurationTests {

	@Rule
	public OutputCapture output = new OutputCapture();

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
		assertThat(this.context.getBean(VelocityViewResolver.class)).isNotNull();
		assertThat(this.context.getBean(VelocityConfigurer.class)).isNotNull();
	}

	@Test
	public void nonExistentTemplateLocation() {
		registerAndRefreshContext(
				"spring.velocity.resourceLoaderPath:" + "classpath:/does-not-exist/");
		this.output.expect(containsString("Cannot find template location"));
	}

	@Test
	public void emptyTemplateLocation() {
		new File("target/test-classes/templates/empty-directory").mkdir();
		registerAndRefreshContext("spring.velocity.resourceLoaderPath:"
				+ "classpath:/templates/empty-directory/");
	}

	@Test
	public void defaultViewResolution() throws Exception {
		registerAndRefreshContext();
		MockHttpServletResponse response = render("home");
		String result = response.getContentAsString();
		assertThat(result).contains("home");
		assertThat(response.getContentType()).isEqualTo("text/html;charset=UTF-8");
	}

	@Test
	public void customContentType() throws Exception {
		registerAndRefreshContext("spring.velocity.contentType:application/json");
		MockHttpServletResponse response = render("home");
		String result = response.getContentAsString();
		assertThat(result).contains("home");
		assertThat(response.getContentType()).isEqualTo("application/json;charset=UTF-8");
	}

	@Test
	public void customCharset() throws Exception {
		registerAndRefreshContext("spring.velocity.charset:ISO-8859-1");
		assertThat(this.context.getBean(VelocityConfigurer.class).getVelocityEngine()
				.getProperty("input.encoding")).isEqualTo("ISO-8859-1");
	}

	@Test
	public void customPrefix() throws Exception {
		registerAndRefreshContext("spring.velocity.prefix:prefix/");
		MockHttpServletResponse response = render("prefixed");
		String result = response.getContentAsString();
		assertThat(result).contains("prefixed");
	}

	@Test
	public void customSuffix() throws Exception {
		registerAndRefreshContext("spring.velocity.suffix:.freemarker");
		MockHttpServletResponse response = render("suffixed");
		String result = response.getContentAsString();
		assertThat(result).contains("suffixed");
	}

	@Test
	public void customTemplateLoaderPath() throws Exception {
		registerAndRefreshContext(
				"spring.velocity.resourceLoaderPath:classpath:/custom-templates/");
		MockHttpServletResponse response = render("custom");
		String result = response.getContentAsString();
		assertThat(result).contains("custom");
	}

	@Test
	public void disableCache() {
		registerAndRefreshContext("spring.velocity.cache:false");
		assertThat(this.context.getBean(VelocityViewResolver.class).getCacheLimit())
				.isEqualTo(0);
	}

	@Test
	public void customVelocitySettings() {
		registerAndRefreshContext(
				"spring.velocity.properties.directive.parse.max.depth:10");
		assertThat(this.context.getBean(VelocityConfigurer.class).getVelocityEngine()
				.getProperty("directive.parse.max.depth")).isEqualTo("10");
	}

	@Test
	public void renderTemplate() throws Exception {
		registerAndRefreshContext();
		VelocityConfigurer velocity = this.context.getBean(VelocityConfigurer.class);
		StringWriter writer = new StringWriter();
		Template template = velocity.getVelocityEngine().getTemplate("message.vm");
		template.process();
		VelocityContext velocityContext = new VelocityContext();
		velocityContext.put("greeting", "Hello World");
		template.merge(velocityContext, writer);
		assertThat(writer.toString()).contains("Hello World");
	}

	@Test
	public void renderNonWebAppTemplate() throws Exception {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				VelocityAutoConfiguration.class);
		try {
			VelocityEngine velocity = context.getBean(VelocityEngine.class);
			StringWriter writer = new StringWriter();
			Template template = velocity.getTemplate("message.vm");
			template.process();
			VelocityContext velocityContext = new VelocityContext();
			velocityContext.put("greeting", "Hello World");
			template.merge(velocityContext, writer);
			assertThat(writer.toString()).contains("Hello World");
		}
		finally {
			context.close();
		}
	}

	@Test
	public void usesEmbeddedVelocityViewResolver() {
		registerAndRefreshContext("spring.velocity.toolbox:/toolbox.xml");
		VelocityViewResolver resolver = this.context.getBean(VelocityViewResolver.class);
		assertThat(resolver).isInstanceOf(EmbeddedVelocityViewResolver.class);
	}

	@Test
	public void registerResourceHandlingFilterDisabledByDefault() throws Exception {
		registerAndRefreshContext();
		assertThat(this.context.getBeansOfType(ResourceUrlEncodingFilter.class))
				.isEmpty();
	}

	@Test
	public void registerResourceHandlingFilterOnlyIfResourceChainIsEnabled()
			throws Exception {
		registerAndRefreshContext("spring.resources.chain.enabled:true");
		assertThat(this.context.getBean(ResourceUrlEncodingFilter.class)).isNotNull();
	}

	@Test
	public void allowSessionOverride() {
		registerAndRefreshContext("spring.velocity.allow-session-override:true");
		AbstractTemplateViewResolver viewResolver = this.context
				.getBean(VelocityViewResolver.class);
		assertThat(viewResolver).extracting("allowSessionOverride").containsExactly(true);
	}

	private void registerAndRefreshContext(String... env) {
		EnvironmentTestUtils.addEnvironment(this.context, env);
		this.context.register(VelocityAutoConfiguration.class);
		this.context.refresh();
	}

	public String getGreeting() {
		return "Hello World";
	}

	private MockHttpServletResponse render(String viewName) throws Exception {
		VelocityViewResolver resolver = this.context.getBean(VelocityViewResolver.class);
		View view = resolver.resolveViewName(viewName, Locale.UK);
		assertThat(view).isNotNull();
		HttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute(RequestContext.WEB_APPLICATION_CONTEXT_ATTRIBUTE,
				this.context);
		MockHttpServletResponse response = new MockHttpServletResponse();
		view.render(null, request, response);
		return response;
	}

}
