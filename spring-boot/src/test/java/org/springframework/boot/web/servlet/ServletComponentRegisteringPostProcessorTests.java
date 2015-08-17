/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.web.servlet;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.context.embedded.EmbeddedWebApplicationContext;

/**
 * Tests for {@link ServletComponentRegisteringPostProcessor}
 *
 * @author Andy Wilkinson
 */
public class ServletComponentRegisteringPostProcessorTests {

	private final ServletComponentRegisteringPostProcessor postProcessor = new ServletComponentRegisteringPostProcessor(
			new HashSet<String>(Arrays.asList(getClass().getPackage().getName())));

	private final EmbeddedWebApplicationContext context = new EmbeddedWebApplicationContext();

	@Before
	public void before() {
		this.postProcessor.setApplicationContext(this.context);
	}

	@Test
	public void test() {
		this.postProcessor.postProcessBeanFactory(this.context.getBeanFactory());
		this.context.getBeanDefinition("servletWithName");
		this.context.getBeanDefinition("defaultNameServlet");
	}

}
