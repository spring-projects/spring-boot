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

package org.springframework.boot.autoconfigure.h2;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.context.embedded.ServletRegistrationBean;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link H2ConsoleAutoConfiguration}
 *
 * @author Andy Wilkinson
 */
public class H2ConsoleAutoConfigurationTests {

	private AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();

	@Rule
	public ExpectedException thrown = ExpectedException.none();

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
	public void consoleIsDisabledByDefault() {
		this.context.register(H2ConsoleAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBeansOfType(ServletRegistrationBean.class).size(),
				is(equalTo(0)));
	}

	@Test
	public void propertyCanEnableConsole() {
		this.context.register(H2ConsoleAutoConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.h2.console.enabled:true");
		this.context.refresh();
		assertThat(this.context.getBeansOfType(ServletRegistrationBean.class).size(),
				is(equalTo(1)));
		assertThat(this.context.getBean(ServletRegistrationBean.class).getUrlMappings(),
				hasItems("/h2-console/*"));
	}

	@Test
	public void customPathMustBeginWithASlash() {
		this.thrown.expect(BeanCreationException.class);
		this.thrown.expectMessage("Path must start with /");
		this.context.register(H2ConsoleAutoConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.h2.console.enabled:true", "spring.h2.console.path:custom");
		this.context.refresh();
	}

	@Test
	public void customPathWithTrailingSlash() {
		this.context.register(H2ConsoleAutoConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.h2.console.enabled:true", "spring.h2.console.path:/custom/");
		this.context.refresh();
		assertThat(this.context.getBeansOfType(ServletRegistrationBean.class).size(),
				is(equalTo(1)));
		assertThat(this.context.getBean(ServletRegistrationBean.class).getUrlMappings(),
				hasItems("/custom/*"));
	}

	@Test
	public void customPath() {
		this.context.register(H2ConsoleAutoConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.h2.console.enabled:true", "spring.h2.console.path:/custom");
		this.context.refresh();
		assertThat(this.context.getBeansOfType(ServletRegistrationBean.class).size(),
				is(equalTo(1)));
		assertThat(this.context.getBean(ServletRegistrationBean.class).getUrlMappings(),
				hasItems("/custom/*"));
	}

}
