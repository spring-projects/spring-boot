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

package org.springframework.boot;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.DefaultBeanNameGenerator;
import org.springframework.boot.context.embedded.AnnotationConfigEmbeddedWebApplicationContext;
import org.springframework.boot.context.embedded.jetty.JettyEmbeddedServletContainerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.env.CommandLinePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.StringUtils;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link SpringApplication}.
 * 
 * @author Phillip Webb
 */
public class SpringApplicationTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private ConfigurableApplicationContext context;

	private Environment getEnvironment() {
		if (this.context != null) {
			return this.context.getEnvironment();
		}
		throw new IllegalStateException("Could not obtain Environment");
	}

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void sourcesMustNotBeNull() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Sources must not be empty");
		new SpringApplication((Object[]) null).run();
	}

	@Test
	public void sourcesMustNotBeEmpty() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Sources must not be empty");
		new SpringApplication().run();
	}

	@Test
	public void disableBanner() throws Exception {
		SpringApplication application = spy(new SpringApplication(ExampleConfig.class));
		application.setWebEnvironment(false);
		application.setShowBanner(false);
		application.run();
		verify(application, never()).printBanner();
	}

	@Test
	public void customBanner() throws Exception {
		SpringApplication application = spy(new SpringApplication(ExampleConfig.class));
		application.setWebEnvironment(false);
		application.run();
		verify(application).printBanner();
	}

	@Test
	public void customId() throws Exception {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebEnvironment(false);
		this.context = application.run("--spring.application.name=foo");
		assertEquals("foo", this.context.getId());
	}

	@Test
	public void specificApplicationContextClass() throws Exception {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setApplicationContextClass(StaticApplicationContext.class);
		this.context = application.run();
		assertThat(this.context, instanceOf(StaticApplicationContext.class));
	}

	@Test
	public void specificApplicationContextInitializer() throws Exception {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebEnvironment(false);
		final AtomicReference<ApplicationContext> reference = new AtomicReference<ApplicationContext>();
		application
				.setInitializers(Arrays
						.asList(new ApplicationContextInitializer<ConfigurableApplicationContext>() {
							@Override
							public void initialize(ConfigurableApplicationContext context) {
								reference.set(context);
							}
						}));
		this.context = application.run("--foo=bar");
		assertThat(this.context, sameInstance(reference.get()));
		// Custom initializers do not switch off the defaults
		assertThat(getEnvironment().getProperty("foo"), equalTo("bar"));
	}

	@Test
	public void contextRefreshedEventListener() throws Exception {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebEnvironment(false);
		final AtomicReference<ApplicationContext> reference = new AtomicReference<ApplicationContext>();
		class InitalizerListener implements
				ApplicationContextInitializer<ConfigurableApplicationContext>,
				ApplicationListener<ContextRefreshedEvent> {
			@Override
			public void onApplicationEvent(ContextRefreshedEvent event) {
				reference.set(event.getApplicationContext());
			}

			@Override
			public void initialize(ConfigurableApplicationContext applicationContext) {
			}
		}
		application.setInitializers(Arrays.asList(new InitalizerListener()));
		this.context = application.run("--foo=bar");
		assertThat(this.context, sameInstance(reference.get()));
		// Custom initializers do not switch off the defaults
		assertThat(getEnvironment().getProperty("foo"), equalTo("bar"));
	}

	@Test
	public void defaultApplicationContext() throws Exception {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebEnvironment(false);
		this.context = application.run();
		assertThat(this.context, instanceOf(AnnotationConfigApplicationContext.class));
	}

	@Test
	public void defaultApplicationContextForWeb() throws Exception {
		SpringApplication application = new SpringApplication(ExampleWebConfig.class);
		application.setWebEnvironment(true);
		this.context = application.run();
		assertThat(this.context,
				instanceOf(AnnotationConfigEmbeddedWebApplicationContext.class));
	}

	@Test
	public void customEnvironment() throws Exception {
		TestSpringApplication application = new TestSpringApplication(ExampleConfig.class);
		application.setWebEnvironment(false);
		ConfigurableEnvironment environment = new StandardEnvironment();
		application.setEnvironment(environment);
		application.run();
		verify(application.getLoader()).setEnvironment(environment);
	}

	@Test
	public void customResourceLoader() throws Exception {
		TestSpringApplication application = new TestSpringApplication(ExampleConfig.class);
		application.setWebEnvironment(false);
		ResourceLoader resourceLoader = new DefaultResourceLoader();
		application.setResourceLoader(resourceLoader);
		this.context = application.run();
		verify(application.getLoader()).setResourceLoader(resourceLoader);
	}

	@Test
	public void customResourceLoaderFromConstructor() throws Exception {
		ResourceLoader resourceLoader = new DefaultResourceLoader();
		TestSpringApplication application = new TestSpringApplication(resourceLoader,
				ExampleWebConfig.class);
		this.context = application.run();
		verify(application.getLoader()).setResourceLoader(resourceLoader);
	}

	@Test
	public void customBeanNameGenerator() throws Exception {
		TestSpringApplication application = new TestSpringApplication(
				ExampleWebConfig.class);
		BeanNameGenerator beanNameGenerator = new DefaultBeanNameGenerator();
		application.setBeanNameGenerator(beanNameGenerator);
		this.context = application.run();
		verify(application.getLoader()).setBeanNameGenerator(beanNameGenerator);
		assertThat(
				this.context
						.getBean(AnnotationConfigUtils.CONFIGURATION_BEAN_NAME_GENERATOR),
				sameInstance((Object) beanNameGenerator));
	}

	@Test
	public void commandLinePropertySource() throws Exception {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebEnvironment(false);
		ConfigurableEnvironment environment = new StandardEnvironment();
		application.setEnvironment(environment);
		application.run();
		assertThat(
				hasPropertySource(environment, CommandLinePropertySource.class,
						"commandLineArgs"), equalTo(true));
	}

	@Test
	public void disableCommandLinePropertySource() throws Exception {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebEnvironment(false);
		application.setAddCommandLineProperties(false);
		ConfigurableEnvironment environment = new StandardEnvironment();
		application.setEnvironment(environment);
		application.run();
		assertThat(
				hasPropertySource(environment, MapPropertySource.class, "commandLineArgs"),
				equalTo(false));
	}

	@Test
	public void runCommandLineRunners() throws Exception {
		SpringApplication application = new SpringApplication(CommandLineRunConfig.class);
		application.setWebEnvironment(false);
		this.context = application.run("arg");
		assertTrue(this.context.getBean("runnerA", TestCommandLineRunner.class).hasRun());
		assertTrue(this.context.getBean("runnerB", TestCommandLineRunner.class).hasRun());
	}

	@Test
	public void loadSources() throws Exception {
		Object[] sources = { ExampleConfig.class, "a", TestCommandLineRunner.class };
		TestSpringApplication application = new TestSpringApplication(sources);
		application.setWebEnvironment(false);
		application.setUseMockLoader(true);
		application.run();
		@SuppressWarnings("unchecked")
		Set<Object> initialSources = (Set<Object>) ReflectionTestUtils.getField(
				application, "initialSources");
		assertThat(initialSources.toArray(), equalTo(sources));
	}

	@Test
	public void wildcardSources() {
		Object[] sources = { "classpath:org/springframework/boot/sample-${sample.app.test.prop}.xml" };
		TestSpringApplication application = new TestSpringApplication(sources);
		application.setWebEnvironment(false);
		application.run();
	}

	@Test
	public void run() throws Exception {
		this.context = SpringApplication.run(ExampleWebConfig.class);
		assertNotNull(this.context);
	}

	@Test
	public void runComponents() throws Exception {
		this.context = SpringApplication.run(new Object[] { ExampleWebConfig.class,
				Object.class }, new String[0]);
		assertNotNull(this.context);
	}

	@Test
	public void exit() throws Exception {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebEnvironment(false);
		ApplicationContext context = application.run();
		assertNotNull(context);
		assertEquals(0, SpringApplication.exit(context));
	}

	@Test
	public void exitWithExplicitCode() throws Exception {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebEnvironment(false);
		ApplicationContext context = application.run();
		assertNotNull(context);
		assertEquals(2, SpringApplication.exit(context, new ExitCodeGenerator() {
			@Override
			public int getExitCode() {
				return 2;
			}
		}));
	}

	@Test
	public void defaultCommandLineArgs() throws Exception {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setDefaultProperties(StringUtils.splitArrayElementsIntoProperties(
				new String[] { "baz=", "bar=spam" }, "="));
		application.setWebEnvironment(false);
		this.context = application.run("--bar=foo", "bucket", "crap");
		assertThat(this.context, instanceOf(AnnotationConfigApplicationContext.class));
		assertThat(getEnvironment().getProperty("bar"), equalTo("foo"));
		assertThat(getEnvironment().getProperty("baz"), equalTo(""));
	}

	@Test
	public void commandLineArgsApplyToSpringApplication() throws Exception {
		TestSpringApplication application = new TestSpringApplication(ExampleConfig.class);
		application.setWebEnvironment(false);
		application.run("--spring.main.show_banner=false");
		assertThat(application.getShowBanner(), is(false));
	}

	@Test
	public void registerShutdownHook() throws Exception {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setApplicationContextClass(SpyApplicationContext.class);
		this.context = application.run();
		SpyApplicationContext applicationContext = (SpyApplicationContext) this.context;
		verify(applicationContext.getApplicationContext()).registerShutdownHook();
	}

	private boolean hasPropertySource(ConfigurableEnvironment environment,
			Class<?> propertySourceClass, String name) {
		for (PropertySource<?> source : environment.getPropertySources()) {
			if (propertySourceClass.isInstance(source)
					&& (name == null || name.equals(source.getName()))) {
				return true;
			}
		}
		return false;
	}

	// FIXME test initializers

	// FIXME test config files?

	public static class SpyApplicationContext extends AnnotationConfigApplicationContext {

		ConfigurableApplicationContext applicationContext = spy(new AnnotationConfigApplicationContext());

		@Override
		public void registerShutdownHook() {
			this.applicationContext.registerShutdownHook();
		}

		public ConfigurableApplicationContext getApplicationContext() {
			return this.applicationContext;
		}

	}

	private static class TestSpringApplication extends SpringApplication {

		private BeanDefinitionLoader loader;

		private boolean useMockLoader;

		private boolean showBanner;

		public TestSpringApplication(Object... sources) {
			super(sources);
		}

		public TestSpringApplication(ResourceLoader resourceLoader, Object... sources) {
			super(resourceLoader, sources);
		}

		public void setUseMockLoader(boolean useMockLoader) {
			this.useMockLoader = useMockLoader;
		}

		@Override
		protected BeanDefinitionLoader createBeanDefinitionLoader(
				BeanDefinitionRegistry registry, Object[] sources) {
			if (this.useMockLoader) {
				this.loader = mock(BeanDefinitionLoader.class);
			}
			else {
				this.loader = spy(super.createBeanDefinitionLoader(registry, sources));
			}
			return this.loader;
		}

		public BeanDefinitionLoader getLoader() {
			return this.loader;
		}

		@Override
		public void setShowBanner(boolean showBanner) {
			super.setShowBanner(showBanner);
			this.showBanner = showBanner;
		}

		public boolean getShowBanner() {
			return this.showBanner;
		}

	}

	@Configuration
	static class ExampleConfig {

	}

	@Configuration
	static class ExampleWebConfig {

		@Bean
		public JettyEmbeddedServletContainerFactory container() {
			return new JettyEmbeddedServletContainerFactory();
		}

	}

	@Configuration
	static class CommandLineRunConfig {

		@Bean
		public TestCommandLineRunner runnerB() {
			return new TestCommandLineRunner(Ordered.LOWEST_PRECEDENCE, "runnerA");
		}

		@Bean
		public TestCommandLineRunner runnerA() {
			return new TestCommandLineRunner(Ordered.HIGHEST_PRECEDENCE);
		}
	}

	static class TestCommandLineRunner implements CommandLineRunner,
			ApplicationContextAware, Ordered {

		private String[] expectedBefore;

		private ApplicationContext applicationContext;

		private String[] args;

		private int order;

		public TestCommandLineRunner(int order, String... expectedBefore) {
			this.expectedBefore = expectedBefore;
			this.order = order;
		}

		@Override
		public void setApplicationContext(ApplicationContext applicationContext)
				throws BeansException {
			this.applicationContext = applicationContext;
		}

		@Override
		public int getOrder() {
			return this.order;
		}

		@Override
		public void run(String... args) {
			this.args = args;
			for (String name : this.expectedBefore) {
				TestCommandLineRunner bean = this.applicationContext.getBean(name,
						TestCommandLineRunner.class);
				assertTrue(bean.hasRun());
			}
		}

		public boolean hasRun() {
			return this.args != null;
		}

	}
}
