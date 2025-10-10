/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
import java.util.Properties;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationContextFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationShutdownHookInstance;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.testsupport.classpath.resources.WithResource;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Profiles;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * Tests for {@link SpringApplicationBuilder}.
 *
 * @author Dave Syer
 */
class SpringApplicationBuilderTests {

	private @Nullable ConfigurableApplicationContext context;

	@AfterEach
	void close() {
		close(this.context);
		SpringApplicationShutdownHookInstance.reset();
	}

	private void close(@Nullable ApplicationContext context) {
		if (context != null) {
			if (context instanceof ConfigurableApplicationContext configurableContext) {
				configurableContext.close();
			}
			close(context.getParent());
		}
	}

	@Test
	@WithResource(name = "application.properties", content = """
			b=file
			c=file
			""")
	@WithResource(name = "application-foo.properties", content = "b=profile-specific-file")
	void profileAndProperties() {
		SpringApplicationBuilder application = new SpringApplicationBuilder().sources(ExampleConfig.class)
			.contextFactory(ApplicationContextFactory.ofContextClass(StaticApplicationContext.class))
			.profiles("foo")
			.properties("a=default");
		this.context = application.run();
		assertThat(this.context).isInstanceOf(StaticApplicationContext.class);
		assertThat(this.context.getEnvironment().getProperty("a")).isEqualTo("default");
		assertThat(this.context.getEnvironment().getProperty("b")).isEqualTo("profile-specific-file");
		assertThat(this.context.getEnvironment().getProperty("c")).isEqualTo("file");
		assertThat(this.context.getEnvironment().acceptsProfiles(Profiles.of("foo"))).isTrue();
	}

	@Test
	void propertiesAsMap() {
		SpringApplicationBuilder application = new SpringApplicationBuilder().sources(ExampleConfig.class)
			.contextFactory(ApplicationContextFactory.ofContextClass(StaticApplicationContext.class))
			.properties(Collections.singletonMap("bar", "foo"));
		this.context = application.run();
		assertThat(this.context.getEnvironment().getProperty("bar")).isEqualTo("foo");
	}

	@Test
	void propertiesAsProperties() {
		Properties properties = StringUtils.splitArrayElementsIntoProperties(new String[] { "bar=foo" }, "=");
		assertThat(properties).isNotNull();
		SpringApplicationBuilder application = new SpringApplicationBuilder().sources(ExampleConfig.class)
			.contextFactory(ApplicationContextFactory.ofContextClass(StaticApplicationContext.class))
			.properties(properties);
		this.context = application.run();
		assertThat(this.context.getEnvironment().getProperty("bar")).isEqualTo("foo");
	}

	@Test
	void propertiesWithRepeatSeparator() {
		SpringApplicationBuilder application = new SpringApplicationBuilder().sources(ExampleConfig.class)
			.contextFactory(ApplicationContextFactory.ofContextClass(StaticApplicationContext.class))
			.properties("one=c:\\logging.file.name", "two=a:b", "three:c:\\logging.file.name", "four:a:b");
		this.context = application.run();
		ConfigurableEnvironment environment = this.context.getEnvironment();
		assertThat(environment.getProperty("one")).isEqualTo("c:\\logging.file.name");
		assertThat(environment.getProperty("two")).isEqualTo("a:b");
		assertThat(environment.getProperty("three")).isEqualTo("c:\\logging.file.name");
		assertThat(environment.getProperty("four")).isEqualTo("a:b");
	}

	@Test
	void specificApplicationContextFactory() {
		SpringApplicationBuilder application = new SpringApplicationBuilder().sources(ExampleConfig.class)
			.contextFactory(ApplicationContextFactory.ofContextClass(StaticApplicationContext.class));
		this.context = application.run();
		assertThat(this.context).isInstanceOf(StaticApplicationContext.class);
	}

