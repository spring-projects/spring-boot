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

package org.springframework.boot.context.web;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.servlet.ServletContext;

import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link SpringBootServletInitializer}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
public class SpringBootServletInitializerTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private ServletContext servletContext = new MockServletContext();

	private SpringApplication application;

	@Test
	public void failsWithoutConfigure() throws Exception {
		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectMessage("No SpringApplication sources have been defined");
		new MockSpringBootServletInitializer()
				.createRootApplicationContext(this.servletContext);
	}

	@Test
	public void withConfigurationAnnotation() throws Exception {
		new WithConfigurationAnnotation()
				.createRootApplicationContext(this.servletContext);
		assertThat(this.application.getSources(),
				equalToSet(WithConfigurationAnnotation.class, ErrorPageFilter.class));
	}

	@Test
	public void withConfiguredSource() throws Exception {
		new WithConfiguredSource().createRootApplicationContext(this.servletContext);
		assertThat(this.application.getSources(),
				equalToSet(Config.class, ErrorPageFilter.class));
	}

	@Test
	public void applicationBuilderCanBeCustomized() throws Exception {
		CustomSpringBootServletInitializer servletInitializer = new CustomSpringBootServletInitializer();
		servletInitializer.createRootApplicationContext(this.servletContext);
		assertThat(servletInitializer.applicationBuilder.built, equalTo(true));
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void mainClassHasSensibleDefault() throws Exception {
		new WithConfigurationAnnotation()
				.createRootApplicationContext(this.servletContext);
		Class mainApplicationClass = (Class<?>) new DirectFieldAccessor(this.application)
				.getPropertyValue("mainApplicationClass");
		assertThat(mainApplicationClass,
				is(equalTo((Class) WithConfigurationAnnotation.class)));
	}

	@Test
	public void withErrorPageFilterNotRegistered() throws Exception {
		new WithErrorPageFilterNotRegistered()
				.createRootApplicationContext(this.servletContext);
		assertThat(this.application.getSources(),
				equalToSet(WithErrorPageFilterNotRegistered.class));
	}

	private Matcher<? super Set<Object>> equalToSet(Object... items) {
		Set<Object> set = new LinkedHashSet<Object>();
		Collections.addAll(set, items);
		return equalTo(set);
	}

	private class MockSpringBootServletInitializer extends SpringBootServletInitializer {

		@Override
		protected WebApplicationContext run(SpringApplication application) {
			SpringBootServletInitializerTests.this.application = application;
			return null;
		};

	}

	private class CustomSpringBootServletInitializer
			extends MockSpringBootServletInitializer {

		private final CustomSpringApplicationBuilder applicationBuilder = new CustomSpringApplicationBuilder();

		@Override
		protected SpringApplicationBuilder createSpringApplicationBuilder() {
			return this.applicationBuilder;
		}

		@Override
		protected SpringApplicationBuilder configure(
				SpringApplicationBuilder application) {
			return application.sources(Config.class);
		}
	}

	@Configuration
	public class WithConfigurationAnnotation extends MockSpringBootServletInitializer {
	}

	public class WithConfiguredSource extends MockSpringBootServletInitializer {

		@Override
		protected SpringApplicationBuilder configure(
				SpringApplicationBuilder application) {
			return application.sources(Config.class);
		}

	}

	@Configuration
	public class WithErrorPageFilterNotRegistered
			extends MockSpringBootServletInitializer {

		public WithErrorPageFilterNotRegistered() {
			setRegisterErrorPageFilter(false);
		}

	}

	@Configuration
	public static class Config {

	}

	private static class CustomSpringApplicationBuilder extends SpringApplicationBuilder {

		private boolean built;

		@Override
		public SpringApplication build() {
			this.built = true;
			return super.build();
		}

	}

}
