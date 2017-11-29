/*
 * Copyright 2012-2017 the original author or authors.
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
import java.util.Collections;

import org.junit.After;
import org.junit.Test;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link SpringApplicationBuilder}.
 *
 * @author Dave Syer
 */
public class SpringApplicationBuilderTests {

	private ConfigurableApplicationContext context;

	@After
	public void close() {
		close(this.context);
	}

	private void close(ApplicationContext context) {
		if (context != null) {
			if (context instanceof ConfigurableApplicationContext) {
				((ConfigurableApplicationContext) context).close();
			}
			close(context.getParent());
		}
	}

	@Test
	public void profileAndProperties() throws Exception {
		SpringApplicationBuilder application = new SpringApplicationBuilder()
				.sources(ExampleConfig.class).contextClass(StaticApplicationContext.class)
				.profiles("foo").properties("foo=bar");
		this.context = application.run();
		assertThat(this.context).isInstanceOf(StaticApplicationContext.class);
		assertThat(this.context.getEnvironment().getProperty("foo")).isEqualTo("bucket");
		assertThat(this.context.getEnvironment().acceptsProfiles("foo")).isTrue();
	}

	@Test
	public void propertiesAsMap() throws Exception {
		SpringApplicationBuilder application = new SpringApplicationBuilder()
				.sources(ExampleConfig.class).contextClass(StaticApplicationContext.class)
				.properties(Collections.singletonMap("bar", "foo"));
		this.context = application.run();
		assertThat(this.context.getEnvironment().getProperty("bar")).isEqualTo("foo");
	}

	@Test
	public void propertiesAsProperties() throws Exception {
		SpringApplicationBuilder application = new SpringApplicationBuilder()
				.sources(ExampleConfig.class).contextClass(StaticApplicationContext.class)
				.properties(StringUtils.splitArrayElementsIntoProperties(
						new String[] { "bar=foo" }, "="));
		this.context = application.run();
		assertThat(this.context.getEnvironment().getProperty("bar")).isEqualTo("foo");
	}

	@Test
	public void propertiesWithRepeatSeparator() throws Exception {
		SpringApplicationBuilder application = new SpringApplicationBuilder()
				.sources(ExampleConfig.class).contextClass(StaticApplicationContext.class)
				.properties("one=c:\\logging.file", "two=a:b", "three:c:\\logging.file",
						"four:a:b");
		this.context = application.run();
		ConfigurableEnvironment environment = this.context.getEnvironment();
		assertThat(environment.getProperty("one")).isEqualTo("c:\\logging.file");
		assertThat(environment.getProperty("two")).isEqualTo("a:b");
		assertThat(environment.getProperty("three")).isEqualTo("c:\\logging.file");
		assertThat(environment.getProperty("four")).isEqualTo("a:b");
	}

	@Test
	public void specificApplicationContextClass() throws Exception {
		SpringApplicationBuilder application = new SpringApplicationBuilder()
				.sources(ExampleConfig.class)
				.contextClass(StaticApplicationContext.class);
		this.context = application.run();
		assertThat(this.context).isInstanceOf(StaticApplicationContext.class);
	}

	@Test
	public void parentContextCreationThatIsRunDirectly() throws Exception {
		SpringApplicationBuilder application = new SpringApplicationBuilder(
				ChildConfig.class).contextClass(SpyApplicationContext.class);
		application.parent(ExampleConfig.class);
		this.context = application.run("foo.bar=baz");
		verify(((SpyApplicationContext) this.context).getApplicationContext())
				.setParent(any(ApplicationContext.class));
		assertThat(((SpyApplicationContext) this.context).getRegisteredShutdownHook())
				.isFalse();
		assertThat(this.context.getParent().getBean(ApplicationArguments.class)
				.getNonOptionArgs()).contains("foo.bar=baz");
		assertThat(this.context.getBean(ApplicationArguments.class).getNonOptionArgs())
				.contains("foo.bar=baz");
	}

