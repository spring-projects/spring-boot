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

package org.springframework.boot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.DefaultBeanNameGenerator;
import org.springframework.boot.context.embedded.AnnotationConfigEmbeddedWebApplicationContext;
import org.springframework.boot.context.embedded.jetty.JettyEmbeddedServletContainerFactory;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.boot.test.OutputCapture;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.env.CommandLinePropertySource;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StringUtils;
import org.springframework.web.context.support.StandardServletEnvironment;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link SpringApplication}.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Andy Wilkinson
 * @author Christian Dupuis
 * @author Stephane Nicoll
 * @author Jeremy Rickard
 */
public class SpringApplicationTests {

	private String headlessProperty;

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Rule
	public OutputCapture output = new OutputCapture();

	private ConfigurableApplicationContext context;

	private Environment getEnvironment() {
		if (this.context != null) {
			return this.context.getEnvironment();
		}
		throw new IllegalStateException("Could not obtain Environment");
	}

	@Before
	public void storeAndClearHeadlessProperty() {
		this.headlessProperty = System.getProperty("java.awt.headless");
		System.clearProperty("java.awt.headless");
	}

	@After
	public void reinstateHeadlessProperty() {
		if (this.headlessProperty == null) {
			System.clearProperty("java.awt.headless");
		}
		else {
			System.setProperty("java.awt.headless", this.headlessProperty);
		}

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
	public void sourcesMustBeAccessible() throws Exception {
		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectMessage("Cannot load configuration");
		new SpringApplication(InaccessibleConfiguration.class).run();
	}

	@Test
	public void disableBannerWithMode() throws Exception {
		SpringApplication application = spy(new SpringApplication(ExampleConfig.class));
		application.setWebEnvironment(false);
		application.setBannerMode(Banner.Mode.OFF);
		this.context = application.run();
		verify(application, never()).printBanner((Environment) anyObject());
	}

	@SuppressWarnings("deprecation")
	@Test
	public void disableBannerWithBoolean() throws Exception {
		SpringApplication application = spy(new SpringApplication(ExampleConfig.class));
		application.setWebEnvironment(false);
		application.setShowBanner(false);
		this.context = application.run();
		verify(application, never()).printBanner((Environment) anyObject());
	}

	@Test
	public void disableBannerViaShowBannerProperty() throws Exception {
		SpringApplication application = spy(new SpringApplication(ExampleConfig.class));
		application.setWebEnvironment(false);
		this.context = application.run("--spring.main.show_banner=false");
		verify(application, never()).printBanner((Environment) anyObject());
	}

	@Test
	public void disableBannerViaBannerModeProperty() throws Exception {
		SpringApplication application = spy(new SpringApplication(ExampleConfig.class));
		application.setWebEnvironment(false);
		this.context = application.run("--spring.main.banner-mode=off");
		verify(application, never()).printBanner((Environment) anyObject());
	}

	@Test
	public void customBanner() throws Exception {
		SpringApplication application = spy(new SpringApplication(ExampleConfig.class));
		application.setWebEnvironment(false);
		this.context = application.run("--banner.location=classpath:test-banner.txt");
		assertThat(this.output.toString(), startsWith("Running a Test!"));
	}

	@Test
	public void customBannerWithProperties() throws Exception {
		SpringApplication application = spy(new SpringApplication(ExampleConfig.class));
		application.setWebEnvironment(false);
		this.context = application.run(
				"--banner.location=classpath:test-banner-with-placeholder.txt",
				"--test.property=123456");
		assertThat(this.output.toString(),
				startsWith(String.format("Running a Test!%n%n123456")));
	}

	@Test
	public void logsNoActiveProfiles() throws Exception {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebEnvironment(false);
		this.context = application.run();
		assertThat(this.output.toString(), containsString("No profiles are active"));
	}

	@Test
	public void logsActiveProfiles() throws Exception {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebEnvironment(false);
		this.context = application.run("--spring.profiles.active=myprofiles");
		assertThat(this.output.toString(),
				containsString("The following profiles are active: myprofile"));
	}

	@Test
	public void enableBannerInLogViaProperty() throws Exception {
		SpringApplication application = spy(new SpringApplication(ExampleConfig.class));
		application.setWebEnvironment(false);
		this.context = application.run("--spring.main.banner-mode=log");
		verify(application, atLeastOnce()).setBannerMode(Banner.Mode.LOG);
		assertThat(this.output.toString(), containsString("o.s.boot.SpringApplication"));
	}

	@Test
	public void customId() throws Exception {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebEnvironment(false);
		this.context = application.run("--spring.application.name=foo");
		assertThat(this.context.getId(), startsWith("foo"));
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
		application.setInitializers(Arrays.asList(
				new ApplicationContextInitializer<ConfigurableApplicationContext>() {
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
	public void applicationRunningEventListener() {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebEnvironment(false);
		final AtomicReference<SpringApplication> reference = new AtomicReference<SpringApplication>();
		class ApplicationReadyEventListener
				implements ApplicationListener<ApplicationReadyEvent> {
			@Override
			public void onApplicationEvent(ApplicationReadyEvent event) {
				reference.set(event.getSpringApplication());
			}
		}
		application.addListeners(new ApplicationReadyEventListener());
		this.context = application.run("--foo=bar");
		assertThat(application, sameInstance(reference.get()));
	}

	@Test
	public void contextRefreshedEventListener() throws Exception {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebEnvironment(false);
		final AtomicReference<ApplicationContext> reference = new AtomicReference<ApplicationContext>();
		class InitializerListener implements ApplicationListener<ContextRefreshedEvent> {
			@Override
			public void onApplicationEvent(ContextRefreshedEvent event) {
				reference.set(event.getApplicationContext());
			}
		}
		application.setListeners(Arrays.asList(new InitializerListener()));
		this.context = application.run("--foo=bar");
		assertThat(this.context, sameInstance(reference.get()));
		// Custom initializers do not switch off the defaults
		assertThat(getEnvironment().getProperty("foo"), equalTo("bar"));
	}

	@Test
	public void eventsOrder() {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebEnvironment(false);
		final List<ApplicationEvent> events = new ArrayList<ApplicationEvent>();
		class ApplicationRunningEventListener
				implements ApplicationListener<ApplicationEvent> {
			@Override
			public void onApplicationEvent(ApplicationEvent event) {
				events.add((event));
			}
		}
		application.addListeners(new ApplicationRunningEventListener());
		this.context = application.run();
		assertThat(5, is(events.size()));
		assertThat(events.get(0), is(instanceOf(ApplicationStartedEvent.class)));
		assertThat(events.get(1),
				is(instanceOf(ApplicationEnvironmentPreparedEvent.class)));
		assertThat(events.get(2), is(instanceOf(ApplicationPreparedEvent.class)));
		assertThat(events.get(3), is(instanceOf(ContextRefreshedEvent.class)));
		assertThat(events.get(4), is(instanceOf(ApplicationReadyEvent.class)));
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
		TestSpringApplication application = new TestSpringApplication(
				ExampleConfig.class);
		application.setWebEnvironment(false);
		ConfigurableEnvironment environment = new StandardEnvironment();
		application.setEnvironment(environment);
		this.context = application.run();
		verify(application.getLoader()).setEnvironment(environment);
	}

	@Test
	public void customResourceLoader() throws Exception {
		TestSpringApplication application = new TestSpringApplication(
				ExampleConfig.class);
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
		this.context = application.run("--foo=bar");
		assertTrue(hasPropertySource(environment, CommandLinePropertySource.class,
				"commandLineArgs"));
	}

	@Test
	public void commandLinePropertySourceEnhancesEnvironment() throws Exception {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebEnvironment(false);
		ConfigurableEnvironment environment = new StandardEnvironment();
		environment.getPropertySources().addFirst(new MapPropertySource("commandLineArgs",
				Collections.<String, Object>singletonMap("foo", "original")));
		application.setEnvironment(environment);
		this.context = application.run("--foo=bar", "--bar=foo");
		assertTrue(hasPropertySource(environment, CompositePropertySource.class,
				"commandLineArgs"));
		assertEquals("foo", environment.getProperty("bar"));
		// New command line properties take precedence
		assertEquals("bar", environment.getProperty("foo"));
	}

	@Test
	public void propertiesFileEnhancesEnvironment() throws Exception {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebEnvironment(false);
		ConfigurableEnvironment environment = new StandardEnvironment();
		application.setEnvironment(environment);
		this.context = application.run();
		assertEquals("bucket", environment.getProperty("foo"));
	}

	@Test
	public void addProfiles() throws Exception {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebEnvironment(false);
		application.setAdditionalProfiles("foo");
		ConfigurableEnvironment environment = new StandardEnvironment();
		application.setEnvironment(environment);
		this.context = application.run();
		assertTrue(environment.acceptsProfiles("foo"));
	}

	@Test
	public void addProfilesOrder() throws Exception {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebEnvironment(false);
		application.setAdditionalProfiles("foo");
		ConfigurableEnvironment environment = new StandardEnvironment();
		application.setEnvironment(environment);
		this.context = application.run("--spring.profiles.active=bar,spam");
		// Command line should always come last
		assertArrayEquals(new String[] { "foo", "bar", "spam" },
				environment.getActiveProfiles());
	}

	@Test
	public void addProfilesOrderWithProperties() throws Exception {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebEnvironment(false);
		application.setAdditionalProfiles("other");
		ConfigurableEnvironment environment = new StandardEnvironment();
		application.setEnvironment(environment);
		this.context = application.run();
		// Active profile should win over default
		assertEquals("fromotherpropertiesfile", environment.getProperty("my.property"));
	}

	@Test
	public void emptyCommandLinePropertySourceNotAdded() throws Exception {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebEnvironment(false);
		ConfigurableEnvironment environment = new StandardEnvironment();
		application.setEnvironment(environment);
		this.context = application.run();
		assertEquals("bucket", environment.getProperty("foo"));
	}

	@Test
	public void disableCommandLinePropertySource() throws Exception {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebEnvironment(false);
		application.setAddCommandLineProperties(false);
		ConfigurableEnvironment environment = new StandardEnvironment();
		application.setEnvironment(environment);
		this.context = application.run("--foo=bar");
		assertFalse(
				hasPropertySource(environment, PropertySource.class, "commandLineArgs"));
	}

	@Test
	public void runCommandLineRunnersAndApplicationRunners() throws Exception {
		SpringApplication application = new SpringApplication(CommandLineRunConfig.class);
		application.setWebEnvironment(false);
		this.context = application.run("arg");
		assertTrue(this.context.getBean("runnerA", TestCommandLineRunner.class).hasRun());
		assertTrue(this.context.getBean("runnerB", TestApplicationRunner.class).hasRun());
		assertTrue(this.context.getBean("runnerC", TestCommandLineRunner.class).hasRun());
	}

	@Test
	public void loadSources() throws Exception {
		Object[] sources = { ExampleConfig.class, "a", TestCommandLineRunner.class };
		TestSpringApplication application = new TestSpringApplication(sources);
		application.setWebEnvironment(false);
		application.setUseMockLoader(true);
		this.context = application.run();
		Set<Object> initialSources = application.getSources();
		assertThat(initialSources.toArray(), equalTo(sources));
	}

	@Test
	public void wildcardSources() {
		Object[] sources = {
				"classpath:org/springframework/boot/sample-${sample.app.test.prop}.xml" };
		TestSpringApplication application = new TestSpringApplication(sources);
		application.setWebEnvironment(false);
		this.context = application.run();
	}

	@Test
	public void run() throws Exception {
		this.context = SpringApplication.run(ExampleWebConfig.class);
		assertNotNull(this.context);
	}

	@Test
	public void runComponents() throws Exception {
		this.context = SpringApplication.run(
				new Object[] { ExampleWebConfig.class, Object.class }, new String[0]);
		assertNotNull(this.context);
	}

	@Test
	public void exit() throws Exception {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebEnvironment(false);
		this.context = application.run();
		assertNotNull(this.context);
		assertEquals(0, SpringApplication.exit(this.context));
	}

	@Test
	public void exitWithExplicitCode() throws Exception {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebEnvironment(false);
		this.context = application.run();
		assertNotNull(this.context);
		assertEquals(2, SpringApplication.exit(this.context, new ExitCodeGenerator() {
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
		TestSpringApplication application = new TestSpringApplication(
				ExampleConfig.class);
		application.setWebEnvironment(false);
		this.context = application.run("--spring.main.banner-mode=OFF");
		assertThat(application.getBannerMode(), is(Banner.Mode.OFF));
	}

	@Test
	public void registerShutdownHook() throws Exception {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setApplicationContextClass(SpyApplicationContext.class);
		this.context = application.run();
		SpyApplicationContext applicationContext = (SpyApplicationContext) this.context;
		verify(applicationContext.getApplicationContext()).registerShutdownHook();
	}

	@Test
	public void registerListener() throws Exception {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setApplicationContextClass(SpyApplicationContext.class);
		final LinkedHashSet<ApplicationEvent> events = new LinkedHashSet<ApplicationEvent>();
		application.addListeners(new ApplicationListener<ApplicationEvent>() {
			@Override
			public void onApplicationEvent(ApplicationEvent event) {
				events.add(event);
			}
		});
		this.context = application.run();
		assertThat(events, hasItem(isA(ApplicationPreparedEvent.class)));
		assertThat(events, hasItem(isA(ContextRefreshedEvent.class)));
	}

	@Test
	public void registerListenerWithCustomMulticaster() throws Exception {
		SpringApplication application = new SpringApplication(ExampleConfig.class,
				Multicaster.class);
		application.setApplicationContextClass(SpyApplicationContext.class);
		final LinkedHashSet<ApplicationEvent> events = new LinkedHashSet<ApplicationEvent>();
		application.addListeners(new ApplicationListener<ApplicationEvent>() {
			@Override
			public void onApplicationEvent(ApplicationEvent event) {
				events.add(event);
			}
		});
		this.context = application.run();
		assertThat(events, hasItem(isA(ApplicationPreparedEvent.class)));
		assertThat(events, hasItem(isA(ContextRefreshedEvent.class)));
	}

	@Test
	public void registerShutdownHookOff() throws Exception {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setApplicationContextClass(SpyApplicationContext.class);
		application.setRegisterShutdownHook(false);
		this.context = application.run();
		SpyApplicationContext applicationContext = (SpyApplicationContext) this.context;
		verify(applicationContext.getApplicationContext(), never())
				.registerShutdownHook();
	}

	@Test
	public void headless() throws Exception {
		TestSpringApplication application = new TestSpringApplication(
				ExampleConfig.class);
		application.setWebEnvironment(false);
		this.context = application.run();
		assertThat(System.getProperty("java.awt.headless"), equalTo("true"));
	}

	@Test
	public void headlessFalse() throws Exception {
		TestSpringApplication application = new TestSpringApplication(
				ExampleConfig.class);
		application.setWebEnvironment(false);
		application.setHeadless(false);
		this.context = application.run();
		assertThat(System.getProperty("java.awt.headless"), equalTo("false"));
	}

	@Test
	public void headlessSystemPropertyTakesPrecedence() throws Exception {
		System.setProperty("java.awt.headless", "false");
		TestSpringApplication application = new TestSpringApplication(
				ExampleConfig.class);
		application.setWebEnvironment(false);
		this.context = application.run();
		assertThat(System.getProperty("java.awt.headless"), equalTo("false"));
	}

	@Test
	public void getApplicationArgumentsBean() throws Exception {
		TestSpringApplication application = new TestSpringApplication(
				ExampleConfig.class);
		application.setWebEnvironment(false);
		this.context = application.run("--debug", "spring", "boot");
		ApplicationArguments args = this.context.getBean(ApplicationArguments.class);
		assertThat(args.getNonOptionArgs(), equalTo(Arrays.asList("spring", "boot")));
		assertThat(args.containsOption("debug"), equalTo(true));
	}

	@Test
	public void webEnvironmentSwitchedOffInListener() throws Exception {
		TestSpringApplication application = new TestSpringApplication(
				ExampleConfig.class);
		application.addListeners(
				new ApplicationListener<ApplicationEnvironmentPreparedEvent>() {

					@Override
					public void onApplicationEvent(
							ApplicationEnvironmentPreparedEvent event) {
						assertTrue(event
								.getEnvironment() instanceof StandardServletEnvironment);
						EnvironmentTestUtils.addEnvironment(event.getEnvironment(),
								"foo=bar");
						event.getSpringApplication().setWebEnvironment(false);
					}

				});
		this.context = application.run();
		assertFalse(this.context.getEnvironment() instanceof StandardServletEnvironment);
		assertEquals("bar", this.context.getEnvironment().getProperty("foo"));
		assertEquals("test", this.context.getEnvironment().getPropertySources().iterator()
				.next().getName());
	}

	@Test
	public void failureResultsInSingleStackTrace() throws Exception {
		ThreadGroup group = new ThreadGroup("main");
		Thread thread = new Thread(group, "main") {
			@Override
			public void run() {
				SpringApplication application = new SpringApplication(
						FailingConfig.class);
				application.setWebEnvironment(false);
				application.run();
			};
		};
		thread.start();
		thread.join(6000);
		int occurrences = StringUtils.countOccurrencesOf(this.output.toString(),
				"Caused by: java.lang.RuntimeException: ExpectedError");
		assertThat("Expected single stacktrace", occurrences, equalTo(1));
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

	@Configuration
	protected static class InaccessibleConfiguration {

		private InaccessibleConfiguration() {
		}

	}

	public static class SpyApplicationContext extends AnnotationConfigApplicationContext {

		ConfigurableApplicationContext applicationContext = spy(
				new AnnotationConfigApplicationContext());

		@Override
		public void registerShutdownHook() {
			this.applicationContext.registerShutdownHook();
		}

		public ConfigurableApplicationContext getApplicationContext() {
			return this.applicationContext;
		}

		@Override
		public void close() {
			this.applicationContext.close();
		}

	}

	private static class TestSpringApplication extends SpringApplication {

		private BeanDefinitionLoader loader;

		private boolean useMockLoader;

		private Banner.Mode bannerMode;

		TestSpringApplication(Object... sources) {
			super(sources);
		}

		TestSpringApplication(ResourceLoader resourceLoader, Object... sources) {
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
		public void setBannerMode(Banner.Mode bannerMode) {
			super.setBannerMode(bannerMode);
			this.bannerMode = bannerMode;
		}

		public Banner.Mode getBannerMode() {
			return this.bannerMode;
		}

	}

	@Configuration
	static class ExampleConfig {

	}

	@Configuration
	static class Multicaster {

		@Bean
		public SimpleApplicationEventMulticaster applicationEventMulticaster() {
			return new SimpleApplicationEventMulticaster();
		}

	}

	@Configuration
	static class ExampleWebConfig {

		@Bean
		public JettyEmbeddedServletContainerFactory container() {
			return new JettyEmbeddedServletContainerFactory(0);
		}

	}

	@Configuration
	static class FailingConfig {

		@Bean
		public Object fail() {
			throw new RuntimeException("ExpectedError");
		}

	}

	@Configuration
	static class CommandLineRunConfig {

		@Bean
		public TestCommandLineRunner runnerC() {
			return new TestCommandLineRunner(Ordered.LOWEST_PRECEDENCE, "runnerB",
					"runnerA");
		}

		@Bean
		public TestApplicationRunner runnerB() {
			return new TestApplicationRunner(Ordered.LOWEST_PRECEDENCE - 1, "runnerA");
		}

		@Bean
		public TestCommandLineRunner runnerA() {
			return new TestCommandLineRunner(Ordered.HIGHEST_PRECEDENCE);
		}
	}

	static class AbstractTestRunner implements ApplicationContextAware, Ordered {

		private final String[] expectedBefore;

		private ApplicationContext applicationContext;

		private final int order;

		private boolean run;

		AbstractTestRunner(int order, String... expectedBefore) {
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

		public void markAsRan() {
			this.run = true;
			for (String name : this.expectedBefore) {
				AbstractTestRunner bean = this.applicationContext.getBean(name,
						AbstractTestRunner.class);
				assertTrue(bean.hasRun());
			}
		}

		public boolean hasRun() {
			return this.run;
		}

	}

	private static class TestCommandLineRunner extends AbstractTestRunner
			implements CommandLineRunner {

		TestCommandLineRunner(int order, String... expectedBefore) {
			super(order, expectedBefore);
		}

		@Override
		public void run(String... args) {
			markAsRan();
		}

	}

	private static class TestApplicationRunner extends AbstractTestRunner
			implements ApplicationRunner {

		TestApplicationRunner(int order, String... expectedBefore) {
			super(order, expectedBefore);
		}

		@Override
		public void run(ApplicationArguments args) {
			markAsRan();
		}

	}

}
