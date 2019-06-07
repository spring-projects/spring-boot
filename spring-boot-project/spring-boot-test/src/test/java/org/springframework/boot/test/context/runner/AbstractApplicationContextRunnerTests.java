/*
 * Copyright 2012-2019 the original author or authors.
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
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.gson.Gson;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.boot.context.annotation.UserConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.assertj.ApplicationContextAssertProvider;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Abstract tests for {@link AbstractApplicationContextRunner} implementations.
 *
 * @param <T> The runner type
 * @param <C> the context type
 * @param <A> the assertable context type
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
public abstract class AbstractApplicationContextRunnerTests<T extends AbstractApplicationContextRunner<T, C, A>, C extends ConfigurableApplicationContext, A extends ApplicationContextAssertProvider<C>> {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void runWithInitializerShouldInitialize() {
		AtomicBoolean called = new AtomicBoolean();
		get().withInitializer((context) -> called.set(true)).run((context) -> {
		});
		assertThat(called).isTrue();
	}

	@Test
	public void runWithSystemPropertiesShouldSetAndRemoveProperties() {
		String key = "test." + UUID.randomUUID();
		assertThat(System.getProperties().containsKey(key)).isFalse();
		get().withSystemProperties(key + "=value")
				.run((context) -> assertThat(System.getProperties()).containsEntry(key, "value"));
		assertThat(System.getProperties().containsKey(key)).isFalse();
	}

	@Test
	public void runWithSystemPropertiesWhenContextFailsShouldRemoveProperties() {
		String key = "test." + UUID.randomUUID();
		assertThat(System.getProperties().containsKey(key)).isFalse();
		get().withSystemProperties(key + "=value").withUserConfiguration(FailingConfig.class)
				.run((context) -> assertThat(context).hasFailed());
		assertThat(System.getProperties().containsKey(key)).isFalse();
	}

	@Test
	public void runWithSystemPropertiesShouldRestoreOriginalProperties() {
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
	public void runWithSystemPropertiesWhenValueIsNullShouldRemoveProperty() {
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
	public void runWithMultiplePropertyValuesShouldAllAllValues() {
		get().withPropertyValues("test.foo=1").withPropertyValues("test.bar=2").run((context) -> {
			Environment environment = context.getEnvironment();
			assertThat(environment.getProperty("test.foo")).isEqualTo("1");
			assertThat(environment.getProperty("test.bar")).isEqualTo("2");
		});
	}

	@Test
	public void runWithPropertyValuesWhenHasExistingShouldReplaceValue() {
		get().withPropertyValues("test.foo=1").withPropertyValues("test.foo=2").run((context) -> {
			Environment environment = context.getEnvironment();
			assertThat(environment.getProperty("test.foo")).isEqualTo("2");
		});
	}

	@Test
	public void runWithConfigurationsShouldRegisterConfigurations() {
		get().withUserConfiguration(FooConfig.class).run((context) -> assertThat(context).hasBean("foo"));
	}

	@Test
	public void runWithMultipleConfigurationsShouldRegisterAllConfigurations() {
		get().withUserConfiguration(FooConfig.class).withConfiguration(UserConfigurations.of(BarConfig.class))
				.run((context) -> assertThat(context).hasBean("foo").hasBean("bar"));
	}

	@Test
	public void runWithFailedContextShouldReturnFailedAssertableContext() {
		get().withUserConfiguration(FailingConfig.class).run((context) -> assertThat(context).hasFailed());
	}

	@Test
	public void runWithClassLoaderShouldSetClassLoaderOnContext() {
		get().withClassLoader(new FilteredClassLoader(Gson.class.getPackage().getName())).run((context) -> {
			try {
				ClassUtils.forName(Gson.class.getName(), context.getClassLoader());
				fail("Should have thrown a ClassNotFoundException");
			}
			catch (ClassNotFoundException ex) {
				// expected
			}
		});
	}

	@Test
	public void runWithClassLoaderShouldSetClassLoaderOnConditionContext() {
		get().withClassLoader(new FilteredClassLoader(Gson.class.getPackage().getName()))
				.withUserConfiguration(ConditionalConfig.class)
				.run((context) -> assertThat(context).hasSingleBean(ConditionalConfig.class));
	}

	@Test
	public void thrownRuleWorksWithCheckedException() {
		get().run((context) -> {
			this.thrown.expect(IOException.class);
			this.thrown.expectMessage("Expected message");
			throwCheckedException("Expected message");
		});
	}

	protected abstract T get();

	private static void throwCheckedException(String message) throws IOException {
		throw new IOException(message);
	}

	@Configuration
	static class FailingConfig {

		@Bean
		public String foo() {
			throw new IllegalStateException("Failed");
		}

	}

	@Configuration
	static class FooConfig {

		@Bean
		public String foo() {
			return "foo";
		}

	}

	@Configuration
	static class BarConfig {

		@Bean
		public String bar() {
			return "bar";
		}

	}

	@Configuration
	@Conditional(FilteredClassLoaderCondition.class)
	static class ConditionalConfig {

	}

	static class FilteredClassLoaderCondition implements Condition {

		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
			return context.getClassLoader() instanceof FilteredClassLoader;
		}

	}

}
