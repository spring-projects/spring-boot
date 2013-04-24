/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.bootstrap.context.embedded;

import javax.servlet.Servlet;

import org.junit.Test;
import org.springframework.bootstrap.context.embedded.AnnotationConfigEmbeddedWebApplicationContext;
import org.springframework.bootstrap.context.embedded.config.ExampleEmbeddedWebApplicationConfiguration;

import static org.mockito.Mockito.*;

/**
 * Tests for {@link AnnotationConfigEmbeddedWebApplicationContext}.
 * 
 * @author Phillip Webb
 */
public class AnnotationConfigEmbeddedWebApplicationContextTests {

	private AnnotationConfigEmbeddedWebApplicationContext context;

	@Test
	public void createFromScan() throws Exception {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext(
				ExampleEmbeddedWebApplicationConfiguration.class.getPackage().getName());
		verifyContext();
	}

	@Test
	public void createFromConfigClass() throws Exception {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext(
				ExampleEmbeddedWebApplicationConfiguration.class);
		verifyContext();
	}

	@Test
	public void registerAndRefresh() throws Exception {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
		this.context.register(ExampleEmbeddedWebApplicationConfiguration.class);
		this.context.refresh();
		verifyContext();
	}

	@Test
	public void scanAndRefresh() throws Exception {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
		this.context.scan(ExampleEmbeddedWebApplicationConfiguration.class.getPackage()
				.getName());
		this.context.refresh();
		verifyContext();
	}

	private void verifyContext() {
		MockEmbeddedServletContainerFactory containerFactory = this.context
				.getBean(MockEmbeddedServletContainerFactory.class);
		Servlet servlet = this.context.getBean(Servlet.class);
		verify(containerFactory.getServletContext()).addServlet("servlet", servlet);
	}
}
