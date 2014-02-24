/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot.autoconfigure.scalate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;

import org.fusesource.scalate.servlet.ServletTemplateEngine;
import org.fusesource.scalate.spring.view.ScalateViewResolver;
import org.fusesource.scalate.support.UriTemplateSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.core.env.MapPropertySource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.view.AbstractUrlBasedView;

/**
 * Tests for {@link ScalateAutoConfiguration}.
 * 
 * @author Christoph Nagel
 */
public class ScalateAutoConfigurationTests {

	AnnotationConfigWebApplicationContext context;

	/**
	 * Initializes the application and servlet context.
	 */
	@Before
	public void initContext() {
		context = new AnnotationConfigWebApplicationContext();
		ServletContext servletContext = new MockServletContext("");
		servletContext.setAttribute(
				WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, context);
		context.setServletContext(servletContext);
		context.register(ScalateAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		context.refresh();
	}

	@After
	public void closeContext() {
		context.close();
	}

	/**
	 * Checks if the template engine is able to find requested files.
	 */
	@Test
	public void test_ServletTemplateEngine() {
		ServletTemplateEngine engine = context.getBean(ServletTemplateEngine.class);
		UriTemplateSource source = engine.uriToSource("templates/scalate_view.scaml");
		assertTrue(source.delegate().toFile().get().exists());
	}

	/**
	 * Renders the scalate_view.scaml template without layout.
	 * 
	 * @throws Exception
	 */
	@Test
	public void test_ScalateViewResolver_default() throws Exception {
		Map<String, Object> model = new HashMap<String, Object>();
		model.put("name", "Foo");
		// render
		String output = render("scalate_view", model);
		// check output
		assertEquals("<p>Hello Foo</p>", output);
	}

	/**
	 * Renders the scalate_view.scaml template with layout.scaml
	 * 
	 * @throws Exception
	 */
	@Test
	public void test_ScalateViewResolver_layout() throws Exception {
		// render
		String output = render("layout:scalate_view", new HashMap<String, Object>());
		// check output
		assertTrue(output.contains("<p>Hello Unknown</p>"));
		assertTrue(output.startsWith("<!DOCTYPE html>"));
	}

	/**
	 * Renders the scalate_view.scaml template with layout.jade
	 * 
	 * @throws Exception
	 */
	@Test
	public void test_ScalateViewResolver_customized() throws Exception {
		// set specific parameters
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("spring.scalate.suffix", "");
		map.put("spring.scalate.layout", "templates/layout.jade");
		context.getEnvironment().getPropertySources().addFirst(
				new MapPropertySource("test", map));
		context.refresh();
		// render
		String output = render("layout:scalate_view.scaml", new HashMap<String, Object>());
		// check output
		assertTrue(output.contains("<p>Hello Unknown</p>"));
		assertTrue(output.startsWith("<!DOCTYPE html>"));
	}

	/**
	 * Helper to render a view
	 * 
	 * @param viewUrl The view name
	 * @param model The model accessible in the view by attributes
	 * @return The rendered view output
	 * @throws Exception
	 */
	protected String render(String viewUrl, Map<String, Object> model) throws Exception {
		// get resolver
		ScalateViewResolver resolver = context.getBean(ScalateViewResolver.class);
		// get view
		AbstractUrlBasedView view = resolver.buildView(viewUrl);
		// initialize view
		view.setApplicationContext(context);
		MockHttpServletRequest request = new MockHttpServletRequest(
				context.getServletContext());
		MockHttpServletResponse response = new MockHttpServletResponse();
		// render view
		view.render(model, request, response);
		return response.getContentAsString().trim();
	}

}