	@Test
	public void parentContextCreationThatIsBuiltThenRun() throws Exception {
		SpringApplicationBuilder application = new SpringApplicationBuilder(
				ChildConfig.class).contextClass(SpyApplicationContext.class);
		application.parent(ExampleConfig.class);
		this.context = application.build("a=alpha").run("b=bravo");
		verify(((SpyApplicationContext) this.context).getApplicationContext())
				.setParent(any(ApplicationContext.class));
		assertThat(((SpyApplicationContext) this.context).getRegisteredShutdownHook())
				.isFalse();
		assertThat(this.context.getParent().getBean(ApplicationArguments.class)
				.getNonOptionArgs()).contains("a=alpha");
		assertThat(this.context.getBean(ApplicationArguments.class).getNonOptionArgs())
				.contains("b=bravo");
	}

	@Test
	public void parentContextCreationWithChildShutdown() throws Exception {
		SpringApplicationBuilder application = new SpringApplicationBuilder(
				ChildConfig.class).contextClass(SpyApplicationContext.class)
						.registerShutdownHook(true);
		application.parent(ExampleConfig.class);
		this.context = application.run();
		verify(((SpyApplicationContext) this.context).getApplicationContext())
				.setParent(any(ApplicationContext.class));
		assertThat(((SpyApplicationContext) this.context).getRegisteredShutdownHook())
				.isTrue();
	}

	@Test
	public void contextWithClassLoader() throws Exception {
		SpringApplicationBuilder application = new SpringApplicationBuilder(
				ExampleConfig.class).contextClass(SpyApplicationContext.class);
		ClassLoader classLoader = new URLClassLoader(new URL[0],
				getClass().getClassLoader());
		application.resourceLoader(new DefaultResourceLoader(classLoader));
		this.context = application.run();
		assertThat(((SpyApplicationContext) this.context).getClassLoader())
				.isEqualTo(classLoader);
	}

	@Test
	public void parentContextWithClassLoader() throws Exception {
		SpringApplicationBuilder application = new SpringApplicationBuilder(
				ChildConfig.class).contextClass(SpyApplicationContext.class);
		ClassLoader classLoader = new URLClassLoader(new URL[0],
				getClass().getClassLoader());
		application.resourceLoader(new DefaultResourceLoader(classLoader));
		application.parent(ExampleConfig.class);
		this.context = application.run();
		assertThat(((SpyApplicationContext) this.context).getResourceLoader()
				.getClassLoader()).isEqualTo(classLoader);
	}

	@Test
	public void parentFirstCreation() throws Exception {
		SpringApplicationBuilder application = new SpringApplicationBuilder(
				ExampleConfig.class).child(ChildConfig.class);
		application.contextClass(SpyApplicationContext.class);
		this.context = application.run();
		verify(((SpyApplicationContext) this.context).getApplicationContext())
				.setParent(any(ApplicationContext.class));
		assertThat(((SpyApplicationContext) this.context).getRegisteredShutdownHook())
				.isFalse();
	}

	@Test
	public void parentFirstCreationWithProfileAndDefaultArgs() throws Exception {
		SpringApplicationBuilder application = new SpringApplicationBuilder(
				ExampleConfig.class).profiles("node").properties("transport=redis")
						.child(ChildConfig.class).web(WebApplicationType.NONE);
		this.context = application.run();
		assertThat(this.context.getEnvironment().acceptsProfiles("node")).isTrue();
		assertThat(this.context.getEnvironment().getProperty("transport"))
				.isEqualTo("redis");
		assertThat(this.context.getParent().getEnvironment().acceptsProfiles("node"))
				.isTrue();
		assertThat(this.context.getParent().getEnvironment().getProperty("transport"))
				.isEqualTo("redis");
		// only defined in node profile
		assertThat(this.context.getEnvironment().getProperty("bar")).isEqualTo("spam");
	}

	@Test
	public void parentFirstWithDifferentProfile() throws Exception {
		SpringApplicationBuilder application = new SpringApplicationBuilder(
				ExampleConfig.class).profiles("node").properties("transport=redis")
						.child(ChildConfig.class).profiles("admin")
						.web(WebApplicationType.NONE);
		this.context = application.run();
		assertThat(this.context.getEnvironment().acceptsProfiles("node", "admin"))
				.isTrue();
		assertThat(this.context.getParent().getEnvironment().acceptsProfiles("admin"))
				.isFalse();
	}