	@Test
	void parentContextCreationThatIsRunDirectly() {
		SpringApplicationBuilder application = new SpringApplicationBuilder(ChildConfig.class)
			.contextFactory(ApplicationContextFactory.ofContextClass(SpyApplicationContext.class));
		application.parent(ExampleConfig.class);
		this.context = application.run("foo.bar=baz");
		then(((SpyApplicationContext) this.context).getApplicationContext()).should()
			.setParent(any(ApplicationContext.class));
		assertThat(SpringApplicationShutdownHookInstance.get()).didNotRegisterApplicationContext(this.context);
		ApplicationContext parent = this.context.getParent();
		assertThat(parent).isNotNull();
		assertThat(parent.getBean(ApplicationArguments.class).getNonOptionArgs()).contains("foo.bar=baz");
		assertThat(this.context.getBean(ApplicationArguments.class).getNonOptionArgs()).contains("foo.bar=baz");
	}

	@Test
	void parentContextCreationThatIsBuiltThenRun() {
		SpringApplicationBuilder application = new SpringApplicationBuilder(ChildConfig.class)
			.contextFactory(ApplicationContextFactory.ofContextClass(SpyApplicationContext.class));
		application.parent(ExampleConfig.class);
		this.context = application.build("a=alpha").run("b=bravo");
		then(((SpyApplicationContext) this.context).getApplicationContext()).should()
			.setParent(any(ApplicationContext.class));
		assertThat(SpringApplicationShutdownHookInstance.get()).didNotRegisterApplicationContext(this.context);
		ApplicationContext parent = this.context.getParent();
		assertThat(parent).isNotNull();
		assertThat(parent.getBean(ApplicationArguments.class).getNonOptionArgs()).contains("a=alpha");
		assertThat(this.context.getBean(ApplicationArguments.class).getNonOptionArgs()).contains("b=bravo");
	}

	@Test
	void parentContextCreationWithChildShutdown() {
		SpringApplicationBuilder application = new SpringApplicationBuilder(ChildConfig.class)
			.contextFactory(ApplicationContextFactory.ofContextClass(SpyApplicationContext.class))
			.registerShutdownHook(true);
		application.parent(ExampleConfig.class);
		this.context = application.run();
		then(((SpyApplicationContext) this.context).getApplicationContext()).should()
			.setParent(any(ApplicationContext.class));
		assertThat(SpringApplicationShutdownHookInstance.get()).registeredApplicationContext(this.context);
	}

	@Test
	void contextWithClassLoader() {
		SpringApplicationBuilder application = new SpringApplicationBuilder(ExampleConfig.class)
			.contextFactory(ApplicationContextFactory.ofContextClass(SpyApplicationContext.class));
		ClassLoader classLoader = new URLClassLoader(new URL[0], getClass().getClassLoader());
		application.resourceLoader(new DefaultResourceLoader(classLoader));
		this.context = application.run();
		assertThat(this.context.getClassLoader()).isEqualTo(classLoader);
	}

	@Test
	void parentContextWithClassLoader() {
		SpringApplicationBuilder application = new SpringApplicationBuilder(ChildConfig.class)
			.contextFactory(ApplicationContextFactory.ofContextClass(SpyApplicationContext.class));
		ClassLoader classLoader = new URLClassLoader(new URL[0], getClass().getClassLoader());
		application.resourceLoader(new DefaultResourceLoader(classLoader));
		application.parent(ExampleConfig.class);
		this.context = application.run();
		ResourceLoader resourceLoader = ((SpyApplicationContext) this.context).getResourceLoader();
		assertThat(resourceLoader).isNotNull();
		assertThat(resourceLoader.getClassLoader()).isEqualTo(classLoader);
	}

	@Test
	void parentFirstCreation() {
		SpringApplicationBuilder application = new SpringApplicationBuilder(ExampleConfig.class)
			.child(ChildConfig.class);
		application.contextFactory(ApplicationContextFactory.ofContextClass(SpyApplicationContext.class));
		this.context = application.run();
		then(((SpyApplicationContext) this.context).getApplicationContext()).should()
			.setParent(any(ApplicationContext.class));
		assertThat(SpringApplicationShutdownHookInstance.get()).didNotRegisterApplicationContext(this.context);
	}

