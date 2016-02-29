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

package org.springframework.boot.web.servlet.view.velocity;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.mock.MockHttpServletRequest;
import org.apache.struts.mock.MockHttpServletResponse;
import org.apache.struts.mock.MockServletContext;
import org.apache.velocity.tools.ToolContext;
import org.junit.Test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.view.velocity.VelocityConfigurer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link EmbeddedVelocityToolboxView}.
 *
 * @author Phillip Webb
 */
@SuppressWarnings("deprecation")
public class EmbeddedVelocityToolboxViewTests {

	private static final String PATH = EmbeddedVelocityToolboxViewTests.class.getPackage()
			.getName().replace(".", "/");

	@Test
	public void loadsContextFromClassPath() throws Exception {
		ToolContext context = getToolContext(PATH + "/toolbox.xml");
		assertThat(context.getToolbox().keySet()).contains("math");
	}

	@Test
	public void loadsWithoutConfig() throws Exception {
		ToolContext context = getToolContext(null);
		assertThat(context).isNotNull();
	}

	private ToolContext getToolContext(String toolboxConfigLocation) throws Exception {
		AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
		context.setServletContext(new MockServletContext());
		context.register(Config.class);
		context.refresh();
		EmbeddedVelocityToolboxView view = context
				.getBean(EmbeddedVelocityToolboxView.class);
		view.setToolboxConfigLocation(toolboxConfigLocation);
		Map<String, Object> model = new LinkedHashMap<String, Object>();
		HttpServletRequest request = new MockHttpServletRequest();
		HttpServletResponse response = new MockHttpServletResponse();
		ToolContext toolContext = (ToolContext) view.createVelocityContext(model, request,
				response);
		context.close();
		return toolContext;
	}

	@Configuration
	static class Config {

		@Bean
		public EmbeddedVelocityToolboxView view() {
			EmbeddedVelocityToolboxView view = new EmbeddedVelocityToolboxView();
			view.setUrl("http://example.com");
			return view;
		}

		@Bean
		public VelocityConfigurer velocityConfigurer() {
			return new VelocityConfigurer();
		}

	}

}
