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

package org.springframework.boot;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import jakarta.annotation.PostConstruct;
import org.assertj.core.api.Condition;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.ArgumentMatchers;
import org.mockito.InOrder;
import org.mockito.Mockito;

import org.springframework.aot.AotDetector;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanCurrentlyInCreationException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionOverrideException;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.DefaultBeanNameGenerator;
import org.springframework.boot.Banner.Mode;
import org.springframework.boot.BootstrapRegistry.InstanceSupplier;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.AvailabilityState;
import org.springframework.boot.availability.LivenessState;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.boot.builder.ParentContextApplicationContextInitializer;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationContextInitializedEvent;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.boot.context.event.ApplicationStartingEvent;
import org.springframework.boot.context.event.SpringApplicationEvent;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.testsupport.classpath.ForkedClassPath;
import org.springframework.boot.testsupport.classpath.resources.WithResource;
import org.springframework.boot.testsupport.system.CapturedOutput;
import org.springframework.boot.testsupport.system.OutputCaptureExtension;
import org.springframework.boot.web.context.reactive.ReactiveWebApplicationContext;
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
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.context.event.SmartApplicationListener;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.CommandLinePropertySource;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.Profiles;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.metrics.ApplicationStartup;
import org.springframework.core.metrics.StartupStep;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.context.ConfigurableWebEnvironment;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;

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
 * @author Madhura Bhave
 * @author Brian Clozel
 * @author Artsiom Yudovin
 * @author Marten Deinum
 * @author Nguyen Bao Sach
 * @author Chris Bono
 * @author Sebastien Deleuze
 * @author Moritz Halbritter
 * @author Tadaya Tsuyukubo
 * @author Yanming Zhou
 * @author Sijun Yang
 * @author Giheon Do
 */
@ExtendWith(OutputCaptureExtension.class)
class SpringApplicationTests {

	private String headlessProperty;

	private ConfigurableApplicationContext context;

	private Environment getEnvironment() {
		if (this.context != null) {
			return this.context.getEnvironment();
		}
		throw new IllegalStateException("Could not obtain Environment");
	}

	@BeforeEach
	void storeAndClearHeadlessProperty() {
		this.headlessProperty = System.getProperty("java.awt.headless");
		System.clearProperty("java.awt.headless");
	}

	@AfterEach
	void reinstateHeadlessProperty() {
		if (this.headlessProperty == null) {
			System.clearProperty("java.awt.headless");
		}
		else {
			System.setProperty("java.awt.headless", this.headlessProperty);
		}
	}

	@AfterEach
	void cleanUp() {
		if (this.context != null) {
			this.context.close();
		}
		System.clearProperty("spring.main.banner-mode");
		SpringApplicationShutdownHookInstance.reset();
	}

	@Test
	void sourcesMustNotBeNull() {
		assertThatIllegalArgumentException().isThrownBy(() -> new SpringApplication((Class<?>[]) null).run())
			.withMessageContaining("'primarySources' must not be null");
	}

	@Test
	void sourcesMustNotBeEmpty() {
		assertThatIllegalStateException().isThrownBy(() -> new SpringApplication().run())
			.withMessageContaining("No sources defined");
	}

	@Test
	void sourcesMustBeAccessible() {
		assertThatExceptionOfType(BeanDefinitionStoreException.class)
			.isThrownBy(() -> new SpringApplication(InaccessibleConfiguration.class).run())
			.havingRootCause()
			.isInstanceOf(IllegalArgumentException.class)
			.withMessageContaining("No visible constructors");
	}

	@Test
	@WithResource(name = "banner.txt", content = "Running a Test!")
	void customBanner(CapturedOutput output) {
		SpringApplication application = spy(new SpringApplication(ExampleConfig.class));
		application.setWebApplicationType(WebApplicationType.NONE);
		this.context = application.run();
		assertThat(output).startsWith("Running a Test!");

	}

	@Test
	@WithResource(name = "banner.txt", content = """
			Running a Test!

			${test.property}""")
	void customBannerWithProperties(CapturedOutput output) {
		SpringApplication application = spy(new SpringApplication(ExampleConfig.class));
		application.setWebApplicationType(WebApplicationType.NONE);
		this.context = application.run("--test.property=123456");
		assertThat(output).containsPattern("Running a Test!\\s+123456");
	}

	@Test
	void logsActiveProfilesWithoutProfileAndSingleDefault(CapturedOutput output) {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		this.context = application.run();
		assertThat(output).contains("No active profile set, falling back to 1 default profile: \"default\"");
	}

	@Test
	void logsActiveProfilesWithoutProfileAndMultipleDefaults(CapturedOutput output) {
		MockEnvironment environment = new MockEnvironment();
		environment.setDefaultProfiles("p0", "default");
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		application.setEnvironment(environment);
		this.context = application.run();
		assertThat(output).contains("No active profile set, falling back to 2 default profiles: \"p0\", \"default\"");
	}

	@Test
	void logsActiveProfilesWithSingleProfile(CapturedOutput output) {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		this.context = application.run("--spring.profiles.active=myprofiles");
		assertThat(output).contains("The following 1 profile is active: \"myprofiles\"");
	}

	@Test
	void logsActiveProfilesWithMultipleProfiles(CapturedOutput output) {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		application.setAdditionalProfiles("p1", "p2");
		application.run();
		assertThat(output).contains("The following 2 profiles are active: \"p1\", \"p2\"");
	}

	@Test
	void enableBannerInLogViaProperty(CapturedOutput output) {
		SpringApplication application = spy(new SpringApplication(ExampleConfig.class));
		application.setWebApplicationType(WebApplicationType.NONE);
		this.context = application.run("--spring.main.banner-mode=log");
		assertThatBannerModeIs(application, Banner.Mode.LOG);
		assertThat(output).contains("o.s.b.SpringApplication");
	}

	@Test
	@WithResource(name = "bindtoapplication.properties", content = "spring.main.banner-mode=off")
	void triggersConfigFileApplicationListenerBeforeBinding() {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		this.context = application.run("--spring.config.name=bindtoapplication");
		assertThatBannerModeIs(application, Mode.OFF);
	}

	@Test
	void bindsSystemPropertyToSpringApplication() {
		System.setProperty("spring.main.banner-mode", "off");
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		this.context = application.run();
		assertThatBannerModeIs(application, Mode.OFF);
	}

	@Test
	void bindsYamlStyleBannerModeToSpringApplication() {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setDefaultProperties(Collections.singletonMap("spring.main.banner-mode", false));
		application.setWebApplicationType(WebApplicationType.NONE);
		this.context = application.run();
		assertThatBannerModeIs(application, Mode.OFF);
	}

	@Test
	void bindsBooleanAsStringBannerModeToSpringApplication() {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		this.context = application.run("--spring.main.banner-mode=false");
		assertThatBannerModeIs(application, Mode.OFF);
	}

	@Test
	void customId() {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		this.context = application.run("--spring.application.name=foo");
		assertThat(this.context.getId()).startsWith("foo");
	}

	@Test
	void specificApplicationContextFactory() {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application
			.setApplicationContextFactory(ApplicationContextFactory.ofContextClass(StaticApplicationContext.class));
		this.context = application.run();
		assertThat(this.context).isInstanceOf(StaticApplicationContext.class);
	}

	@Test
	void specificApplicationContextInitializer() {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		final AtomicReference<ApplicationContext> reference = new AtomicReference<>();
		application.setInitializers(Collections
			.singletonList((ApplicationContextInitializer<ConfigurableApplicationContext>) reference::set));
		this.context = application.run("--foo=bar");
		assertThat(this.context).isSameAs(reference.get());
		// Custom initializers do not switch off the defaults
		assertThat(getEnvironment().getProperty("foo")).isEqualTo("bar");
	}

	@Test
	void applicationRunningEventListener() {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		AtomicReference<ApplicationReadyEvent> reference = addListener(application, ApplicationReadyEvent.class);
		this.context = application.run("--foo=bar");
		assertThat(application).isSameAs(reference.get().getSpringApplication());
	}

	@Test
	void contextRefreshedEventListener() {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		AtomicReference<ContextRefreshedEvent> reference = addListener(application, ContextRefreshedEvent.class);
		this.context = application.run("--foo=bar");
		assertThat(this.context).isSameAs(reference.get().getApplicationContext());
		// Custom initializers do not switch off the defaults
		assertThat(getEnvironment().getProperty("foo")).isEqualTo("bar");
	}