	@Test
	@WithResource(name = "application-node.properties", content = "bar=spam")
	void parentFirstCreationWithProfileAndDefaultArgs() {
		SpringApplicationBuilder application = new SpringApplicationBuilder(ExampleConfig.class).profiles("node")
			.properties("transport=redis")
			.child(ChildConfig.class)
			.web(WebApplicationType.NONE);
		this.context = application.run();
		assertThat(this.context.getEnvironment().acceptsProfiles(Profiles.of("node"))).isTrue();
		assertThat(this.context.getEnvironment().getProperty("transport")).isEqualTo("redis");
		ApplicationContext parent = this.context.getParent();
		assertThat(parent).isNotNull();
		assertThat(parent.getEnvironment().acceptsProfiles(Profiles.of("node"))).isTrue();
		assertThat(parent.getEnvironment().getProperty("transport")).isEqualTo("redis");
		// only defined in node profile
		assertThat(this.context.getEnvironment().getProperty("bar")).isEqualTo("spam");
	}

	@Test
	void parentFirstWithDifferentProfile() {
		SpringApplicationBuilder application = new SpringApplicationBuilder(ExampleConfig.class).profiles("node")
			.properties("transport=redis")
			.child(ChildConfig.class)
			.profiles("admin")
			.web(WebApplicationType.NONE);
		this.context = application.run();
		assertThat(this.context.getEnvironment().acceptsProfiles(Profiles.of("node", "admin"))).isTrue();
		ApplicationContext parent = this.context.getParent();
		assertThat(parent).isNotNull();
		assertThat(parent.getEnvironment().acceptsProfiles(Profiles.of("admin"))).isFalse();
	}

	@Test
	void parentWithDifferentProfile() {
		SpringApplicationBuilder shared = new SpringApplicationBuilder(ExampleConfig.class).profiles("node")
			.properties("transport=redis");
		SpringApplicationBuilder application = shared.child(ChildConfig.class)
			.profiles("admin")
			.web(WebApplicationType.NONE);
		shared.profiles("parent");
		this.context = application.run();
		assertThat(this.context.getEnvironment().acceptsProfiles(Profiles.of("node", "admin"))).isTrue();
		ApplicationContext parent = this.context.getParent();
		assertThat(parent).isNotNull();
		assertThat(parent.getEnvironment().acceptsProfiles(Profiles.of("node", "parent"))).isTrue();
		assertThat(parent.getEnvironment().acceptsProfiles(Profiles.of("admin"))).isFalse();
	}

	@Test
	void parentFirstWithDifferentProfileAndExplicitEnvironment() {
		SpringApplicationBuilder application = new SpringApplicationBuilder(ExampleConfig.class)
			.environment(new StandardEnvironment())
			.profiles("node")
			.properties("transport=redis")
			.child(ChildConfig.class)
			.profiles("admin")
			.web(WebApplicationType.NONE);
		this.context = application.run();
		assertThat(this.context.getEnvironment().acceptsProfiles(Profiles.of("node", "admin"))).isTrue();
		// Now they share an Environment explicitly so there's no way to keep the profiles
		// separate
		ApplicationContext parent = this.context.getParent();
		assertThat(parent).isNotNull();
		assertThat(parent.getEnvironment().acceptsProfiles(Profiles.of("admin"))).isTrue();
	}

	@Test
	void parentContextIdentical() {
		SpringApplicationBuilder application = new SpringApplicationBuilder(ExampleConfig.class);
		application.parent(ExampleConfig.class);
		application.contextFactory(ApplicationContextFactory.ofContextClass(SpyApplicationContext.class));
		this.context = application.run();
		then(((SpyApplicationContext) this.context).getApplicationContext()).should()
			.setParent(any(ApplicationContext.class));
	}

	@Test
	void initializersCreatedOnce() {
		SpringApplicationBuilder application = new SpringApplicationBuilder(ExampleConfig.class)
			.web(WebApplicationType.NONE);
		this.context = application.run();
		assertThat(application.application().getInitializers()).hasSize(3);
	}

