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

package org.springframework.boot.builder;

import java.net.URL;
import java.net.URLClassLoader;

import org.junit.After;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * @author Dave Syer
 */
public class SpringApplicationBuilderTests {

	private ConfigurableApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void profileAndProperties() throws Exception {
		SpringApplicationBuilder application = new SpringApplicationBuilder()
				.sources(ExampleConfig.class)
				.contextClass(StaticApplicationContext.class).profiles("foo")
				.properties("foo=bar");
		this.context = application.run();
		assertThat(this.context, is(instanceOf(StaticApplicationContext.class)));
		assertThat(this.context.getEnvironment().getProperty("foo"),
				is(equalTo("bucket")));
		assertThat(this.context.getEnvironment().acceptsProfiles("foo"), is(true));
	}

	@Test
	public void specificApplicationContextClass() throws Exception {
		SpringApplicationBuilder application = new SpringApplicationBuilder().sources(
				ExampleConfig.class).contextClass(StaticApplicationContext.class);
		this.context = application.run();
		assertThat(this.context, is(instanceOf(StaticApplicationContext.class)));
	}

	@Test
	public void parentContextCreation() throws Exception {
		SpringApplicationBuilder application = new SpringApplicationBuilder(
				ChildConfig.class).contextClass(SpyApplicationContext.class);
		application.parent(ExampleConfig.class);
		this.context = application.run();
		verify(((SpyApplicationContext) this.context).getApplicationContext()).setParent(
				any(ApplicationContext.class));
	}

	@Test
	public void contextWithClassLoader() throws Exception {
		SpringApplicationBuilder application = new SpringApplicationBuilder(
				ExampleConfig.class).contextClass(SpyApplicationContext.class);
		ClassLoader classLoader = new URLClassLoader(new URL[0], getClass()
				.getClassLoader());
		application.resourceLoader(new DefaultResourceLoader(classLoader));
		this.context = application.run();
		assertThat(((SpyApplicationContext) this.context).getClassLoader(),
				is(equalTo(classLoader)));
	}

	@Test
	public void parentContextWithClassLoader() throws Exception {
		SpringApplicationBuilder application = new SpringApplicationBuilder(
				ChildConfig.class).contextClass(SpyApplicationContext.class);
		ClassLoader classLoader = new URLClassLoader(new URL[0], getClass()
				.getClassLoader());
		application.resourceLoader(new DefaultResourceLoader(classLoader));
		application.parent(ExampleConfig.class);
		this.context = application.run();
		assertThat(((SpyApplicationContext) this.context).getResourceLoader()
				.getClassLoader(), is(equalTo(classLoader)));
	}

	@Test
	public void parentFirstCreation() throws Exception {
		SpringApplicationBuilder application = new SpringApplicationBuilder(
				ExampleConfig.class).child(ChildConfig.class);
		application.contextClass(SpyApplicationContext.class);
		this.context = application.run();
		verify(((SpyApplicationContext) this.context).getApplicationContext()).setParent(
				any(ApplicationContext.class));
	}

	@Test
	public void parentFirstCreationWithProfileAndDefaultArgs() throws Exception {
		SpringApplicationBuilder application = new SpringApplicationBuilder(
				ExampleConfig.class).profiles("node").properties("transport=redis")
				.child(ChildConfig.class).web(false);
		this.context = application.run();
		assertThat(this.context.getEnvironment().acceptsProfiles("node"), is(true));
		assertThat(this.context.getEnvironment().getProperty("transport"),
				is(equalTo("redis")));
		assertThat(this.context.getParent().getEnvironment().acceptsProfiles("node"),
				is(true));
		assertThat(this.context.getParent().getEnvironment().getProperty("transport"),
				is(equalTo("redis")));
		// only defined in node profile
		assertThat(this.context.getEnvironment().getProperty("bar"), is(equalTo("spam")));
	}

	@Test
	public void parentContextIdentical() throws Exception {
		SpringApplicationBuilder application = new SpringApplicationBuilder(
				ExampleConfig.class);
		application.parent(ExampleConfig.class);
		application.contextClass(SpyApplicationContext.class);
		this.context = application.run();
		verify(((SpyApplicationContext) this.context).getApplicationContext()).setParent(
				any(ApplicationContext.class));
	}

	@Configuration
	static class ExampleConfig {

	}

	@Configuration
	static class ChildConfig {

	}

	public static class SpyApplicationContext extends AnnotationConfigApplicationContext {

		private ConfigurableApplicationContext applicationContext = spy(new AnnotationConfigApplicationContext());
		private ResourceLoader resourceLoader;

		@Override
		public void setParent(ApplicationContext parent) {
			this.applicationContext.setParent(parent);
		}

		public ConfigurableApplicationContext getApplicationContext() {
			return this.applicationContext;
		}

		@Override
		public void setResourceLoader(ResourceLoader resourceLoader) {
			super.setResourceLoader(resourceLoader);
			this.resourceLoader = resourceLoader;
		}

		public ResourceLoader getResourceLoader() {
			return this.resourceLoader;
		}

	}
}