	@Test
	@SuppressWarnings("unchecked")
	void eventsArePublishedInExpectedOrder() {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		ApplicationListener<ApplicationEvent> listener = mock(ApplicationListener.class);
		application.addListeners(listener);
		this.context = application.run();
		InOrder inOrder = Mockito.inOrder(listener);
		then(listener).should(inOrder).onApplicationEvent(isA(ApplicationStartingEvent.class));
		then(listener).should(inOrder).onApplicationEvent(isA(ApplicationEnvironmentPreparedEvent.class));
		then(listener).should(inOrder).onApplicationEvent(isA(ApplicationContextInitializedEvent.class));
		then(listener).should(inOrder).onApplicationEvent(isA(ApplicationPreparedEvent.class));
		then(listener).should(inOrder).onApplicationEvent(isA(ContextRefreshedEvent.class));
		then(listener).should(inOrder).onApplicationEvent(isA(ApplicationStartedEvent.class));
		then(listener).should(inOrder)
			.onApplicationEvent(argThat(isAvailabilityChangeEventWithState(LivenessState.CORRECT)));
		then(listener).should(inOrder).onApplicationEvent(isA(ApplicationReadyEvent.class));
		then(listener).should(inOrder)
			.onApplicationEvent(argThat(isAvailabilityChangeEventWithState(ReadinessState.ACCEPTING_TRAFFIC)));
		inOrder.verifyNoMoreInteractions();
	}

	@Test
	void applicationStartedEventHasStartedTime() {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		AtomicReference<ApplicationStartedEvent> reference = addListener(application, ApplicationStartedEvent.class);
		this.context = application.run();
		assertThat(reference.get()).isNotNull().extracting(ApplicationStartedEvent::getTimeTaken).isNotNull();
	}

	@Test
	void applicationReadyEventHasReadyTime() {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		AtomicReference<ApplicationReadyEvent> reference = addListener(application, ApplicationReadyEvent.class);
		this.context = application.run();
		assertThat(reference.get()).isNotNull().extracting(ApplicationReadyEvent::getTimeTaken).isNotNull();
	}

	@Test
	void defaultApplicationContext() {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		this.context = application.run();
		assertThat(this.context).isInstanceOf(AnnotationConfigApplicationContext.class);
	}