	@Test
	void initializersCreatedOnceForChild() {
		SpringApplicationBuilder application = new SpringApplicationBuilder(ExampleConfig.class)
			.child(ChildConfig.class)
			.web(WebApplicationType.NONE);
		this.context = application.run();
		assertThat(application.application().getInitializers()).hasSize(4);
	}

	@Test
	void initializersIncludeDefaults() {
		SpringApplicationBuilder application = new SpringApplicationBuilder(ExampleConfig.class)
			.web(WebApplicationType.NONE)
			.initializers((ConfigurableApplicationContext applicationContext) -> {
			});
		this.context = application.run();
		assertThat(application.application().getInitializers()).hasSize(4);
	}

	@Test
	void sourcesWithBoundSources() {
		SpringApplicationBuilder application = new SpringApplicationBuilder().web(WebApplicationType.NONE)
			.sources(ExampleConfig.class)
			.properties("spring.main.sources=" + ChildConfig.class.getName());
		this.context = application.run();
		this.context.getBean(ExampleConfig.class);
		this.context.getBean(ChildConfig.class);
	}

	@Test
	void addBootstrapRegistryInitializer() {
		SpringApplicationBuilder application = new SpringApplicationBuilder(ExampleConfig.class)
			.web(WebApplicationType.NONE)
			.addBootstrapRegistryInitializer((context) -> context.addCloseListener(
					(event) -> event.getApplicationContext().getBeanFactory().registerSingleton("test", "spring")));
		this.context = application.run();
		assertThat(this.context.getBean("test")).isEqualTo("spring");
	}

	@Test
	void setEnvironmentPrefix() {
		SpringApplicationBuilder builder = new SpringApplicationBuilder(ExampleConfig.class).environmentPrefix("test");
		assertThat(builder.application().getEnvironmentPrefix()).isEqualTo("test");
	}

	@Test
	void customApplicationWithResourceLoader() {
		ResourceLoader resourceLoader = mock(ResourceLoader.class);
		given(resourceLoader.getClassLoader()).willReturn(getClass().getClassLoader());
		SpringApplicationBuilder applicationBuilder = new SpringApplicationBuilder(resourceLoader,
				ExampleConfig.class) {
			@Override
			protected SpringApplication createSpringApplication(@Nullable ResourceLoader resourceLoader,
					Class<?>... sources) {
				return new CustomSpringApplication(resourceLoader, sources);
			}
		};
		SpringApplication application = applicationBuilder.build();
		assertThat(application).asInstanceOf(InstanceOfAssertFactories.type(CustomSpringApplication.class))
			.satisfies((customApp) -> assertThat(customApp.resourceLoader).isEqualTo(resourceLoader));
	}

	@Configuration(proxyBeanMethods = false)
	static class ExampleConfig {

	}

	@Configuration(proxyBeanMethods = false)
	static class ChildConfig {

	}

	static class CustomSpringApplication extends SpringApplication {

		private final @Nullable ResourceLoader resourceLoader;

		CustomSpringApplication(@Nullable ResourceLoader resourceLoader, Class<?>... primarySources) {
			super(resourceLoader, primarySources);
			this.resourceLoader = resourceLoader;
		}

	}

	static class SpyApplicationContext extends AnnotationConfigApplicationContext {

		private final ConfigurableApplicationContext applicationContext = spy(new AnnotationConfigApplicationContext());

		private @Nullable ResourceLoader resourceLoader;

		@Override
		public void setParent(@Nullable ApplicationContext parent) {
			this.applicationContext.setParent(parent);
		}

		ConfigurableApplicationContext getApplicationContext() {
			return this.applicationContext;
		}

		@Override
		public void setResourceLoader(ResourceLoader resourceLoader) {
			super.setResourceLoader(resourceLoader);
			this.resourceLoader = resourceLoader;
		}

		@Nullable ResourceLoader getResourceLoader() {
			return this.resourceLoader;
		}

		@Override
		public void close() {
			super.close();
			this.applicationContext.close();
		}

		@Override
		public @Nullable ApplicationContext getParent() {
			return this.applicationContext.getParent();
		}

	}

}