	@Test
	public void parentWithDifferentProfile() throws Exception {
		SpringApplicationBuilder shared = new SpringApplicationBuilder(
				ExampleConfig.class).profiles("node").properties("transport=redis");
		SpringApplicationBuilder application = shared.child(ChildConfig.class)
				.profiles("admin").web(WebApplicationType.NONE);
		shared.profiles("parent");
		this.context = application.run();
		assertThat(this.context.getEnvironment().acceptsProfiles("node", "admin"))
				.isTrue();
		assertThat(this.context.getParent().getEnvironment().acceptsProfiles("node",
				"parent")).isTrue();
		assertThat(this.context.getParent().getEnvironment().acceptsProfiles("admin"))
				.isFalse();
	}

	@Test
	public void parentFirstWithDifferentProfileAndExplicitEnvironment() throws Exception {
		SpringApplicationBuilder application = new SpringApplicationBuilder(
				ExampleConfig.class).environment(new StandardEnvironment())
						.profiles("node").properties("transport=redis")
						.child(ChildConfig.class).profiles("admin")
						.web(WebApplicationType.NONE);
		this.context = application.run();
		assertThat(this.context.getEnvironment().acceptsProfiles("node", "admin"))
				.isTrue();
		// Now they share an Environment explicitly so there's no way to keep the profiles
		// separate
		assertThat(this.context.getParent().getEnvironment().acceptsProfiles("admin"))
				.isTrue();
	}

	@Test
	public void parentContextIdentical() throws Exception {
		SpringApplicationBuilder application = new SpringApplicationBuilder(
				ExampleConfig.class);
		application.parent(ExampleConfig.class);
		application.contextClass(SpyApplicationContext.class);
		this.context = application.run();
		verify(((SpyApplicationContext) this.context).getApplicationContext())
				.setParent(any(ApplicationContext.class));
	}

	@Test
	public void initializersCreatedOnce() throws Exception {
		SpringApplicationBuilder application = new SpringApplicationBuilder(
				ExampleConfig.class).web(WebApplicationType.NONE);
		this.context = application.run();
		assertThat(application.application().getInitializers()).hasSize(4);
	}

	@Test
	public void initializersCreatedOnceForChild() throws Exception {
		SpringApplicationBuilder application = new SpringApplicationBuilder(
				ExampleConfig.class).child(ChildConfig.class)
						.web(WebApplicationType.NONE);
		this.context = application.run();
		assertThat(application.application().getInitializers()).hasSize(5);
	}

	@Test
	public void initializersIncludeDefaults() throws Exception {
		SpringApplicationBuilder application = new SpringApplicationBuilder(
				ExampleConfig.class).web(WebApplicationType.NONE).initializers(
						(ConfigurableApplicationContext applicationContext) -> {
						});
		this.context = application.run();
		assertThat(application.application().getInitializers()).hasSize(5);
	}

	@Test
	public void sourcesWithBoundSources() throws Exception {
		SpringApplicationBuilder application = new SpringApplicationBuilder()
				.web(WebApplicationType.NONE).sources(ExampleConfig.class)
				.properties("spring.main.sources=" + ChildConfig.class.getName());
		this.context = application.run();
		this.context.getBean(ExampleConfig.class);
		this.context.getBean(ChildConfig.class);
	}

	@Configuration
	static class ExampleConfig {

	}

	@Configuration
	static class ChildConfig {

	}

	public static class SpyApplicationContext extends AnnotationConfigApplicationContext {

		private final ConfigurableApplicationContext applicationContext = spy(
				new AnnotationConfigApplicationContext());

		private ResourceLoader resourceLoader;

		private boolean registeredShutdownHook;

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

		@Override
		public void registerShutdownHook() {
			super.registerShutdownHook();
			this.registeredShutdownHook = true;
		}

		public boolean getRegisteredShutdownHook() {
			return this.registeredShutdownHook;
		}

		@Override
		public void close() {
			super.close();
			this.applicationContext.close();
		}

		@Override
		public ApplicationContext getParent() {
			return this.applicationContext.getParent();
		}

	}

}
