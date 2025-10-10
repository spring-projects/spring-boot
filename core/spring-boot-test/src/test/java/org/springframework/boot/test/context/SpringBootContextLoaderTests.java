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

package org.springframework.boot.test.context;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.ApplicationContextFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest.UseMainMethod;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.web.context.reactive.GenericReactiveWebApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ApplicationContextFailureProcessor;
import org.springframework.test.context.BootstrapUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link SpringBootContextLoader}
 *
 * @author Stephane Nicoll
 * @author Scott Frederick
 * @author Madhura Bhave
 * @author Sijun Yang
 */
class SpringBootContextLoaderTests {

	@BeforeEach
	void setUp() {
		ContextLoaderApplicationContextFailureProcessor.reset();
	}

	@Test
	void environmentPropertiesSimple() {
		Map<String, Object> config = getMergedContextConfigurationProperties(SimpleConfig.class);
		assertKey(config, "key", "myValue");
		assertKey(config, "anotherKey", "anotherValue");
	}

	@Test
	void environmentPropertiesSimpleNonAlias() {
		Map<String, Object> config = getMergedContextConfigurationProperties(SimpleConfigNonAlias.class);
		assertKey(config, "key", "myValue");
		assertKey(config, "anotherKey", "anotherValue");
	}

	@Test
	void environmentPropertiesOverrideDefaults() {
		Map<String, Object> config = getMergedContextConfigurationProperties(OverrideConfig.class);
		assertKey(config, "server.port", "2345");
	}

	@Test
	void environmentPropertiesAppend() {
		Map<String, Object> config = getMergedContextConfigurationProperties(AppendConfig.class);
		assertKey(config, "key", "myValue");
		assertKey(config, "otherKey", "otherValue");
	}

	@Test
	void environmentPropertiesSeparatorInValue() {
		Map<String, Object> config = getMergedContextConfigurationProperties(SameSeparatorInValue.class);
		assertKey(config, "key", "my=Value");
		assertKey(config, "anotherKey", "another:Value");
	}

	@Test
	void environmentPropertiesAnotherSeparatorInValue() {
		Map<String, Object> config = getMergedContextConfigurationProperties(AnotherSeparatorInValue.class);
		assertKey(config, "key", "my:Value");
		assertKey(config, "anotherKey", "another=Value");
	}

	@Test // gh-4384
	@Disabled
	void environmentPropertiesNewLineInValue() {
		Map<String, Object> config = getMergedContextConfigurationProperties(NewLineInValue.class);
		assertKey(config, "key", "myValue");
		assertKey(config, "variables", "foo=FOO\n bar=BAR");
	}

	@Test
	void noActiveProfiles() {
		assertThat(getActiveProfiles(SimpleConfig.class)).isEmpty();
	}

	@Test
	void multipleActiveProfiles() {
		assertThat(getActiveProfiles(MultipleActiveProfiles.class)).containsExactly("profile1", "profile2");
	}

	@Test // gh-28776
	void testPropertyValuesShouldTakePrecedenceWhenInlinedPropertiesPresent() {
		TestContext context = new ExposedTestContextManager(SimpleConfig.class).getExposedTestContext();
		StandardEnvironment environment = (StandardEnvironment) context.getApplicationContext().getEnvironment();
		TestPropertyValues.of("key=thisValue").applyTo(environment);
		assertThat(environment.getProperty("key")).isEqualTo("thisValue");
		assertThat(environment.getPropertySources().get("active-test-profiles")).isNull();
	}

	@Test
	void testPropertyValuesShouldTakePrecedenceWhenInlinedPropertiesPresentAndProfilesActive() {
		TestContext context = new ExposedTestContextManager(ActiveProfileWithInlinedProperties.class)
			.getExposedTestContext();
		StandardEnvironment environment = (StandardEnvironment) context.getApplicationContext().getEnvironment();
		TestPropertyValues.of("key=thisValue").applyTo(environment);
		assertThat(environment.getProperty("key")).isEqualTo("thisValue");
		assertThat(environment.getPropertySources().get("active-test-profiles")).isNotNull();
	}

