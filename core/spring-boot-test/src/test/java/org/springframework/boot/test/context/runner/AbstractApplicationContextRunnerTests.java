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

package org.springframework.boot.test.context.runner;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanCurrentlyInCreationException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.BeanDefinitionOverrideException;
import org.springframework.boot.context.annotation.Configurations;
import org.springframework.boot.context.annotation.UserConfigurations;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.assertj.ApplicationContextAssertProvider;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIOException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Abstract tests for {@link AbstractApplicationContextRunner} implementations.
 *
 * @param <T> The runner type
 * @param <C> the context type
 * @param <A> the assertable context type
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
abstract class AbstractApplicationContextRunnerTests<T extends AbstractApplicationContextRunner<T, C, A>, C extends ConfigurableApplicationContext, A extends ApplicationContextAssertProvider<C>> {

	@Test
	void runWithInitializerShouldInitialize() {
		AtomicBoolean called = new AtomicBoolean();
		get().withInitializer((context) -> called.set(true)).run((context) -> {
		});
		assertThat(called).isTrue();
	}

	@Test
	void runWithSystemPropertiesShouldSetAndRemoveProperties() {
		String key = "test." + UUID.randomUUID();
		assertThat(System.getProperties()).doesNotContainKey(key);
		get().withSystemProperties(key + "=value")
			.run((context) -> assertThat(System.getProperties()).containsEntry(key, "value"));
		assertThat(System.getProperties()).doesNotContainKey(key);
	}

	@Test
	void runWithSystemPropertiesWhenContextFailsShouldRemoveProperties() {
		String key = "test." + UUID.randomUUID();
		assertThat(System.getProperties()).doesNotContainKey(key);
		get().withSystemProperties(key + "=value")
			.withUserConfiguration(FailingConfig.class)
			.run((context) -> assertThat(context).hasFailed());
		assertThat(System.getProperties()).doesNotContainKey(key);
	}

	@Test
	void runWithSystemPropertiesShouldRestoreOriginalProperties() {
		String key = "test." + UUID.randomUUID();
		System.setProperty(key, "value");
		try {
			assertThat(System.getProperties().getProperty(key)).isEqualTo("value");
			get().withSystemProperties(key + "=newValue")
				.run((context) -> assertThat(System.getProperties()).containsEntry(key, "newValue"));
			assertThat(System.getProperties().getProperty(key)).isEqualTo("value");
		}
		finally {
			System.clearProperty(key);
		}
	}

	@Test
	void runWithSystemPropertiesWhenValueIsNullShouldRemoveProperty() {
		String key = "test." + UUID.randomUUID();
		System.setProperty(key, "value");
		try {
			assertThat(System.getProperties().getProperty(key)).isEqualTo("value");
			get().withSystemProperties(key + "=")
				.run((context) -> assertThat(System.getProperties()).doesNotContainKey(key));
			assertThat(System.getProperties().getProperty(key)).isEqualTo("value");
		}
		finally {
			System.clearProperty(key);
		}
	}

	@Test
	void runWithMultiplePropertyValuesShouldAllAllValues() {
		get().withPropertyValues("test.foo=1").withPropertyValues("test.bar=2").run((context) -> {
			Environment environment = context.getEnvironment();
			assertThat(environment.getProperty("test.foo")).isEqualTo("1");
			assertThat(environment.getProperty("test.bar")).isEqualTo("2");
		});
	}

	@Test
	void runWithPropertyValuesWhenHasExistingShouldReplaceValue() {
		get().withPropertyValues("test.foo=1").withPropertyValues("test.foo=2").run((context) -> {
			Environment environment = context.getEnvironment();
			assertThat(environment.getProperty("test.foo")).isEqualTo("2");
		});
	}

	@Test
	void runWithConfigurationsShouldRegisterConfigurations() {
		get().withUserConfiguration(FooConfig.class).run((context) -> assertThat(context).hasBean("foo"));
	}

	@Test
	void runWithUserConfigurationsRegistersDefaultBeanName() {
		get().withUserConfiguration(FooConfig.class)
			.run((context) -> assertThat(context).hasBean("abstractApplicationContextRunnerTests.FooConfig"));
	}

