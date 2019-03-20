/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.PostConstruct;

import org.assertj.core.api.Condition;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.DefaultBeanNameGenerator;
import org.springframework.boot.context.embedded.AnnotationConfigEmbeddedWebApplicationContext;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.ApplicationStartingEvent;
import org.springframework.boot.testutil.InternalOutputCapture;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.env.CommandLinePropertySource;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.support.StandardServletEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.isA;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Tests for {@link SpringApplication}.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Andy Wilkinson
 * @author Christian Dupuis
 * @author Stephane Nicoll
 * @author Jeremy Rickard
 * @author Craig Burke
 */
public class SpringApplicationTests {

	private String headlessProperty;

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Rule
	public InternalOutputCapture output = new InternalOutputCapture();

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
	public void customBanner() throws Exception {
		SpringApplication application = spy(new SpringApplication(ExampleConfig.class));
		application.setWebEnvironment(false);
		this.context = application.run("--banner.location=classpath:test-banner.txt");
		assertThat(this.output.toString()).startsWith("Running a Test!");
	}

	@Test
	public void customBannerWithProperties() throws Exception {
		SpringApplication application = spy(new SpringApplication(ExampleConfig.class));
		application.setWebEnvironment(false);
		this.context = application.run(
				"--banner.location=classpath:test-banner-with-placeholder.txt",
				"--test.property=123456");
		assertThat(this.output.toString()).containsPattern("Running a Test!\\s+123456");
	}

	@Test
	public void imageBannerAndTextBanner() throws Exception {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		MockResourceLoader resourceLoader = new MockResourceLoader();
		resourceLoader.addResource("banner.gif", "black-and-white.gif");
		resourceLoader.addResource("banner.txt", "foobar.txt");
		application.setWebEnvironment(false);
		application.setResourceLoader(resourceLoader);
		application.run();
		assertThat(this.output.toString()).contains("@@@@").contains("Foo Bar");
	}

	@Test
	public void imageBannerLoads() throws Exception {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		MockResourceLoader resourceLoader = new MockResourceLoader();
		resourceLoader.addResource("banner.gif", "black-and-white.gif");
		application.setWebEnvironment(false);
		application.setResourceLoader(resourceLoader);
		application.run();
		assertThat(this.output.toString()).contains("@@@@@@");
	}

	@Test
	public void logsNoActiveProfiles() throws Exception {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebEnvironment(false);
		this.context = application.run();
		assertThat(this.output.toString()).contains(
				"No active profile set, falling back to default profiles: default");
	}

	@Test
	public void logsActiveProfiles() throws Exception {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebEnvironment(false);
		this.context = application.run("--spring.profiles.active=myprofiles");
		assertThat(this.output.toString())
				.contains("The following profiles are active: myprofile");
	}

	@Test
	public void enableBannerInLogViaProperty() throws Exception {
		SpringApplication application = spy(new SpringApplication(ExampleConfig.class));
		application.setWebEnvironment(false);
		this.context = application.run("--spring.main.banner-mode=log");
		verify(application, atLeastOnce()).setBannerMode(Banner.Mode.LOG);
		assertThat(this.output.toString()).contains("o.s.boot.SpringApplication");
	}

	@Test
	public void customId() throws Exception {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebEnvironment(false);
		this.context = application.run("--spring.application.name=foo");
		assertThat(this.context.getId()).startsWith("foo");
	}

	@Test
	public void specificApplicationContextClass() throws Exception {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setApplicationContextClass(StaticApplicationContext.class);
		this.context = application.run();
		assertThat(this.context).isInstanceOf(StaticApplicationContext.class);
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
		assertThat(this.context).isSameAs(reference.get());
		// Custom initializers do not switch off the defaults
		assertThat(getEnvironment().getProperty("foo")).isEqualTo("bar");
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
		assertThat(application).isSameAs(reference.get());
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
		assertThat(this.context).isSameAs(reference.get());
		// Custom initializers do not switch off the defaults
		assertThat(getEnvironment().getProperty("foo")).isEqualTo("bar");
	}