	@Test
	void propertySourceOrdering() {
		TestContext context = new ExposedTestContextManager(PropertySourceOrdering.class).getExposedTestContext();
		ConfigurableEnvironment environment = (ConfigurableEnvironment) context.getApplicationContext()
			.getEnvironment();
		List<String> names = environment.getPropertySources()
			.stream()
			.map(PropertySource::getName)
			.collect(Collectors.toCollection(ArrayList::new));
		String configResource = names.remove(names.size() - 2);
		assertThat(names).containsExactly("configurationProperties", "Inlined Test Properties", "commandLineArgs",
				"systemProperties", "systemEnvironment", "random", "applicationInfo");
		assertThat(configResource).startsWith("Config resource");
	}

	@Test
	void whenEnvironmentChangesWebApplicationTypeToNoneThenContextTypeChangesAccordingly() {
		TestContext context = new ExposedTestContextManager(ChangingWebApplicationTypeToNone.class)
			.getExposedTestContext();
		assertThat(context.getApplicationContext()).isNotInstanceOf(WebApplicationContext.class);
	}

	@Test
	void whenEnvironmentChangesWebApplicationTypeToReactiveThenContextTypeChangesAccordingly() {
		TestContext context = new ExposedTestContextManager(ChangingWebApplicationTypeToReactive.class)
			.getExposedTestContext();
		assertThat(context.getApplicationContext()).isInstanceOf(GenericReactiveWebApplicationContext.class);
	}

	@Test
	void whenUseMainMethodAlwaysAndMainMethodThrowsException() {
		TestContext testContext = new ExposedTestContextManager(UseMainMethodAlwaysAndMainMethodThrowsException.class)
			.getExposedTestContext();
		assertThatIllegalStateException().isThrownBy(testContext::getApplicationContext)
			.havingCause()
			.withMessageContaining("ThrownFromMain");
	}

	@Test
	void whenUseMainMethodWhenAvailableAndNoMainMethod() {
		TestContext testContext = new ExposedTestContextManager(UseMainMethodWhenAvailableAndNoMainMethod.class)
			.getExposedTestContext();
		ApplicationContext applicationContext = testContext.getApplicationContext();
		assertThat(applicationContext.getEnvironment().getActiveProfiles()).isEmpty();
	}

	@Test
	void whenUseMainMethodWhenAvailableAndMainMethod() {
		TestContext testContext = new ExposedTestContextManager(UseMainMethodWhenAvailableAndMainMethod.class)
			.getExposedTestContext();
		ApplicationContext applicationContext = testContext.getApplicationContext();
		assertThat(applicationContext.getEnvironment().getActiveProfiles()).contains("frommain");
	}

	@Test
	void whenUseMainMethodNever() {
		TestContext testContext = new ExposedTestContextManager(UseMainMethodNever.class).getExposedTestContext();
		ApplicationContext applicationContext = testContext.getApplicationContext();
		assertThat(applicationContext.getEnvironment().getActiveProfiles()).isEmpty();
	}

	@Test
	void whenUseMainMethodWithBeanThrowingException() {
		TestContext testContext = new ExposedTestContextManager(UseMainMethodWithBeanThrowingException.class)
			.getExposedTestContext();
		assertThatIllegalStateException().isThrownBy(testContext::getApplicationContext)
			.havingCause()
			.satisfies((exception) -> {
				assertThat(exception).isInstanceOf(BeanCreationException.class);
				assertThat(exception).isSameAs(ContextLoaderApplicationContextFailureProcessor.contextLoadException);
			});
		assertThat(ContextLoaderApplicationContextFailureProcessor.failedContext).isNotNull();
	}

	@Test
	void whenNoMainMethodWithBeanThrowingException() {
		TestContext testContext = new ExposedTestContextManager(NoMainMethodWithBeanThrowingException.class)
			.getExposedTestContext();
		assertThatIllegalStateException().isThrownBy(testContext::getApplicationContext)
			.havingCause()
			.satisfies((exception) -> {
				assertThat(exception).isInstanceOf(BeanCreationException.class);
				assertThat(exception).isSameAs(ContextLoaderApplicationContextFailureProcessor.contextLoadException);
			});
		assertThat(ContextLoaderApplicationContextFailureProcessor.failedContext).isNotNull();
	}

	@Test
	void whenUseMainMethodWithContextHierarchyThrowsException() {
		TestContext testContext = new ExposedTestContextManager(UseMainMethodWithContextHierarchy.class)
			.getExposedTestContext();
		assertThatIllegalStateException().isThrownBy(testContext::getApplicationContext)
			.havingCause()
			.withMessage("UseMainMethod.ALWAYS cannot be used with @ContextHierarchy tests");
	}