	@Test
	void runWithUserConfigurationsWhenHasSameShortClassNamedRegistersWithoutBeanName() {
		get()
			.withUserConfiguration(org.springframework.boot.test.context.example.duplicate.first.EmptyConfig.class,
					org.springframework.boot.test.context.example.duplicate.second.EmptyConfig.class)
			.run((context) -> assertThat(context.getStartupFailure())
				.isInstanceOf(BeanDefinitionOverrideException.class));
	}

	@Test
	void runFullyQualifiedNameConfigurationsRegistersFullyQualifiedBeanName() {
		get().withConfiguration(FullyQualifiedNameConfigurations.of(FooConfig.class))
			.run((context) -> assertThat(context).hasBean(FooConfig.class.getName()));
	}

	@Test
	void runWithFullyQualifiedNameConfigurationsWhenHasSameShortClassNamedRegistersWithFullyQualifiedBeanName() {
		get()
			.withConfiguration(FullyQualifiedNameConfigurations.of(
					org.springframework.boot.test.context.example.duplicate.first.EmptyConfig.class,
					org.springframework.boot.test.context.example.duplicate.second.EmptyConfig.class))
			.run((context) -> assertThat(context)
				.hasSingleBean(org.springframework.boot.test.context.example.duplicate.first.EmptyConfig.class)
				.hasSingleBean(org.springframework.boot.test.context.example.duplicate.second.EmptyConfig.class));
	}

	@Test
	void runWithUserNamedBeanShouldRegisterBean() {
		get().withBean("foo", String.class, () -> "foo").run((context) -> assertThat(context).hasBean("foo"));
	}

	@Test
	void runWithUserBeanShouldRegisterBeanWithDefaultName() {
		get().withBean(String.class, () -> "foo").run((context) -> assertThat(context).hasBean("string"));
	}

	@Test
	void runWithMultipleConfigurationsShouldRegisterAllConfigurations() {
		get().withUserConfiguration(FooConfig.class)
			.withConfiguration(UserConfigurations.of(BarConfig.class))
			.run((context) -> assertThat(context).hasBean("foo").hasBean("bar"));
	}

	@Test
	void runWithFailedContextShouldReturnFailedAssertableContext() {
		get().withUserConfiguration(FailingConfig.class).run((context) -> assertThat(context).hasFailed());
	}

	@Test
	void runWithClassLoaderShouldSetClassLoaderOnContext() {
		get().withClassLoader(new FilteredClassLoader(Gson.class.getPackage().getName()))
			.run((context) -> assertThatExceptionOfType(ClassNotFoundException.class)
				.isThrownBy(() -> ClassUtils.forName(Gson.class.getName(), context.getClassLoader())));
	}

	@Test
	void runWithClassLoaderShouldSetClassLoaderOnConditionContext() {
		get().withClassLoader(new FilteredClassLoader(Gson.class.getPackage().getName()))
			.withUserConfiguration(ConditionalConfig.class)
			.run((context) -> assertThat(context).hasSingleBean(ConditionalConfig.class));
	}

	@Test
	void consecutiveRunWithFilteredClassLoaderShouldHaveBeanWithLazyProperties() {
		get().withClassLoader(new FilteredClassLoader(Gson.class))
			.withUserConfiguration(LazyConfig.class)
			.run((context) -> assertThat(context).hasSingleBean(ExampleBeanWithLazyProperties.class));

		get().withClassLoader(new FilteredClassLoader(Gson.class))
			.withUserConfiguration(LazyConfig.class)
			.run((context) -> assertThat(context).hasSingleBean(ExampleBeanWithLazyProperties.class));
	}

	@Test
	void thrownRuleWorksWithCheckedException() {
		get().run((context) -> assertThatIOException().isThrownBy(() -> throwCheckedException("Expected message"))
			.withMessageContaining("Expected message"));
	}

	@Test
	void runDisablesBeanOverridingByDefault() {
		get().withUserConfiguration(FooConfig.class).withBean("foo", Integer.class, () -> 42).run((context) -> {
			assertThat(context).hasFailed();
			assertThat(context.getStartupFailure()).isInstanceOf(BeanDefinitionStoreException.class)
				.hasMessageContaining("Invalid bean definition with name 'foo'")
				.hasMessageContaining("@Bean definition illegally overridden by existing bean definition");
		});
	}