	@Test
	void customEnvironment() {
		TestSpringApplication application = new TestSpringApplication(ExampleConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		ConfigurableEnvironment environment = new StandardEnvironment();
		application.setEnvironment(environment);
		this.context = application.run();
		then(application.getLoader()).should().setEnvironment(environment);
	}

	@Test
	void customResourceLoader() {
		TestSpringApplication application = new TestSpringApplication(ExampleConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		ResourceLoader resourceLoader = new DefaultResourceLoader();
		application.setResourceLoader(resourceLoader);
		this.context = application.run();
		then(application.getLoader()).should().setResourceLoader(resourceLoader);
	}

	@Test
	void customResourceLoaderFromConstructor() {
		ResourceLoader resourceLoader = new DefaultResourceLoader();
		TestSpringApplication application = new TestSpringApplication(resourceLoader, ExampleConfig.class);
		this.context = application.run();
		then(application.getLoader()).should().setResourceLoader(resourceLoader);
	}

	@Test
	void customBeanNameGenerator() {
		TestSpringApplication application = new TestSpringApplication(ExampleConfig.class);
		BeanNameGenerator beanNameGenerator = new DefaultBeanNameGenerator();
		application.setBeanNameGenerator(beanNameGenerator);
		this.context = application.run();
		then(application.getLoader()).should().setBeanNameGenerator(beanNameGenerator);
		Object actualGenerator = this.context.getBean(AnnotationConfigUtils.CONFIGURATION_BEAN_NAME_GENERATOR);
		assertThat(actualGenerator).isSameAs(beanNameGenerator);
	}

	@Test
	void commandLinePropertySource() {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		ConfigurableEnvironment environment = new StandardEnvironment();
		application.setEnvironment(environment);
		this.context = application.run("--foo=bar");
		assertThat(environment).has(matchingPropertySource(CommandLinePropertySource.class, "commandLineArgs"));
	}

	@Test
	void commandLinePropertySourceEnhancesEnvironment() {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		ConfigurableEnvironment environment = new StandardEnvironment();
		environment.getPropertySources()
			.addFirst(new MapPropertySource("commandLineArgs", Collections.singletonMap("foo", "original")));
		application.setEnvironment(environment);
		this.context = application.run("--foo=bar", "--bar=foo");
		assertThat(environment).has(matchingPropertySource(CompositePropertySource.class, "commandLineArgs"));
		assertThat(environment.getProperty("bar")).isEqualTo("foo");
		// New command line properties take precedence
		assertThat(environment.getProperty("foo")).isEqualTo("bar");
		CompositePropertySource composite = (CompositePropertySource) environment.getPropertySources()
			.get("commandLineArgs");
		assertThat(composite.getPropertySources()).hasSize(2);
		assertThat(composite.getPropertySources()).first()
			.matches((source) -> source.getName().equals("springApplicationCommandLineArgs"),
					"is named springApplicationCommandLineArgs");
		assertThat(composite.getPropertySources()).element(1)
			.matches((source) -> source.getName().equals("commandLineArgs"), "is named commandLineArgs");
	}

	@Test
	@WithResource(name = "application.properties", content = "foo=bucket")
	void propertiesFileEnhancesEnvironment() {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		ConfigurableEnvironment environment = new StandardEnvironment();
		application.setEnvironment(environment);
		this.context = application.run();
		assertThat(environment.getProperty("foo")).isEqualTo("bucket");
	}

	@Test
	void addProfiles() {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		application.setAdditionalProfiles("foo");
		ConfigurableEnvironment environment = new StandardEnvironment();
		application.setEnvironment(environment);
		this.context = application.run();
		assertThat(environment.acceptsProfiles(Profiles.of("foo"))).isTrue();
	}

	@Test
	void additionalProfilesOrderedBeforeActiveProfiles() {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		application.setAdditionalProfiles("foo");
		ConfigurableEnvironment environment = new StandardEnvironment();
		application.setEnvironment(environment);
		this.context = application.run("--spring.profiles.active=bar,spam");
		assertThat(environment.getActiveProfiles()).containsExactly("foo", "bar", "spam");
	}

	@Test
	@WithResource(name = "application.properties", content = "my.property=fromapplicationproperties")
	@WithResource(name = "application-other.properties", content = "my.property=fromotherpropertiesfile")
	void addProfilesOrderWithProperties() {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		application.setAdditionalProfiles("other");
		ConfigurableEnvironment environment = new StandardEnvironment();
		application.setEnvironment(environment);
		this.context = application.run();
		// Active profile should win over default
		assertThat(environment.getProperty("my.property")).isEqualTo("fromotherpropertiesfile");
	}

	@Test
	@WithResource(name = "application.properties", content = "foo=bucket")
	void emptyCommandLinePropertySourceNotAdded() {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		ConfigurableEnvironment environment = new StandardEnvironment();
		application.setEnvironment(environment);
		this.context = application.run();
		assertThat(environment.getProperty("foo")).isEqualTo("bucket");
	}

	@Test
	void disableCommandLinePropertySource() {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		application.setAddCommandLineProperties(false);
		ConfigurableEnvironment environment = new StandardEnvironment();
		application.setEnvironment(environment);
		this.context = application.run("--foo=bar");
		assertThat(environment).doesNotHave(matchingPropertySource(PropertySource.class, "commandLineArgs"));
	}

	@Test
	void contextUsesApplicationConversionService() {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		this.context = application.run();
		assertThat(this.context.getBeanFactory().getConversionService())
			.isInstanceOf(ApplicationConversionService.class);
		assertThat(this.context.getEnvironment().getConversionService())
			.isInstanceOf(ApplicationConversionService.class);
	}

	@Test
	void contextWhenHasAddConversionServiceFalseUsesRegularConversionService() {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		application.setAddConversionService(false);
		this.context = application.run();
		assertThat(this.context.getBeanFactory().getConversionService()).isNull();
		assertThat(this.context.getEnvironment().getConversionService())
			.isNotInstanceOf(ApplicationConversionService.class);
	}

	@Test
	void runCommandLineRunnersAndApplicationRunners() {
		SpringApplication application = new SpringApplication(CommandLineRunConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		this.context = application.run("arg");
		assertThat(this.context).has(runTestRunnerBean("runnerA"));
		assertThat(this.context).has(runTestRunnerBean("runnerB"));
		assertThat(this.context).has(runTestRunnerBean("runnerC"));
	}

	@Test
	void runCommandLineRunnersAndApplicationRunnersWithParentContext() {
		SpringApplication application = new SpringApplication(CommandLineRunConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		application.addInitializers(new ParentContextApplicationContextInitializer(
				new AnnotationConfigApplicationContext(CommandLineRunParentConfig.class)));
		this.context = application.run("arg");
		assertThat(this.context).has(runTestRunnerBean("runnerA"));
		assertThat(this.context).has(runTestRunnerBean("runnerB"));
		assertThat(this.context).has(runTestRunnerBean("runnerC"));
		assertThat(this.context).doesNotHave(runTestRunnerBean("runnerP"));
	}

	@Test
	void runCommandLineRunnersAndApplicationRunnersUsingOrderOnBeanDefinitions() {
		SpringApplication application = new SpringApplication(BeanDefinitionOrderRunnerConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		this.context = application.run("arg");
		BeanDefinitionOrderRunnerConfig config = this.context.getBean(BeanDefinitionOrderRunnerConfig.class);
		assertThat(config.runners).containsExactly("runnerA", "runnerB", "runnerC");
	}

	@Test
	@SuppressWarnings("unchecked")
	void runnersAreCalledAfterStartedIsLoggedAndBeforeApplicationReadyEventIsPublished(CapturedOutput output)
			throws Exception {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		ApplicationRunner applicationRunner = mock(ApplicationRunner.class);
		CommandLineRunner commandLineRunner = mock(CommandLineRunner.class);
		application.addInitializers((context) -> {
			ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
			beanFactory.registerSingleton("commandLineRunner", (CommandLineRunner) (args) -> {
				assertThat(output).contains("Started");
				commandLineRunner.run(args);
			});
			beanFactory.registerSingleton("applicationRunner", (ApplicationRunner) (args) -> {
				assertThat(output).contains("Started");
				applicationRunner.run(args);
			});
		});
		application.setWebApplicationType(WebApplicationType.NONE);
		ApplicationListener<ApplicationReadyEvent> eventListener = mock(ApplicationListener.class);
		application.addListeners(eventListener);
		this.context = application.run();
		InOrder applicationRunnerOrder = Mockito.inOrder(eventListener, applicationRunner);
		applicationRunnerOrder.verify(applicationRunner).run(any(ApplicationArguments.class));
		applicationRunnerOrder.verify(eventListener).onApplicationEvent(any(ApplicationReadyEvent.class));
		InOrder commandLineRunnerOrder = Mockito.inOrder(eventListener, commandLineRunner);
		commandLineRunnerOrder.verify(commandLineRunner).run();
		commandLineRunnerOrder.verify(eventListener).onApplicationEvent(any(ApplicationReadyEvent.class));
	}

	@Test
	void applicationRunnerFailureCausesApplicationFailedEventToBePublished() throws Exception {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		@SuppressWarnings("unchecked")
		ApplicationListener<SpringApplicationEvent> listener = mock(ApplicationListener.class);
		application.addListeners(listener);
		ApplicationRunner runner = mock(ApplicationRunner.class);
		Exception failure = new Exception();
		willThrow(failure).given(runner).run(isA(ApplicationArguments.class));
		application.addInitializers((context) -> context.getBeanFactory().registerSingleton("runner", runner));
		assertThatIllegalStateException().isThrownBy(application::run).withCause(failure);
		then(listener).should().onApplicationEvent(isA(ApplicationStartedEvent.class));
		then(listener).should().onApplicationEvent(isA(ApplicationFailedEvent.class));
		then(listener).should(never()).onApplicationEvent(isA(ApplicationReadyEvent.class));
	}

	@Test
	void commandLineRunnerFailureCausesApplicationFailedEventToBePublished() throws Exception {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		@SuppressWarnings("unchecked")
		ApplicationListener<SpringApplicationEvent> listener = mock(ApplicationListener.class);
		application.addListeners(listener);
		CommandLineRunner runner = mock(CommandLineRunner.class);
		Exception failure = new Exception();
		willThrow(failure).given(runner).run();
		application.addInitializers((context) -> context.getBeanFactory().registerSingleton("runner", runner));
		assertThatIllegalStateException().isThrownBy(application::run).withCause(failure);
		then(listener).should().onApplicationEvent(isA(ApplicationStartedEvent.class));
		then(listener).should().onApplicationEvent(isA(ApplicationFailedEvent.class));
		then(listener).should(never()).onApplicationEvent(isA(ApplicationReadyEvent.class));
	}

	@Test
	void failureInReadyEventListenerDoesNotCausePublicationOfFailedEvent() {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		@SuppressWarnings("unchecked")
		ApplicationListener<SpringApplicationEvent> listener = mock(ApplicationListener.class);
		application.addListeners(listener);
		RuntimeException failure = new RuntimeException();
		willThrow(failure).given(listener).onApplicationEvent(isA(ApplicationReadyEvent.class));
		assertThatExceptionOfType(RuntimeException.class).isThrownBy(application::run).isEqualTo(failure);
		then(listener).should().onApplicationEvent(isA(ApplicationReadyEvent.class));
		then(listener).should(never()).onApplicationEvent(isA(ApplicationFailedEvent.class));
	}

	@Test
	void failureInReadyEventListenerCloseApplicationContext(CapturedOutput output) {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		ExitCodeListener exitCodeListener = new ExitCodeListener();
		application.addListeners(exitCodeListener);
		@SuppressWarnings("unchecked")
		ApplicationListener<SpringApplicationEvent> listener = mock(ApplicationListener.class);
		application.addListeners(listener);
		ExitStatusException failure = new ExitStatusException();
		willThrow(failure).given(listener).onApplicationEvent(isA(ApplicationReadyEvent.class));
		assertThatExceptionOfType(RuntimeException.class).isThrownBy(application::run);
		then(listener).should().onApplicationEvent(isA(ApplicationReadyEvent.class));
		then(listener).should(never()).onApplicationEvent(isA(ApplicationFailedEvent.class));
		assertThat(exitCodeListener.getExitCode()).isEqualTo(11);
		assertThat(output).contains("Application run failed");
	}

	@Test
	void failureOnTheJvmLogsApplicationRunFailed(CapturedOutput output) {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		ExitCodeListener exitCodeListener = new ExitCodeListener();
		application.addListeners(exitCodeListener);
		@SuppressWarnings("unchecked")
		ApplicationListener<SpringApplicationEvent> listener = mock(ApplicationListener.class);
		application.addListeners(listener);
		ExitStatusException failure = new ExitStatusException();
		willThrow(failure).given(listener).onApplicationEvent(isA(ApplicationReadyEvent.class));
		assertThatExceptionOfType(RuntimeException.class).isThrownBy(application::run);
		then(listener).should().onApplicationEvent(isA(ApplicationReadyEvent.class));
		then(listener).should(never()).onApplicationEvent(isA(ApplicationFailedEvent.class));
		assertThat(exitCodeListener.getExitCode()).isEqualTo(11);
		// Leading space only happens when logging
		assertThat(output).contains(" Application run failed").contains("ExitStatusException");
	}

	@Test
	@ForkedClassPath
	void failureInANativeImageWritesFailureToSystemOut(CapturedOutput output) {
		System.setProperty("org.graalvm.nativeimage.imagecode", "true");
		try {
			SpringApplication application = new SpringApplication(ExampleConfig.class);
			application.setWebApplicationType(WebApplicationType.NONE);
			ExitCodeListener exitCodeListener = new ExitCodeListener();
			application.addListeners(exitCodeListener);
			@SuppressWarnings("unchecked")
			ApplicationListener<SpringApplicationEvent> listener = mock(ApplicationListener.class);
			application.addListeners(listener);
			ExitStatusException failure = new ExitStatusException();
			willThrow(failure).given(listener).onApplicationEvent(isA(ApplicationReadyEvent.class));
			assertThatExceptionOfType(RuntimeException.class).isThrownBy(application::run);
			then(listener).should().onApplicationEvent(isA(ApplicationReadyEvent.class));
			then(listener).should(never()).onApplicationEvent(isA(ApplicationFailedEvent.class));
			assertThat(exitCodeListener.getExitCode()).isEqualTo(11);
			// Leading space only happens when logging
			assertThat(output).doesNotContain(" Application run failed")
				.contains("Application run failed")
				.contains("ExitStatusException");
		}
		finally {
			System.clearProperty("org.graalvm.nativeimage.imagecode");
		}
	}

	@Test
	void loadSources() {
		Class<?>[] sources = { ExampleConfig.class, TestCommandLineRunner.class };
		TestSpringApplication application = new TestSpringApplication(sources);
		application.getSources().add("a");
		application.setWebApplicationType(WebApplicationType.NONE);
		application.setUseMockLoader(true);
		this.context = application.run();
		Set<Object> allSources = application.getAllSources();
		assertThat(allSources).contains(ExampleConfig.class, TestCommandLineRunner.class, "a");
	}

	@Test
	@WithSampleBeansXmlResource
	@WithResource(name = "application.properties", content = "sample.app.test.prop=*")
	void wildcardSources() {
		TestSpringApplication application = new TestSpringApplication();
		application.getSources().add("classpath*:org/springframework/boot/sample-${sample.app.test.prop}.xml");
		application.setWebApplicationType(WebApplicationType.NONE);
		this.context = application.run("--spring.config.location=classpath:/");
	}

	@Test
	void run() {
		this.context = SpringApplication.run(ExampleConfig.class);
		assertThat(this.context).isNotNull();
	}

	@Test
	void runComponents() {
		this.context = SpringApplication.run(new Class<?>[] { ExampleConfig.class, Object.class }, new String[0]);
		assertThat(this.context).isNotNull();
	}

	@Test
	void exit() {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		this.context = application.run();
		assertThat(this.context).isNotNull();
		assertThat(SpringApplication.exit(this.context)).isZero();
	}

	@Test
	void exitWithExplicitCode() {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		ExitCodeListener listener = new ExitCodeListener();
		application.addListeners(listener);
		application.setWebApplicationType(WebApplicationType.NONE);
		this.context = application.run();
		assertThat(this.context).isNotNull();
		assertThat(SpringApplication.exit(this.context, (ExitCodeGenerator) () -> 2)).isEqualTo(2);
		assertThat(listener.getExitCode()).isEqualTo(2);
	}

	@Test
	void exitWithExplicitCodeFromException() {
		final SpringBootExceptionHandler handler = mock(SpringBootExceptionHandler.class);
		SpringApplication application = new SpringApplication(ExitCodeCommandLineRunConfig.class) {

			@Override
			SpringBootExceptionHandler getSpringBootExceptionHandler() {
				return handler;
			}

		};
		ExitCodeListener listener = new ExitCodeListener();
		application.addListeners(listener);
		application.setWebApplicationType(WebApplicationType.NONE);
		assertThatIllegalStateException().isThrownBy(application::run);
		then(handler).should().registerExitCode(11);
		assertThat(listener.getExitCode()).isEqualTo(11);
	}

	@Test
	void exitWithExplicitCodeFromMappedException() {
		final SpringBootExceptionHandler handler = mock(SpringBootExceptionHandler.class);
		SpringApplication application = new SpringApplication(MappedExitCodeCommandLineRunConfig.class) {

			@Override
			SpringBootExceptionHandler getSpringBootExceptionHandler() {
				return handler;
			}

		};
		ExitCodeListener listener = new ExitCodeListener();
		application.addListeners(listener);
		application.setWebApplicationType(WebApplicationType.NONE);
		assertThatIllegalStateException().isThrownBy(application::run);
		then(handler).should().registerExitCode(11);
		assertThat(listener.getExitCode()).isEqualTo(11);
	}

	@Test
	void exceptionFromRefreshIsHandledGracefully(CapturedOutput output) {
		final SpringBootExceptionHandler handler = mock(SpringBootExceptionHandler.class);
		SpringApplication application = new SpringApplication(RefreshFailureConfig.class) {

			@Override
			SpringBootExceptionHandler getSpringBootExceptionHandler() {
				return handler;
			}

		};
		ExitCodeListener listener = new ExitCodeListener();
		application.addListeners(listener);
		application.setWebApplicationType(WebApplicationType.NONE);
		assertThatExceptionOfType(RuntimeException.class).isThrownBy(application::run);
		then(handler).should()
			.registerLoggedException(
					assertArg((exception) -> assertThat(exception).hasCauseInstanceOf(RefreshFailureException.class)));
		assertThat(output).doesNotContain("NullPointerException");
	}

	@Test
	void defaultCommandLineArgs() {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setDefaultProperties(
				StringUtils.splitArrayElementsIntoProperties(new String[] { "baz=", "bar=spam" }, "="));
		application.setWebApplicationType(WebApplicationType.NONE);
		this.context = application.run("--bar=foo", "bucket", "crap");
		assertThat(this.context).isInstanceOf(AnnotationConfigApplicationContext.class);
		assertThat(getEnvironment().getProperty("bar")).isEqualTo("foo");
		assertThat(getEnvironment().getProperty("baz")).isEmpty();
	}

	@Test
	void defaultPropertiesShouldBeMerged() {
		MockEnvironment environment = new MockEnvironment();
		environment.getPropertySources()
			.addFirst(new MapPropertySource(DefaultPropertiesPropertySource.NAME,
					Collections.singletonMap("bar", "foo")));
		SpringApplication application = new SpringApplicationBuilder(ExampleConfig.class).environment(environment)
			.properties("baz=bing")
			.web(WebApplicationType.NONE)
			.build();
		this.context = application.run();
		assertThat(getEnvironment().getProperty("bar")).isEqualTo("foo");
		assertThat(getEnvironment().getProperty("baz")).isEqualTo("bing");
	}

	@Test
	void commandLineArgsApplyToSpringApplication() {
		TestSpringApplication application = new TestSpringApplication(ExampleConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		this.context = application.run("--spring.main.banner-mode=OFF");
		assertThat(application.getBannerMode()).isEqualTo(Banner.Mode.OFF);
	}

	@Test
	void registerShutdownHook() {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		this.context = application.run();
		assertThat(SpringApplicationShutdownHookInstance.get()).registeredApplicationContext(this.context);
	}

	@Test
	void registerShutdownHookOff() {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		application.setRegisterShutdownHook(false);
		this.context = application.run();
		assertThat(SpringApplicationShutdownHookInstance.get()).didNotRegisterApplicationContext(this.context);
	}

	@Test
	void registerListener() {
		SpringApplication application = new SpringApplication(ExampleConfig.class, ListenerConfig.class);
		application.setApplicationContextFactory(ApplicationContextFactory.ofContextClass(SpyApplicationContext.class));
		Set<ApplicationEvent> events = new LinkedHashSet<>();
		application.addListeners((ApplicationListener<ApplicationEvent>) events::add);
		this.context = application.run();
		assertThat(events).hasAtLeastOneElementOfType(ApplicationPreparedEvent.class);
		assertThat(events).hasAtLeastOneElementOfType(ContextRefreshedEvent.class);
		verifyRegisteredListenerSuccessEvents();
	}

	@Test
	void registerListenerWithCustomMulticaster() {
		SpringApplication application = new SpringApplication(ExampleConfig.class, ListenerConfig.class,
				Multicaster.class);
		application.setApplicationContextFactory(ApplicationContextFactory.ofContextClass(SpyApplicationContext.class));
		Set<ApplicationEvent> events = new LinkedHashSet<>();
		application.addListeners((ApplicationListener<ApplicationEvent>) events::add);
		this.context = application.run();
		assertThat(events).hasAtLeastOneElementOfType(ApplicationPreparedEvent.class);
		assertThat(events).hasAtLeastOneElementOfType(ContextRefreshedEvent.class);
		verifyRegisteredListenerSuccessEvents();
	}

	@SuppressWarnings("unchecked")
	private void verifyRegisteredListenerSuccessEvents() {
		ApplicationListener<ApplicationEvent> listener = this.context.getBean("testApplicationListener",
				ApplicationListener.class);
		InOrder inOrder = Mockito.inOrder(listener);
		then(listener).should(inOrder).onApplicationEvent(isA(ContextRefreshedEvent.class));
		then(listener).should(inOrder).onApplicationEvent(isA(ApplicationStartedEvent.class));
		then(listener).should(inOrder)
			.onApplicationEvent(argThat(isAvailabilityChangeEventWithState(LivenessState.CORRECT)));
		then(listener).should(inOrder).onApplicationEvent(isA(ApplicationReadyEvent.class));
		then(listener).should(inOrder)
			.onApplicationEvent(argThat(isAvailabilityChangeEventWithState(ReadinessState.ACCEPTING_TRAFFIC)));
		inOrder.verifyNoMoreInteractions();
	}

	@SuppressWarnings("unchecked")
	@Test
	void applicationListenerFromApplicationIsCalledWhenContextFailsRefreshBeforeListenerRegistration() {
		ApplicationListener<ApplicationEvent> listener = mock(ApplicationListener.class);
		SpringApplication application = new SpringApplication(BrokenBeanFactoryPostProcessing.class);
		application.addListeners(listener);
		assertThatExceptionOfType(ApplicationContextException.class).isThrownBy(application::run);
		verifyRegisteredListenerFailedFromApplicationEvents(listener);
	}

	@SuppressWarnings("unchecked")
	@Test
	void applicationListenerFromApplicationIsCalledWhenContextFailsRefreshAfterListenerRegistration() {
		ApplicationListener<ApplicationEvent> listener = mock(ApplicationListener.class);
		SpringApplication application = new SpringApplication(BrokenPostConstructConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		application.addListeners(listener);
		assertThatExceptionOfType(BeanCreationException.class).isThrownBy(application::run);
		verifyRegisteredListenerFailedFromApplicationEvents(listener);
	}

	private void verifyRegisteredListenerFailedFromApplicationEvents(ApplicationListener<ApplicationEvent> listener) {
		InOrder inOrder = Mockito.inOrder(listener);
		then(listener).should(inOrder).onApplicationEvent(isA(ApplicationStartingEvent.class));
		then(listener).should(inOrder).onApplicationEvent(isA(ApplicationEnvironmentPreparedEvent.class));
		then(listener).should(inOrder).onApplicationEvent(isA(ApplicationContextInitializedEvent.class));
		then(listener).should(inOrder).onApplicationEvent(isA(ApplicationPreparedEvent.class));
		then(listener).should(inOrder).onApplicationEvent(isA(ApplicationFailedEvent.class));
		inOrder.verifyNoMoreInteractions();
	}

	@SuppressWarnings("unchecked")
	@Test
	void applicationListenerFromContextIsCalledWhenContextFailsRefreshBeforeListenerRegistration() {
		final ApplicationListener<ApplicationEvent> listener = mock(ApplicationListener.class);
		SpringApplication application = new SpringApplication(BrokenBeanFactoryPostProcessing.class);
		application.addInitializers((applicationContext) -> applicationContext.addApplicationListener(listener));
		assertThatExceptionOfType(ApplicationContextException.class).isThrownBy(application::run);
		then(listener).should().onApplicationEvent(isA(ApplicationFailedEvent.class));
		then(listener).shouldHaveNoMoreInteractions();
	}

	@SuppressWarnings("unchecked")
	@Test
	void applicationListenerFromContextIsCalledWhenContextFailsRefreshAfterListenerRegistration() {
		ApplicationListener<ApplicationEvent> listener = mock(ApplicationListener.class);
		SpringApplication application = new SpringApplication(BrokenPostConstructConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		application.addInitializers((applicationContext) -> applicationContext.addApplicationListener(listener));
		assertThatExceptionOfType(BeanCreationException.class).isThrownBy(application::run);
		then(listener).should().onApplicationEvent(isA(ApplicationFailedEvent.class));
		then(listener).shouldHaveNoMoreInteractions();
	}

	@Test
	void headless() {
		TestSpringApplication application = new TestSpringApplication(ExampleConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		this.context = application.run();
		assertThat(System.getProperty("java.awt.headless")).isEqualTo("true");
	}

	@Test
	void headlessFalse() {
		TestSpringApplication application = new TestSpringApplication(ExampleConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		application.setHeadless(false);
		this.context = application.run();
		assertThat(System.getProperty("java.awt.headless")).isEqualTo("false");
	}

	@Test
	void headlessSystemPropertyTakesPrecedence() {
		System.setProperty("java.awt.headless", "false");
		TestSpringApplication application = new TestSpringApplication(ExampleConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		this.context = application.run();
		assertThat(System.getProperty("java.awt.headless")).isEqualTo("false");
	}

	@Test
	void getApplicationArgumentsBean() {
		TestSpringApplication application = new TestSpringApplication(ExampleConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		this.context = application.run("--debug", "spring", "boot");
		ApplicationArguments args = this.context.getBean(ApplicationArguments.class);
		assertThat(args.getNonOptionArgs()).containsExactly("spring", "boot");
		assertThat(args.containsOption("debug")).isTrue();
	}

	@Test
	void nonWebApplicationConfiguredViaAPropertyHasTheCorrectTypeOfContextAndEnvironment() {
		ConfigurableApplicationContext context = new SpringApplication(ExampleConfig.class)
			.run("--spring.main.web-application-type=none");
		assertThat(context).isNotInstanceOfAny(WebApplicationContext.class, ReactiveWebApplicationContext.class);
		assertThat(context.getEnvironment()).isNotInstanceOfAny(ConfigurableWebEnvironment.class);
	}

	@Test
	void failureResultsInSingleStackTrace(CapturedOutput output) throws Exception {
		ThreadGroup group = new ThreadGroup("main");
		Thread thread = new Thread(group, "main") {

			@Override
			public void run() {
				SpringApplication application = new SpringApplication(FailingConfig.class);
				application.setWebApplicationType(WebApplicationType.NONE);
				application.run();
			}

		};
		thread.start();
		thread.join(6000);
		assertThat(output).containsOnlyOnce("Caused by: java.lang.RuntimeException: ExpectedError");
	}

	@Test
	void beanDefinitionOverridingIsDisabledByDefault() {
		assertThatExceptionOfType(BeanDefinitionOverrideException.class)
			.isThrownBy(() -> new SpringApplication(ExampleConfig.class, OverrideConfig.class).run());
	}

	@Test
	void beanDefinitionOverridingCanBeEnabled() {
		assertThat(new SpringApplication(ExampleConfig.class, OverrideConfig.class)
			.run("--spring.main.allow-bean-definition-overriding=true", "--spring.main.web-application-type=none")
			.getBean("someBean")).isEqualTo("override");
	}

	@Test
	void circularReferencesAreDisabledByDefault() {
		assertThatExceptionOfType(UnsatisfiedDependencyException.class)
			.isThrownBy(
					() -> new SpringApplication(ExampleProducerConfiguration.class, ExampleConsumerConfiguration.class)
						.run("--spring.main.web-application-type=none"))
			.withRootCauseInstanceOf(BeanCurrentlyInCreationException.class);
	}

	@Test
	void circularReferencesCanBeEnabled() {
		assertThatNoException().isThrownBy(
				() -> new SpringApplication(ExampleProducerConfiguration.class, ExampleConsumerConfiguration.class)
					.run("--spring.main.web-application-type=none", "--spring.main.allow-circular-references=true"));
	}

	@Test
	@WithResource(name = "custom-config/application.yml", content = "hello: world")
	void relaxedBindingShouldWorkBeforeEnvironmentIsPrepared() {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		this.context = application.run("--spring.config.additionalLocation=classpath:custom-config/");
		assertThat(this.context.getEnvironment().getProperty("hello")).isEqualTo("world");
	}

	@Test
	void lazyInitializationIsDisabledByDefault() {
		assertThat(new SpringApplication(LazyInitializationConfig.class).run("--spring.main.web-application-type=none")
			.getBean(AtomicInteger.class)).hasValue(1);
	}

	@Test
	void lazyInitializationCanBeEnabled() {
		assertThat(new SpringApplication(LazyInitializationConfig.class)
			.run("--spring.main.web-application-type=none", "--spring.main.lazy-initialization=true")
			.getBean(AtomicInteger.class)).hasValue(0);
	}

	@Test
	void lazyInitializationIgnoresBeansThatAreExplicitlyNotLazy() {
		assertThat(new SpringApplication(NotLazyInitializationConfig.class)
			.run("--spring.main.web-application-type=none", "--spring.main.lazy-initialization=true")
			.getBean(AtomicInteger.class)).hasValue(1);
	}

	@Test
	void lazyInitializationIgnoresLazyInitializationExcludeFilteredBeans() {
		assertThat(new SpringApplication(LazyInitializationExcludeFilterConfig.class)
			.run("--spring.main.web-application-type=none", "--spring.main.lazy-initialization=true")
			.getBean(AtomicInteger.class)).hasValue(1);
	}

	@Test
	void customApplicationStartupPublishStartupSteps() {
		ApplicationStartup applicationStartup = mock(ApplicationStartup.class);
		StartupStep startupStep = mock(StartupStep.class);
		given(applicationStartup.start(anyString())).willReturn(startupStep);
		given(startupStep.tag(anyString(), anyString())).willReturn(startupStep);
		given(startupStep.tag(anyString(), ArgumentMatchers.<Supplier<String>>any())).willReturn(startupStep);
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		application.setApplicationStartup(applicationStartup);
		this.context = application.run();
		assertThat(this.context.getBean(ApplicationStartup.class)).isEqualTo(applicationStartup);
		then(applicationStartup).should().start("spring.boot.application.starting");
		then(applicationStartup).should().start("spring.boot.application.environment-prepared");
		then(applicationStartup).should().start("spring.boot.application.context-prepared");
		then(applicationStartup).should().start("spring.boot.application.context-loaded");
		then(applicationStartup).should().start("spring.boot.application.started");
		then(applicationStartup).should().start("spring.boot.application.ready");
		long startCount = mockingDetails(applicationStartup).getInvocations()
			.stream()
			.filter((invocation) -> invocation.getMethod().toString().contains("start("))
			.count();
		long endCount = mockingDetails(startupStep).getInvocations()
			.stream()
			.filter((invocation) -> invocation.getMethod().toString().contains("end("))
			.count();
		assertThat(startCount).isEqualTo(endCount);
	}

	@Test
	void customApplicationStartupPublishStartupStepsWithFailure() {
		ApplicationStartup applicationStartup = mock(ApplicationStartup.class);
		StartupStep startupStep = mock(StartupStep.class);
		given(applicationStartup.start(anyString())).willReturn(startupStep);
		given(startupStep.tag(anyString(), anyString())).willReturn(startupStep);
		given(startupStep.tag(anyString(), ArgumentMatchers.<Supplier<String>>any())).willReturn(startupStep);
		SpringApplication application = new SpringApplication(BrokenPostConstructConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		application.setApplicationStartup(applicationStartup);
		assertThatExceptionOfType(BeanCreationException.class).isThrownBy(application::run);
		then(applicationStartup).should().start("spring.boot.application.starting");
		then(applicationStartup).should().start("spring.boot.application.environment-prepared");
		then(applicationStartup).should().start("spring.boot.application.failed");
		long startCount = mockingDetails(applicationStartup).getInvocations()
			.stream()
			.filter((invocation) -> invocation.getMethod().toString().contains("start("))
			.count();
		long endCount = mockingDetails(startupStep).getInvocations()
			.stream()
			.filter((invocation) -> invocation.getMethod().toString().contains("end("))
			.count();
		assertThat(startCount).isEqualTo(endCount);
	}

	@Test
	void addBootstrapRegistryInitializer() {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		application.addBootstrapRegistryInitializer(
				(bootstrapContext) -> bootstrapContext.register(String.class, InstanceSupplier.of("boot")));
		TestApplicationListener listener = new TestApplicationListener();
		application.addListeners(listener);
		application.run();
		ApplicationStartingEvent startingEvent = listener.getEvent(ApplicationStartingEvent.class);
		assertThat(startingEvent.getBootstrapContext().get(String.class)).isEqualTo("boot");
		ApplicationEnvironmentPreparedEvent environmentPreparedEvent = listener
			.getEvent(ApplicationEnvironmentPreparedEvent.class);
		assertThat(environmentPreparedEvent.getBootstrapContext().get(String.class)).isEqualTo("boot");
	}

	@Test
	void addBootstrapRegistryInitializerCanRegisterBeans() {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		application.addBootstrapRegistryInitializer((bootstrapContext) -> {
			bootstrapContext.register(String.class, InstanceSupplier.of("boot"));
			bootstrapContext.addCloseListener((event) -> event.getApplicationContext()
				.getBeanFactory()
				.registerSingleton("test", event.getBootstrapContext().get(String.class)));
		});
		ConfigurableApplicationContext applicationContext = application.run();
		assertThat(applicationContext.getBean("test")).isEqualTo("boot");
	}

	@Test
	void settingEnvironmentPrefixViaPropertiesThrowsException() {
		assertThatIllegalStateException()
			.isThrownBy(() -> new SpringApplication().run("--spring.main.environment-prefix=my"));
	}

	@Test
	void bindsEnvironmentPrefixToSpringApplication() {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setEnvironmentPrefix("my");
		application.setWebApplicationType(WebApplicationType.NONE);
		this.context = application.run();
		assertThat(application.getEnvironmentPrefix()).isEqualTo("my");
	}

	@Test
	@WithResource(name = "spring-application-config-property-source.properties",
			content = "test.name=spring-application-config-property-source")
	void movesConfigClassPropertySourcesToEnd() {
		SpringApplication application = new SpringApplication(PropertySourceConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		application.setDefaultProperties(Collections.singletonMap("test.name", "test"));
		this.context = application.run();
		assertThat(this.context.getEnvironment().getProperty("test.name"))
			.isEqualTo("spring-application-config-property-source");
	}

	@Test
	void deregistersShutdownHookForFailedApplicationContext() {
		SpringApplication application = new SpringApplication(BrokenPostConstructConfig.class);
		List<ApplicationEvent> events = new ArrayList<>();
		application.addListeners(events::add);
		application.setWebApplicationType(WebApplicationType.NONE);
		assertThatExceptionOfType(BeanCreationException.class).isThrownBy(application::run);
		assertThat(events).hasAtLeastOneElementOfType(ApplicationFailedEvent.class);
		ApplicationFailedEvent failure = events.stream()
			.filter((event) -> event instanceof ApplicationFailedEvent)
			.map(ApplicationFailedEvent.class::cast)
			.findFirst()
			.get();
		assertThat(SpringApplicationShutdownHookInstance.get())
			.didNotRegisterApplicationContext(failure.getApplicationContext());
	}

	@Test
	void withRunnableHookRunsWithHook() {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		SpringApplicationRunListener runListener = mock(SpringApplicationRunListener.class);
		SpringApplicationHook hook = (springApplication) -> runListener;
		SpringApplication.withHook(hook, () -> this.context = application.run());
		then(runListener).should().starting(any());
		then(runListener).should().contextPrepared(this.context);
		then(runListener).should().ready(eq(this.context), any());
		assertThat(this.context.isRunning()).isTrue();
	}

	@Test
	void withCallableHookRunsWithHook() {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		SpringApplicationRunListener runListener = mock(SpringApplicationRunListener.class);
		SpringApplicationHook hook = (springApplication) -> runListener;
		this.context = SpringApplication.withHook(hook, () -> application.run());
		then(runListener).should().starting(any());
		then(runListener).should().contextPrepared(this.context);
		then(runListener).should().ready(eq(this.context), any());
		assertThat(this.context.isRunning()).isTrue();
	}

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	void withHookWhenHookThrowsAbandonedRunExceptionAbandonsRun() {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		ApplicationListener listener = mock(ApplicationListener.class);
		application.addListeners(listener);
		application.setWebApplicationType(WebApplicationType.NONE);
		SpringApplicationRunListener runListener = spy(new SpringApplicationRunListener() {

			@Override
			public void contextLoaded(ConfigurableApplicationContext context) {
				throw new SpringApplication.AbandonedRunException(context);
			}

		});
		SpringApplicationHook hook = (springApplication) -> runListener;
		assertThatExceptionOfType(SpringApplication.AbandonedRunException.class)
			.isThrownBy(() -> SpringApplication.withHook(hook, () -> application.run()))
			.satisfies((ex) -> assertThat(ex.getApplicationContext().isRunning()).isFalse());
		then(runListener).should().starting(any());
		then(runListener).should().contextPrepared(any());
		then(runListener).should(never()).ready(any(), any());
		then(runListener).should(never()).failed(any(), any());
		then(listener).should().onApplicationEvent(any(ApplicationStartingEvent.class));
		then(listener).should().onApplicationEvent(any(ApplicationEnvironmentPreparedEvent.class));
		then(listener).should().onApplicationEvent(any(ApplicationPreparedEvent.class));
		then(listener).should(never()).onApplicationEvent(any(ApplicationReadyEvent.class));
		then(listener).should(never()).onApplicationEvent(any(ApplicationFailedEvent.class));
	}

	@Test
	// gh-32555
	void shouldUseAotInitializer() {
		SpringApplication application = new SpringApplication(ExampleAotProcessedMainClass.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		application.setMainApplicationClass(ExampleAotProcessedMainClass.class);
		System.setProperty(AotDetector.AOT_ENABLED, "true");
		try {
			ApplicationContext context = application.run();
			assertThat(context.getBean("test")).isEqualTo("test");
		}
		finally {
			System.clearProperty(AotDetector.AOT_ENABLED);
		}
	}

	@Test
	void shouldReportFriendlyErrorIfAotInitializerNotFound() {
		SpringApplication application = new SpringApplication(TestSpringApplication.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		application.setMainApplicationClass(TestSpringApplication.class);
		System.setProperty(AotDetector.AOT_ENABLED, "true");
		try {
			assertThatExceptionOfType(AotInitializerNotFoundException.class).isThrownBy(application::run)
				.withMessageMatching("^.+AOT initializer .+ could not be found$");
		}
		finally {
			System.clearProperty(AotDetector.AOT_ENABLED);
		}
	}

	@Test
	void fromRunsWithAdditionalSources() {
		assertThat(ExampleAdditionalConfig.local.get()).isNull();
		this.context = SpringApplication.from(ExampleFromMainMethod::main)
			.with(ExampleAdditionalConfig.class)
			.run()
			.getApplicationContext();
		assertThat(ExampleAdditionalConfig.local.get()).isNotNull();
		ExampleAdditionalConfig.local.remove();
	}

	@Test
	void fromReturnsApplicationContext() {
		this.context = SpringApplication.from(ExampleFromMainMethod::main)
			.with(ExampleAdditionalConfig.class)
			.run()
			.getApplicationContext();
		assertThat(this.context).isNotNull();
	}

	@Test
	void fromWithMultipleApplicationsOnlyAppliesAdditionalSourcesOnce() {
		this.context = SpringApplication.from(MultipleApplicationsMainMethod::main)
			.with(SingleUseAdditionalConfig.class)
			.run()
			.getApplicationContext();
		assertThatNoException().isThrownBy(() -> this.context.getBean(SingleUseAdditionalConfig.class));
	}

	@Test
	void fromAppliesProfiles() {
		this.context = SpringApplication.from(ExampleFromMainMethod::main)
			.with(ProfileConfig.class)
			.withAdditionalProfiles("custom")
			.run()
			.getApplicationContext();
		assertThat(this.context).isNotNull();
		assertThat(this.context.getBeanProvider(Example.class).getIfAvailable()).isNotNull();
	}

	@Test
	void shouldStartDaemonThreadIfKeepAliveIsEnabled() {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		this.context = application.run("--spring.main.keep-alive=true");
		Set<Thread> threads = getCurrentThreads();
		assertThat(threads).filteredOn((thread) -> thread.getName().equals("keep-alive"))
			.singleElement()
			.satisfies((thread) -> assertThat(thread.isDaemon()).isFalse());
	}

	@Test
	void shouldStopKeepAliveThreadIfContextIsClosed() {
		SpringApplication application = new SpringApplication(ExampleConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		application.setKeepAlive(true);
		this.context = application.run();
		assertThat(getCurrentThreads()).filteredOn((thread) -> thread.getName().equals("keep-alive")).isNotEmpty();
		this.context.close();
		Awaitility.await()
			.atMost(Duration.ofSeconds(30))
			.untilAsserted(
					() -> assertThat(getCurrentThreads()).filteredOn((thread) -> thread.getName().equals("keep-alive"))
						.isEmpty());
	}

	private <S extends AvailabilityState> ArgumentMatcher<ApplicationEvent> isAvailabilityChangeEventWithState(
			S state) {
		return (argument) -> (argument instanceof AvailabilityChangeEvent<?> availabilityChangeEvent)
				&& availabilityChangeEvent.getState().equals(state);
	}

	private <E extends ApplicationEvent> AtomicReference<E> addListener(SpringApplication application,
			Class<E> eventType) {
		AtomicReference<E> reference = new AtomicReference<>();
		application.addListeners(new TestEventListener<>(eventType, reference));
		return reference;
	}

	private Condition<ConfigurableEnvironment> matchingPropertySource(final Class<?> propertySourceClass,
			final String name) {

		return new Condition<>("has property source") {

			@Override
			public boolean matches(ConfigurableEnvironment value) {
				for (PropertySource<?> source : value.getPropertySources()) {
					if (propertySourceClass.isInstance(source) && (name == null || name.equals(source.getName()))) {
						return true;
					}
				}
				return false;
			}

		};
	}

	private Condition<ConfigurableApplicationContext> runTestRunnerBean(String name) {
		return new Condition<>("run testrunner bean") {

			@Override
			public boolean matches(ConfigurableApplicationContext value) {
				return value.getBean(name, AbstractTestRunner.class).hasRun();
			}

		};
	}

	private Set<Thread> getCurrentThreads() {
		return Thread.getAllStackTraces().keySet();
	}

	private void assertThatBannerModeIs(SpringApplication application, Mode mode) {
		Object properties = ReflectionTestUtils.getField(application, "properties");
		assertThat(properties).hasFieldOrPropertyWithValue("bannerMode", mode);
	}

	static class TestEventListener<E extends ApplicationEvent> implements SmartApplicationListener {

		private final Class<E> eventType;

		private final AtomicReference<E> reference;

		TestEventListener(Class<E> eventType, AtomicReference<E> reference) {
			this.eventType = eventType;
			this.reference = reference;
		}

		@Override
		public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
			return this.eventType.isAssignableFrom(eventType);
		}

		@Override
		@SuppressWarnings("unchecked")
		public void onApplicationEvent(ApplicationEvent event) {
			this.reference.set((E) event);
		}

	}

	@Configuration
	static class InaccessibleConfiguration {

		private InaccessibleConfiguration() {
		}

		@Bean
		String testMessage() {
			return "test";
		}

	}

	static class SpyApplicationContext extends AnnotationConfigApplicationContext {

		ConfigurableApplicationContext applicationContext = spy(new AnnotationConfigApplicationContext());

		@Override
		public void registerShutdownHook() {
			this.applicationContext.registerShutdownHook();
		}

		ConfigurableApplicationContext getApplicationContext() {
			return this.applicationContext;
		}

		@Override
		public void close() {
			this.applicationContext.close();
			super.close();
		}

	}

	static class TestSpringApplication extends SpringApplication {

		private BeanDefinitionLoader loader;

		private boolean useMockLoader;

		TestSpringApplication(Class<?>... primarySources) {
			super(primarySources);
		}

		TestSpringApplication(ResourceLoader resourceLoader, Class<?>... primarySources) {
			super(resourceLoader, primarySources);
		}

		void setUseMockLoader(boolean useMockLoader) {
			this.useMockLoader = useMockLoader;
		}

		@Override
		protected BeanDefinitionLoader createBeanDefinitionLoader(BeanDefinitionRegistry registry, Object[] sources) {
			if (this.useMockLoader) {
				this.loader = mock(BeanDefinitionLoader.class);
			}
			else {
				this.loader = spy(super.createBeanDefinitionLoader(registry, sources));
			}
			return this.loader;
		}

		BeanDefinitionLoader getLoader() {
			return this.loader;
		}

		Banner.Mode getBannerMode() {
			return this.properties.getBannerMode(new MockEnvironment());
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ExampleConfig {

		@Bean
		String someBean() {
			return "test";
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class OverrideConfig {

		@Bean
		String someBean() {
			return "override";
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class BrokenPostConstructConfig {

		@Bean
		Thing thing() {
			return new Thing();
		}

		static class Thing {

			@PostConstruct
			void boom() {
				throw new IllegalStateException();
			}

		}

	}

	@Configuration(proxyBeanMethods = false)
	static class BrokenBeanFactoryPostProcessing {

		@Bean
		static BeanFactoryPostProcessor brokenBeanFactoryPostProcessor() {
			return (beanFactory) -> {
				throw new ApplicationContextException("broken");
			};
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ListenerConfig {

		@Bean
		ApplicationListener<?> testApplicationListener() {
			return mock(ApplicationListener.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class Multicaster {

		@Bean(name = AbstractApplicationContext.APPLICATION_EVENT_MULTICASTER_BEAN_NAME)
		ApplicationEventMulticaster applicationEventMulticaster() {
			return spy(new SimpleApplicationEventMulticaster());
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class FailingConfig {

		@Bean
		Object fail() {
			throw new RuntimeException("ExpectedError");
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CommandLineRunConfig {

		@Bean
		TestCommandLineRunner runnerC() {
			return new TestCommandLineRunner("runnerC", Ordered.LOWEST_PRECEDENCE, "runnerB", "runnerA");
		}

		@Bean
		TestApplicationRunner runnerB() {
			return new TestApplicationRunner("runnerB", Ordered.LOWEST_PRECEDENCE - 1, "runnerA");
		}

		@Bean
		TestCommandLineRunner runnerA() {
			return new TestCommandLineRunner("runnerA", Ordered.HIGHEST_PRECEDENCE);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CommandLineRunParentConfig {

		@Bean
		TestCommandLineRunner runnerP() {
			return new TestCommandLineRunner("runnerP", Ordered.LOWEST_PRECEDENCE);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class BeanDefinitionOrderRunnerConfig {

		private final List<String> runners = new ArrayList<>();

		@Bean
		@Order
		CommandLineRunner runnerC() {
			return (args) -> this.runners.add("runnerC");
		}

		@Bean
		@Order(Ordered.LOWEST_PRECEDENCE - 1)
		ApplicationRunner runnerB() {
			return (args) -> this.runners.add("runnerB");
		}

		@Bean
		@Order(Ordered.HIGHEST_PRECEDENCE)
		CommandLineRunner runnerA() {
			return (args) -> this.runners.add("runnerA");
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ExitCodeCommandLineRunConfig {

		@Bean
		CommandLineRunner runner() {
			return (args) -> {
				throw new IllegalStateException(new ExitStatusException());
			};
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class MappedExitCodeCommandLineRunConfig {

		@Bean
		CommandLineRunner runner() {
			return (args) -> {
				throw new IllegalStateException();
			};
		}

		@Bean
		ExitCodeExceptionMapper exceptionMapper() {
			return (exception) -> {
				if (exception instanceof IllegalStateException) {
					return 11;
				}
				return 0;
			};
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class RefreshFailureConfig {

		@PostConstruct
		void fail() {
			throw new RefreshFailureException();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class LazyInitializationConfig {

		@Bean
		AtomicInteger counter() {
			return new AtomicInteger();
		}

		@Bean
		LazyBean lazyBean(AtomicInteger counter) {
			return new LazyBean(counter);
		}

		static class LazyBean {

			LazyBean(AtomicInteger counter) {
				counter.incrementAndGet();
			}

		}

	}

	@Configuration(proxyBeanMethods = false)
	static class NotLazyInitializationConfig {

		@Bean
		AtomicInteger counter() {
			return new AtomicInteger();
		}

		@Bean
		@Lazy(false)
		NotLazyBean NotLazyBean(AtomicInteger counter) {
			return new NotLazyBean(counter);
		}

		static class NotLazyBean {

			NotLazyBean(AtomicInteger counter) {
				counter.getAndIncrement();
			}

		}

	}

	@Configuration(proxyBeanMethods = false)
	static class LazyInitializationExcludeFilterConfig {

		@Bean
		AtomicInteger counter() {
			return new AtomicInteger();
		}

		@Bean
		NotLazyBean notLazyBean(AtomicInteger counter) {
			return new NotLazyBean(counter);
		}

		@Bean
		static LazyInitializationExcludeFilter lazyInitializationExcludeFilter() {
			return LazyInitializationExcludeFilter.forBeanTypes(NotLazyBean.class);
		}

	}

	static class NotLazyBean {

		NotLazyBean(AtomicInteger counter) {
			counter.getAndIncrement();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@org.springframework.context.annotation.PropertySource("classpath:spring-application-config-property-source.properties")
	static class PropertySourceConfig {

	}

	static class ExitStatusException extends RuntimeException implements ExitCodeGenerator {

		@Override
		public int getExitCode() {
			return 11;
		}

	}

	static class RefreshFailureException extends RuntimeException {

	}

	abstract static class AbstractTestRunner implements ApplicationContextAware, Ordered {

		private final String[] expectedBefore;

		private ApplicationContext applicationContext;

		private final int order;

		private boolean run;

		AbstractTestRunner(int order, String... expectedBefore) {
			this.expectedBefore = expectedBefore;
			this.order = order;
		}

		@Override
		public void setApplicationContext(ApplicationContext applicationContext) {
			this.applicationContext = applicationContext;
		}

		@Override
		public int getOrder() {
			return this.order;
		}

		void markAsRan() {
			this.run = true;
			for (String name : this.expectedBefore) {
				AbstractTestRunner bean = this.applicationContext.getBean(name, AbstractTestRunner.class);
				assertThat(bean.hasRun()).isTrue();
			}
		}

		boolean hasRun() {
			return this.run;
		}

	}

	static class TestCommandLineRunner extends AbstractTestRunner implements CommandLineRunner {

		private final String name;

		TestCommandLineRunner(String name, int order, String... expectedBefore) {
			super(order, expectedBefore);
			this.name = name;
		}

		@Override
		public void run(String... args) {
			System.out.println(">>> " + this.name);
			markAsRan();
		}

	}

	static class TestApplicationRunner extends AbstractTestRunner implements ApplicationRunner {

		private final String name;

		TestApplicationRunner(String name, int order, String... expectedBefore) {
			super(order, expectedBefore);
			this.name = name;
		}

		@Override
		public void run(ApplicationArguments args) {
			System.out.println(">>> " + this.name);
			markAsRan();
		}

	}

	static class ExitCodeListener implements ApplicationListener<ExitCodeEvent> {

		private Integer exitCode;

		@Override
		public void onApplicationEvent(ExitCodeEvent event) {
			this.exitCode = event.getExitCode();
		}

		Integer getExitCode() {
			return this.exitCode;
		}

	}

	static class MockResourceLoader implements ResourceLoader {

		private final Map<String, Resource> resources = new HashMap<>();

		void addResource(String source, String path) {
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

	static class TestApplicationListener implements ApplicationListener<ApplicationEvent> {

		private final MultiValueMap<Class<?>, ApplicationEvent> events = new LinkedMultiValueMap<>();

		@Override
		public void onApplicationEvent(ApplicationEvent event) {
			this.events.add(event.getClass(), event);
		}

		@SuppressWarnings("unchecked")
		<E extends ApplicationEvent> E getEvent(Class<E> type) {
			return (E) this.events.get(type).get(0);
		}

	}

	static class Example {

	}

	@FunctionalInterface
	interface ExampleConfigurer {

		void configure(Example example);

	}

	@Configuration(proxyBeanMethods = false)
	static class ExampleProducerConfiguration {

		@Bean
		Example example(ObjectProvider<ExampleConfigurer> configurers) {
			Example example = new Example();
			configurers.orderedStream().forEach((configurer) -> configurer.configure(example));
			return example;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ExampleConsumerConfiguration {

		@Autowired
		Example example;

		@Bean
		ExampleConfigurer configurer() {
			return (example) -> {
			};
		}

	}

	static class ExampleAotProcessedMainClass {

	}

	static class ExampleAotProcessedMainClass__ApplicationContextInitializer
			implements ApplicationContextInitializer<ConfigurableApplicationContext> {

		@Override
		public void initialize(ConfigurableApplicationContext applicationContext) {
			applicationContext.getBeanFactory().registerSingleton("test", "test");
		}

	}

	static class ExampleFromMainMethod {

		static void main(String[] args) {
			SpringApplication application = new SpringApplication(ExampleConfig.class);
			application.setWebApplicationType(WebApplicationType.NONE);
			application.run(args);
		}

	}

	static class MultipleApplicationsMainMethod {

		static void main(String[] args) {
			SpringApplication application = new SpringApplication(ExampleConfig.class);
			application.setWebApplicationType(WebApplicationType.NONE);
			application.addListeners(new ApplicationListener<ApplicationEnvironmentPreparedEvent>() {

				@Override
				public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
					SpringApplicationBuilder builder = new SpringApplicationBuilder(
							InnerApplicationConfiguration.class);
					builder.web(WebApplicationType.NONE);
					builder.run().close();
				}

			});
			application.run(args);
		}

		static class InnerApplicationConfiguration {

		}

	}

	@Configuration
	static class ExampleAdditionalConfig {

		static ThreadLocal<ExampleAdditionalConfig> local = new ThreadLocal<>();

		ExampleAdditionalConfig() {
			local.set(this);
		}

	}

	@Configuration
	static class SingleUseAdditionalConfig {

		private static AtomicBoolean used = new AtomicBoolean(false);

		SingleUseAdditionalConfig() {
			if (!used.compareAndSet(false, true)) {
				throw new IllegalStateException("Single-use configuration has already been used");
			}
		}

	}

	@Configuration
	static class ProfileConfig {

		@Bean
		@Profile("custom")
		Example example() {
			return new Example();
		}

	}

}