	@Test
	void whenMainMethodNotAvailableReturnsNoAotContribution() throws Exception {
		SpringBootContextLoader contextLoader = new SpringBootContextLoader();
		MergedContextConfiguration contextConfiguration = BootstrapUtils
			.resolveTestContextBootstrapper(UseMainMethodWhenAvailableAndNoMainMethod.class)
			.buildMergedContextConfiguration();
		RuntimeHints runtimeHints = mock(RuntimeHints.class);
		contextLoader.loadContextForAotProcessing(contextConfiguration, runtimeHints);
		then(runtimeHints).shouldHaveNoInteractions();
	}

	@Test
	void whenMainMethodPresentRegisterReflectionHints() throws Exception {
		SpringBootContextLoader contextLoader = new SpringBootContextLoader();
		MergedContextConfiguration contextConfiguration = BootstrapUtils
			.resolveTestContextBootstrapper(UseMainMethodWhenAvailableAndMainMethod.class)
			.buildMergedContextConfiguration();
		RuntimeHints runtimeHints = new RuntimeHints();
		contextLoader.loadContextForAotProcessing(contextConfiguration, runtimeHints);
		assertThat(RuntimeHintsPredicates.reflection().onMethodInvocation(ConfigWithMain.class, "main"))
			.accepts(runtimeHints);
	}

	@Test
	void whenSubclassProvidesCustomApplicationContextFactory() {
		TestContext testContext = new ExposedTestContextManager(CustomApplicationContextTest.class)
			.getExposedTestContext();
		assertThat(testContext.getApplicationContext()).isInstanceOf(CustomAnnotationConfigApplicationContext.class);
	}

	private String[] getActiveProfiles(Class<?> testClass) {
		TestContext testContext = new ExposedTestContextManager(testClass).getExposedTestContext();
		ApplicationContext applicationContext = testContext.getApplicationContext();
		return applicationContext.getEnvironment().getActiveProfiles();
	}

	private Map<String, Object> getMergedContextConfigurationProperties(Class<?> testClass) {
		TestContext context = new ExposedTestContextManager(testClass).getExposedTestContext();
		MergedContextConfiguration config = (MergedContextConfiguration) ReflectionTestUtils.getField(context,
				"mergedConfig");
		assertThat(config).isNotNull();
		return TestPropertySourceUtils.convertInlinedPropertiesToMap(config.getPropertySourceProperties());
	}

	private void assertKey(Map<String, Object> actual, String key, Object value) {
		assertThat(actual).as("Key '" + key + "' not found").containsKey(key);
		assertThat(actual).containsEntry(key, value);
	}

	@SpringBootTest(properties = { "key=myValue", "anotherKey:anotherValue" }, classes = Config.class)
	static class SimpleConfig {

	}

	@SpringBootTest(properties = { "key=myValue", "anotherKey:anotherValue" }, classes = Config.class)
	static class SimpleConfigNonAlias {

	}

	@SpringBootTest(properties = "server.port=2345", classes = Config.class)
	static class OverrideConfig {

	}

	@SpringBootTest(properties = { "key=myValue", "otherKey=otherValue" }, classes = Config.class)
	static class AppendConfig {

	}

	@SpringBootTest(properties = { "key=my=Value", "anotherKey:another:Value" }, classes = Config.class)
	static class SameSeparatorInValue {

	}

	@SpringBootTest(properties = { "key=my:Value", "anotherKey:another=Value" }, classes = Config.class)
	static class AnotherSeparatorInValue {

	}

	@SpringBootTest(properties = { "key=myValue", "variables=foo=FOO\n bar=BAR" }, classes = Config.class)
	static class NewLineInValue {

	}

	@SpringBootTest(classes = Config.class)
	@ActiveProfiles({ "profile1", "profile2" })
	static class MultipleActiveProfiles {

	}

	@SpringBootTest(properties = { "key=myValue" }, classes = Config.class)
	@ActiveProfiles({ "profile1" })
	static class ActiveProfileWithInlinedProperties {

	}

	@SpringBootTest(classes = Config.class, args = "args", properties = "one=1")
	@TestPropertySource(properties = "two=2")
	static class PropertySourceOrdering {

	}

	@SpringBootTest(classes = Config.class, args = "--spring.main.web-application-type=none")
	static class ChangingWebApplicationTypeToNone {

	}

	@SpringBootTest(classes = Config.class, args = "--spring.main.web-application-type=reactive")
	static class ChangingWebApplicationTypeToReactive {

	}