	@Test
	void runDisablesCircularReferencesByDefault() {
		get().withUserConfiguration(ExampleConsumerConfiguration.class, ExampleProducerConfiguration.class)
			.run((context) -> {
				assertThat(context).hasFailed();
				assertThat(context).getFailure().hasRootCauseInstanceOf(BeanCurrentlyInCreationException.class);
			});
	}

	@Test
	void circularReferencesCanBeAllowed() {
		get().withAllowCircularReferences(true)
			.withUserConfiguration(ExampleConsumerConfiguration.class, ExampleProducerConfiguration.class)
			.run((context) -> assertThat(context).hasNotFailed());
	}

	@Test
	void runWithUserBeanShouldBeRegisteredInOrder() {
		get().withAllowBeanDefinitionOverriding(true)
			.withBean(String.class, () -> "one")
			.withBean(String.class, () -> "two")
			.withBean(String.class, () -> "three")
			.run((context) -> {
				assertThat(context).hasBean("string");
				assertThat(context.getBean("string")).isEqualTo("three");
			});
	}

	@Test
	void runWithConfigurationsAndUserBeanShouldRegisterUserBeanLast() {
		get().withAllowBeanDefinitionOverriding(true)
			.withUserConfiguration(FooConfig.class)
			.withBean("foo", String.class, () -> "overridden")
			.run((context) -> {
				assertThat(context).hasBean("foo");
				assertThat(context.getBean("foo")).isEqualTo("overridden");
			});
	}

	@Test
	void changesMadeByInitializersShouldBeVisibleToRegisteredClasses() {
		get().withInitializer((context) -> context.getEnvironment().setActiveProfiles("test"))
			.withUserConfiguration(ProfileConfig.class)
			.run((context) -> assertThat(context).hasSingleBean(ProfileConfig.class));
	}

	@Test
	void prepareDoesNotRefreshContext() {
		get().withUserConfiguration(FooConfig.class).prepare((context) -> {
			assertThatIllegalStateException().isThrownBy(() -> context.getBean(String.class))
				.withMessageContaining("not been refreshed");
			context.getSourceApplicationContext().refresh();
			assertThat(context.getBean(String.class)).isEqualTo("foo");
		});
	}

	@Test
	void getWithAdditionalContextInterfaceHasCorrectInstanceOf() {
		getWithAdditionalContextInterface()
			.run((context) -> assertThat(context).isInstanceOf(AdditionalContextInterface.class));
	}

	protected abstract T get();

	protected abstract T getWithAdditionalContextInterface();

	private static void throwCheckedException(String message) throws IOException {
		throw new IOException(message);
	}

	@Configuration(proxyBeanMethods = false)
	static class FailingConfig {

		@Bean
		String foo() {
			throw new IllegalStateException("Failed");
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class FooConfig {

		@Bean
		String foo() {
			return "foo";
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class BarConfig {

		@Bean
		String bar() {
			return "bar";
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Conditional(FilteredClassLoaderCondition.class)
	static class ConditionalConfig {

	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(ExampleProperties.class)
	static class LazyConfig {

		@Bean
		ExampleBeanWithLazyProperties exampleBeanWithLazyProperties() {
			return new ExampleBeanWithLazyProperties();
		}

	}

	static class ExampleBeanWithLazyProperties {

		@Autowired
		@Lazy
		ExampleProperties exampleProperties;

	}

	@ConfigurationProperties
	public static class ExampleProperties {

	}

	static class FilteredClassLoaderCondition implements Condition {

		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
			return context.getClassLoader() instanceof FilteredClassLoader;
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

	@Profile("test")
	@Configuration(proxyBeanMethods = false)
	static class ProfileConfig {

	}

	static class FullyQualifiedNameConfigurations extends Configurations {

		protected FullyQualifiedNameConfigurations(Collection<Class<?>> classes) {
			super(null, classes, Class::getName);
		}

		@Override
		protected Configurations merge(Set<Class<?>> mergedClasses) {
			return new FullyQualifiedNameConfigurations(mergedClasses);
		}

		static FullyQualifiedNameConfigurations of(Class<?>... classes) {
			return new FullyQualifiedNameConfigurations(List.of(classes));
		}

	}

}