	@Test
	@SuppressWarnings("deprecation")
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
		assertThat(events).hasSize(5);
		assertThat(events.get(0)).isInstanceOf(
				org.springframework.boot.context.event.ApplicationStartedEvent.class);
		assertThat(events.get(0)).isInstanceOf(ApplicationStartingEvent.class);
		assertThat(events.get(1)).isInstanceOf(ApplicationEnvironmentPreparedEvent.class);
		assertThat(events.get(2)).isInstanceOf(ApplicationPreparedEvent.class);
		assertThat(events.get(3)).isInstanceOf(ContextRefreshedEvent.class);
		assertThat(events.get(4)).isInstanceOf(ApplicationReadyEvent.class);
	}

	@Test
	public void defaultApplicationContext() throws Exception {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebEnvironment(false);
		this.context = application.run();
		assertThat(this.context).isInstanceOf(AnnotationConfigApplicationContext.class);
	}

	@Test
	public void defaultApplicationContextForWeb() throws Exception {
		SpringApplication application = new SpringApplication(ExampleWebConfig.class);
		application.setWebEnvironment(true);
		this.context = application.run();
		assertThat(this.context)
				.isInstanceOf(AnnotationConfigEmbeddedWebApplicationContext.class);
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
		Object actualGenerator = this.context
				.getBean(AnnotationConfigUtils.CONFIGURATION_BEAN_NAME_GENERATOR);
		assertThat(actualGenerator).isSameAs(beanNameGenerator);
	}

	@Test
	public void customBeanNameGeneratorWithNonWebApplication() throws Exception {
		TestSpringApplication application = new TestSpringApplication(
				ExampleWebConfig.class);
		application.setWebEnvironment(false);
		BeanNameGenerator beanNameGenerator = new DefaultBeanNameGenerator();
		application.setBeanNameGenerator(beanNameGenerator);
		this.context = application.run();
		verify(application.getLoader()).setBeanNameGenerator(beanNameGenerator);
		Object actualGenerator = this.context
				.getBean(AnnotationConfigUtils.CONFIGURATION_BEAN_NAME_GENERATOR);
		assertThat(actualGenerator).isSameAs(beanNameGenerator);
	}

	@Test
	public void commandLinePropertySource() throws Exception {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebEnvironment(false);
		ConfigurableEnvironment environment = new StandardEnvironment();
		application.setEnvironment(environment);
		this.context = application.run("--foo=bar");
		assertThat(environment).has(matchingPropertySource(
				CommandLinePropertySource.class, "commandLineArgs"));
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
		assertThat(environment).has(
				matchingPropertySource(CompositePropertySource.class, "commandLineArgs"));
		assertThat(environment.getProperty("bar")).isEqualTo("foo");
		// New command line properties take precedence
		assertThat(environment.getProperty("foo")).isEqualTo("bar");
	}

	@Test
	public void propertiesFileEnhancesEnvironment() throws Exception {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebEnvironment(false);
		ConfigurableEnvironment environment = new StandardEnvironment();
		application.setEnvironment(environment);
		this.context = application.run();
		assertThat(environment.getProperty("foo")).isEqualTo("bucket");
	}

	@Test
	public void addProfiles() throws Exception {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebEnvironment(false);
		application.setAdditionalProfiles("foo");
		ConfigurableEnvironment environment = new StandardEnvironment();
		application.setEnvironment(environment);
		this.context = application.run();
		assertThat(environment.acceptsProfiles("foo")).isTrue();
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
		assertThat(environment.getActiveProfiles()).containsExactly("foo", "bar", "spam");
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
		assertThat(environment.getProperty("my.property"))
				.isEqualTo("fromotherpropertiesfile");
	}

	@Test
	public void emptyCommandLinePropertySourceNotAdded() throws Exception {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebEnvironment(false);
		ConfigurableEnvironment environment = new StandardEnvironment();
		application.setEnvironment(environment);
		this.context = application.run();
		assertThat(environment.getProperty("foo")).isEqualTo("bucket");
	}

	@Test
	public void disableCommandLinePropertySource() throws Exception {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebEnvironment(false);
		application.setAddCommandLineProperties(false);
		ConfigurableEnvironment environment = new StandardEnvironment();
		application.setEnvironment(environment);
		this.context = application.run("--foo=bar");
		assertThat(environment).doesNotHave(
				matchingPropertySource(PropertySource.class, "commandLineArgs"));
	}

	@Test
	public void runCommandLineRunnersAndApplicationRunners() throws Exception {
		SpringApplication application = new SpringApplication(CommandLineRunConfig.class);
		application.setWebEnvironment(false);
		this.context = application.run("arg");
		assertThat(this.context).has(runTestRunnerBean("runnerA"));
		assertThat(this.context).has(runTestRunnerBean("runnerB"));
		assertThat(this.context).has(runTestRunnerBean("runnerC"));
	}

	@Test
	public void loadSources() throws Exception {
		Object[] sources = { ExampleConfig.class, "a", TestCommandLineRunner.class };
		TestSpringApplication application = new TestSpringApplication(sources);
		application.setWebEnvironment(false);
		application.setUseMockLoader(true);
		this.context = application.run();
		Set<Object> initialSources = application.getSources();
		assertThat(initialSources.toArray()).isEqualTo(sources);
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
		assertThat(this.context).isNotNull();
	}

	@Test
	public void runComponents() throws Exception {
		this.context = SpringApplication.run(
				new Object[] { ExampleWebConfig.class, Object.class }, new String[0]);
		assertThat(this.context).isNotNull();
	}

	@Test
	public void exit() throws Exception {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebEnvironment(false);
		this.context = application.run();
		assertThat(this.context).isNotNull();
		assertThat(SpringApplication.exit(this.context)).isEqualTo(0);
	}

	@Test
	public void exitWithExplicitCode() throws Exception {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		ExitCodeListener listener = new ExitCodeListener();
		application.addListeners(listener);
		application.setWebEnvironment(false);
		this.context = application.run();
		assertThat(this.context).isNotNull();
		assertThat(SpringApplication.exit(this.context, new ExitCodeGenerator() {

			@Override
			public int getExitCode() {
				return 2;
			}

		})).isEqualTo(2);
		assertThat(listener.getExitCode()).isEqualTo(2);
	}

	@Test
	public void exitWithExplicitCodeFromException() throws Exception {
		final SpringBootExceptionHandler handler = mock(SpringBootExceptionHandler.class);
		SpringApplication application = new SpringApplication(
				ExitCodeCommandLineRunConfig.class) {

			@Override
			SpringBootExceptionHandler getSpringBootExceptionHandler() {
				return handler;
			}

		};
		ExitCodeListener listener = new ExitCodeListener();
		application.addListeners(listener);
		application.setWebEnvironment(false);
		try {
			application.run();
			fail("Did not throw");
		}
		catch (IllegalStateException ex) {
		}
		verify(handler).registerExitCode(11);
		assertThat(listener.getExitCode()).isEqualTo(11);
	}

	@Test
	public void exitWithExplicitCodeFromMappedException() throws Exception {
		final SpringBootExceptionHandler handler = mock(SpringBootExceptionHandler.class);
		SpringApplication application = new SpringApplication(
				MappedExitCodeCommandLineRunConfig.class) {

			@Override
			SpringBootExceptionHandler getSpringBootExceptionHandler() {
				return handler;
			}

		};
		ExitCodeListener listener = new ExitCodeListener();
		application.addListeners(listener);
		application.setWebEnvironment(false);
		try {
			application.run();
			fail("Did not throw");
		}
		catch (IllegalStateException ex) {
		}
		verify(handler).registerExitCode(11);
		assertThat(listener.getExitCode()).isEqualTo(11);
	}

	@Test
	public void exceptionFromRefreshIsHandledGracefully() throws Exception {
		final SpringBootExceptionHandler handler = mock(SpringBootExceptionHandler.class);
		SpringApplication application = new SpringApplication(
				RefreshFailureConfig.class) {

			@Override
			SpringBootExceptionHandler getSpringBootExceptionHandler() {
				return handler;
			}

		};
		ExitCodeListener listener = new ExitCodeListener();
		application.addListeners(listener);
		application.setWebEnvironment(false);
		try {
			application.run();
			fail("Did not throw");
		}
		catch (RuntimeException ex) {
		}
		verify(handler).registerLoggedException(any(RefreshFailureException.class));
		assertThat(this.output.toString()).doesNotContain("NullPointerException");
	}

	@Test
	public void defaultCommandLineArgs() throws Exception {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setDefaultProperties(StringUtils.splitArrayElementsIntoProperties(
				new String[] { "baz=", "bar=spam" }, "="));
		application.setWebEnvironment(false);
		this.context = application.run("--bar=foo", "bucket", "crap");
		assertThat(this.context).isInstanceOf(AnnotationConfigApplicationContext.class);
		assertThat(getEnvironment().getProperty("bar")).isEqualTo("foo");
		assertThat(getEnvironment().getProperty("baz")).isEqualTo("");
	}

	@Test
	public void commandLineArgsApplyToSpringApplication() throws Exception {
		TestSpringApplication application = new TestSpringApplication(
				ExampleConfig.class);
		application.setWebEnvironment(false);
		this.context = application.run("--spring.main.banner-mode=OFF");
		assertThat(application.getBannerMode()).isEqualTo(Banner.Mode.OFF);
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
		SpringApplication application = new SpringApplication(ExampleConfig.class,
				ListenerConfig.class);
		application.setApplicationContextClass(SpyApplicationContext.class);
		final LinkedHashSet<ApplicationEvent> events = new LinkedHashSet<ApplicationEvent>();
		application.addListeners(new ApplicationListener<ApplicationEvent>() {

			@Override
			public void onApplicationEvent(ApplicationEvent event) {
				events.add(event);
			}

		});
		this.context = application.run();
		assertThat(events).hasAtLeastOneElementOfType(ApplicationPreparedEvent.class);
		assertThat(events).hasAtLeastOneElementOfType(ContextRefreshedEvent.class);
		verifyTestListenerEvents();
	}

	@Test
	public void registerListenerWithCustomMulticaster() throws Exception {
		SpringApplication application = new SpringApplication(ExampleConfig.class,
				ListenerConfig.class, Multicaster.class);
		application.setApplicationContextClass(SpyApplicationContext.class);
		final LinkedHashSet<ApplicationEvent> events = new LinkedHashSet<ApplicationEvent>();
		application.addListeners(new ApplicationListener<ApplicationEvent>() {

			@Override
			public void onApplicationEvent(ApplicationEvent event) {
				events.add(event);
			}

		});
		this.context = application.run();
		assertThat(events).hasAtLeastOneElementOfType(ApplicationPreparedEvent.class);
		assertThat(events).hasAtLeastOneElementOfType(ContextRefreshedEvent.class);
		verifyTestListenerEvents();
	}

	@SuppressWarnings("unchecked")
	private void verifyTestListenerEvents() {
		ApplicationListener<ApplicationEvent> listener = this.context
				.getBean("testApplicationListener", ApplicationListener.class);
		verifyListenerEvents(listener, ContextRefreshedEvent.class,
				ApplicationReadyEvent.class);
	}

	@SuppressWarnings("unchecked")
	private void verifyListenerEvents(ApplicationListener<ApplicationEvent> listener,
			Class<? extends ApplicationEvent>... eventTypes) {
		for (Class<? extends ApplicationEvent> eventType : eventTypes) {
			verify(listener).onApplicationEvent(argThat(isA(eventType)));
		}
		verifyNoMoreInteractions(listener);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void applicationListenerFromApplicationIsCalledWhenContextFailsRefreshBeforeListenerRegistration() {
		ApplicationListener<ApplicationEvent> listener = mock(ApplicationListener.class);
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.addListeners(listener);
		try {
			application.run();
			fail("Run should have failed with an ApplicationContextException");
		}
		catch (ApplicationContextException ex) {
			verifyListenerEvents(listener, ApplicationStartingEvent.class,
					ApplicationEnvironmentPreparedEvent.class,
					ApplicationPreparedEvent.class, ApplicationFailedEvent.class);
		}
	}

	@SuppressWarnings("unchecked")
	@Test
	public void applicationListenerFromApplicationIsCalledWhenContextFailsRefreshAfterListenerRegistration() {
		ApplicationListener<ApplicationEvent> listener = mock(ApplicationListener.class);
		SpringApplication application = new SpringApplication(
				BrokenPostConstructConfig.class);
		application.setWebEnvironment(false);
		application.addListeners(listener);
		try {
			application.run();
			fail("Run should have failed with a BeanCreationException");
		}
		catch (BeanCreationException ex) {
			verifyListenerEvents(listener, ApplicationStartingEvent.class,
					ApplicationEnvironmentPreparedEvent.class,
					ApplicationPreparedEvent.class, ApplicationFailedEvent.class);
		}
	}

	@SuppressWarnings("unchecked")
	@Test
	public void applicationListenerFromContextIsCalledWhenContextFailsRefreshBeforeListenerRegistration() {
		final ApplicationListener<ApplicationEvent> listener = mock(
				ApplicationListener.class);
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.addInitializers(
				new ApplicationContextInitializer<ConfigurableApplicationContext>() {

					@Override
					public void initialize(
							ConfigurableApplicationContext applicationContext) {
						applicationContext.addApplicationListener(listener);
					}

				});
		try {
			application.run();
			fail("Run should have failed with an ApplicationContextException");
		}
		catch (ApplicationContextException ex) {
			verifyListenerEvents(listener, ApplicationFailedEvent.class);
		}
	}

	@SuppressWarnings("unchecked")
	@Test
	public void applicationListenerFromContextIsCalledWhenContextFailsRefreshAfterListenerRegistration() {
		final ApplicationListener<ApplicationEvent> listener = mock(
				ApplicationListener.class);
		SpringApplication application = new SpringApplication(
				BrokenPostConstructConfig.class);
		application.setWebEnvironment(false);
		application.addInitializers(
				new ApplicationContextInitializer<ConfigurableApplicationContext>() {

					@Override
					public void initialize(
							ConfigurableApplicationContext applicationContext) {
						applicationContext.addApplicationListener(listener);
					}

				});
		try {
			application.run();
			fail("Run should have failed with a BeanCreationException");
		}
		catch (BeanCreationException ex) {
			verifyListenerEvents(listener, ApplicationFailedEvent.class);
		}
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
		assertThat(System.getProperty("java.awt.headless")).isEqualTo("true");
	}

	@Test
	public void headlessFalse() throws Exception {
		TestSpringApplication application = new TestSpringApplication(
				ExampleConfig.class);
		application.setWebEnvironment(false);
		application.setHeadless(false);
		this.context = application.run();
		assertThat(System.getProperty("java.awt.headless")).isEqualTo("false");
	}

	@Test
	public void headlessSystemPropertyTakesPrecedence() throws Exception {
		System.setProperty("java.awt.headless", "false");
		TestSpringApplication application = new TestSpringApplication(
				ExampleConfig.class);
		application.setWebEnvironment(false);
		this.context = application.run();
		assertThat(System.getProperty("java.awt.headless")).isEqualTo("false");
	}

	@Test
	public void getApplicationArgumentsBean() throws Exception {
		TestSpringApplication application = new TestSpringApplication(
				ExampleConfig.class);
		application.setWebEnvironment(false);
		this.context = application.run("--debug", "spring", "boot");
		ApplicationArguments args = this.context.getBean(ApplicationArguments.class);
		assertThat(args.getNonOptionArgs()).containsExactly("spring", "boot");
		assertThat(args.containsOption("debug")).isTrue();
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
						assertThat(event.getEnvironment())
								.isInstanceOf(StandardServletEnvironment.class);
						TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
								event.getEnvironment(), "foo=bar");
						event.getSpringApplication().setWebEnvironment(false);
					}

				});
		this.context = application.run();
		assertThat(this.context.getEnvironment())
				.isNotInstanceOf(StandardServletEnvironment.class);
		assertThat(this.context.getEnvironment().getProperty("foo")).isEqualTo("bar");
		assertThat(this.context.getEnvironment().getPropertySources().iterator().next()
				.getName()).isEqualTo(
						TestPropertySourceUtils.INLINED_PROPERTIES_PROPERTY_SOURCE_NAME);
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
		assertThat(occurrences).as("Expected single stacktrace").isEqualTo(1);
	}

	private Condition<ConfigurableEnvironment> matchingPropertySource(
			final Class<?> propertySourceClass, final String name) {
		return new Condition<ConfigurableEnvironment>("has property source") {

			@Override
			public boolean matches(ConfigurableEnvironment value) {
				for (PropertySource<?> source : value.getPropertySources()) {
					if (propertySourceClass.isInstance(source)
							&& (name == null || name.equals(source.getName()))) {
						return true;
					}
				}
				return false;
			}

		};
	}

	private Condition<ConfigurableApplicationContext> runTestRunnerBean(
			final String name) {
		return new Condition<ConfigurableApplicationContext>("run testrunner bean") {

			@Override
			public boolean matches(ConfigurableApplicationContext value) {
				return value.getBean(name, AbstractTestRunner.class).hasRun();
			}

		};
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
	static class BrokenPostConstructConfig {

		@Bean
		public Thing thing() {
			return new Thing();
		}

		static class Thing {

			@PostConstruct
			public void boom() {
				throw new IllegalStateException();
			}

		}

	}

	@Configuration
	static class ListenerConfig {

		@Bean
		public ApplicationListener<?> testApplicationListener() {
			return mock(ApplicationListener.class);
		}

	}

	@Configuration
	static class Multicaster {

		@Bean(name = AbstractApplicationContext.APPLICATION_EVENT_MULTICASTER_BEAN_NAME)
		public ApplicationEventMulticaster applicationEventMulticaster() {
			return spy(new SimpleApplicationEventMulticaster());
		}

	}

	@Configuration
	static class ExampleWebConfig {

		@Bean
		public TomcatEmbeddedServletContainerFactory container() {
			return new TomcatEmbeddedServletContainerFactory(0);
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

	@Configuration
	static class ExitCodeCommandLineRunConfig {

		@Bean
		public CommandLineRunner runner() {
			return new CommandLineRunner() {

				@Override
				public void run(String... args) throws Exception {
					throw new IllegalStateException(new ExitStatusException());
				}

			};
		}

	}

	@Configuration
	static class MappedExitCodeCommandLineRunConfig {

		@Bean
		public CommandLineRunner runner() {
			return new CommandLineRunner() {

				@Override
				public void run(String... args) throws Exception {
					throw new IllegalStateException();
				}

			};
		}

		@Bean
		public ExitCodeExceptionMapper exceptionMapper() {
			return new ExitCodeExceptionMapper() {

				@Override
				public int getExitCode(Throwable exception) {
					if (exception instanceof IllegalStateException) {
						return 11;
					}
					return 0;
				}

			};
		}

	}

	@Configuration
	static class RefreshFailureConfig {

		@PostConstruct
		public void fail() {
			throw new RefreshFailureException();
		}

	}

	static class ExitStatusException extends RuntimeException
			implements ExitCodeGenerator {

		@Override
		public int getExitCode() {
			return 11;
		}

	}

	static class RefreshFailureException extends RuntimeException {

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
				assertThat(bean.hasRun()).isTrue();
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

	private static class ExitCodeListener implements ApplicationListener<ExitCodeEvent> {

		private int exitCode;

		@Override
		public void onApplicationEvent(ExitCodeEvent event) {
			this.exitCode = event.getExitCode();
		}

		public int getExitCode() {
			return this.exitCode;
		}

	}

	private static class MockResourceLoader implements ResourceLoader {

		private final Map<String, Resource> resources = new HashMap<String, Resource>();

		public void addResource(String source, String path) {
			this.resources.put(source, new ClassPathResource(path, getClass()));
		}

		@Override
		public Resource getResource(String path) {
			Resource resource = this.resources.get(path);
			return (resource != null) ? resource : new ClassPathResource("doesnotexist");
		}

		@Override
		public ClassLoader getClassLoader() {
			return getClass().getClassLoader();
		}

	}

}