	@SpringBootTest(classes = ConfigWithMainThrowingException.class, useMainMethod = UseMainMethod.ALWAYS)
	static class UseMainMethodAlwaysAndMainMethodThrowsException {

	}

	@SpringBootTest(classes = ConfigWithNoMain.class, useMainMethod = UseMainMethod.WHEN_AVAILABLE)
	static class UseMainMethodWhenAvailableAndNoMainMethod {

	}

	@SpringBootTest(classes = ConfigWithMain.class, useMainMethod = UseMainMethod.WHEN_AVAILABLE)
	static class UseMainMethodWhenAvailableAndMainMethod {

	}

	@SpringBootTest(classes = ConfigWithMain.class, useMainMethod = UseMainMethod.NEVER)
	static class UseMainMethodNever {

	}

	@SpringBootTest(classes = ConfigWithMainWithBeanThrowingException.class, useMainMethod = UseMainMethod.ALWAYS)
	static class UseMainMethodWithBeanThrowingException {

	}

	@SpringBootTest(classes = ConfigWithNoMainWithBeanThrowingException.class, useMainMethod = UseMainMethod.NEVER)
	static class NoMainMethodWithBeanThrowingException {

	}

	@SpringBootTest(useMainMethod = UseMainMethod.ALWAYS)
	@ContextHierarchy({ @ContextConfiguration(classes = ConfigWithMain.class),
			@ContextConfiguration(classes = AnotherConfigWithMain.class) })
	static class UseMainMethodWithContextHierarchy {

	}

	@SpringBootTest
	@ContextConfiguration(classes = Config.class, loader = CustomApplicationContextSpringBootContextLoader.class)
	static class CustomApplicationContextTest {

	}

	static class CustomApplicationContextSpringBootContextLoader extends SpringBootContextLoader {

		@Override
		protected ApplicationContextFactory getApplicationContextFactory(MergedContextConfiguration mergedConfig) {
			return (webApplicationType) -> new CustomAnnotationConfigApplicationContext();
		}

	}

	static class CustomAnnotationConfigApplicationContext extends AnnotationConfigApplicationContext {

	}

	@Configuration(proxyBeanMethods = false)
	static class Config {

	}

	@SpringBootConfiguration(proxyBeanMethods = false)
	public static class ConfigWithMain {

		public static void main(String[] args) {
			new SpringApplication(ConfigWithMain.class).run("--spring.profiles.active=frommain");
		}

	}

	@SpringBootConfiguration(proxyBeanMethods = false)
	public static class AnotherConfigWithMain {

		public static void main(String[] args) {
			new SpringApplication(AnotherConfigWithMain.class).run("--spring.profiles.active=anotherfrommain");
		}

	}

	@SpringBootConfiguration(proxyBeanMethods = false)
	static class ConfigWithNoMain {

	}

	@SpringBootConfiguration(proxyBeanMethods = false)
	public static class ConfigWithMainWithBeanThrowingException {

		public static void main(String[] args) {
			new SpringApplication(ConfigWithMainWithBeanThrowingException.class).run();
		}

		@Bean
		String failContextLoad() {
			throw new RuntimeException("ThrownFromBeanMethod");
		}

	}

	@SpringBootConfiguration(proxyBeanMethods = false)
	static class ConfigWithNoMainWithBeanThrowingException {

		@Bean
		String failContextLoad() {
			throw new RuntimeException("ThrownFromBeanMethod");
		}

	}

	@SpringBootConfiguration(proxyBeanMethods = false)
	public static class ConfigWithMainThrowingException {

		public static void main(String[] args) {
			throw new RuntimeException("ThrownFromMain");
		}

	}

	/**
	 * {@link TestContextManager} which exposes the {@link TestContext}.
	 */
	static class ExposedTestContextManager extends TestContextManager {

		ExposedTestContextManager(Class<?> testClass) {
			super(testClass);
		}

		final TestContext getExposedTestContext() {
			return super.getTestContext();
		}

	}

	private static final class ContextLoaderApplicationContextFailureProcessor
			implements ApplicationContextFailureProcessor {

		static @Nullable ApplicationContext failedContext;

		static @Nullable Throwable contextLoadException;

		@Override
		public void processLoadFailure(ApplicationContext context, @Nullable Throwable exception) {
			failedContext = context;
			contextLoadException = exception;
		}

		private static void reset() {
			failedContext = null;
			contextLoadException = null;
		}

	}

}
