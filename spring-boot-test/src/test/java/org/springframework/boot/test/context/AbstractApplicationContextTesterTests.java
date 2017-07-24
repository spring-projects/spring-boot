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

package org.springframework.boot.test.context;

import java.util.UUID;

import com.google.gson.Gson;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.boot.context.annotation.UserConfigurations;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Abstract tests for {@link AbstractApplicationContextTester} implementations.
 *
 * @param <T> The tester type
 * @param <C> the context type
 * @param <A> the assertable context type
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
public abstract class AbstractApplicationContextTesterTests<T extends AbstractApplicationContextTester<T, C, A>, C extends ConfigurableApplicationContext, A extends AssertProviderApplicationContext<C>> {

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	@Test
	public void runWithSystemPropertiesShouldSetAndRemoveProperties() {
		String key = "test." + UUID.randomUUID().toString();
		assertThat(System.getProperties().containsKey(key)).isFalse();
		get().withSystemProperties(key + "=value").run((loaded) -> {
			assertThat(System.getProperties()).containsEntry(key, "value");
		});
		assertThat(System.getProperties().containsKey(key)).isFalse();
	}

	@Test
	public void runWithSystemPropertiesWhenContextFailsShouldRemoveProperties()
			throws Exception {
		String key = "test." + UUID.randomUUID().toString();
		assertThat(System.getProperties().containsKey(key)).isFalse();
		get().withSystemProperties(key + "=value")
				.withUserConfiguration(FailingConfig.class).run((loaded) -> {
					assertThat(loaded).hasFailed();
				});
		assertThat(System.getProperties().containsKey(key)).isFalse();
	}

	@Test
	public void runWithSystemPropertiesShouldRestoreOriginalProperties()
			throws Exception {
		String key = "test." + UUID.randomUUID().toString();
		System.setProperty(key, "value");
		try {
			assertThat(System.getProperties().getProperty(key)).isEqualTo("value");
			get().withSystemProperties(key + "=newValue").run((loaded) -> {
				assertThat(System.getProperties()).containsEntry(key, "newValue");
			});
			assertThat(System.getProperties().getProperty(key)).isEqualTo("value");
		}
		finally {
			System.clearProperty(key);
		}
	}

	@Test
	public void runWithSystemPropertiesWhenValueIsNullShouldRemoveProperty()
			throws Exception {
		String key = "test." + UUID.randomUUID().toString();
		System.setProperty(key, "value");
		try {
			assertThat(System.getProperties().getProperty(key)).isEqualTo("value");
			get().withSystemProperty(key, null).run((loaded) -> {
				assertThat(System.getProperties()).doesNotContainKey(key);
			});
			assertThat(System.getProperties().getProperty(key)).isEqualTo("value");
		}
		finally {
			System.clearProperty(key);
		}
	}

	@Test
	public void runWithMultiplePropertyValuesShouldAllAllValues() throws Exception {
		get().withPropertyValues("test.foo=1").withPropertyValues("test.bar=2")
				.run((loaded) -> {
					Environment environment = loaded.getEnvironment();
					assertThat(environment.getProperty("test.foo")).isEqualTo("1");
					assertThat(environment.getProperty("test.bar")).isEqualTo("2");
				});
	}

	@Test
	public void runWithPropertyValuesWhenHasExistingShouldReplaceValue()
			throws Exception {
		get().withPropertyValues("test.foo=1").withPropertyValues("test.foo=2")
				.run((loaded) -> {
					Environment environment = loaded.getEnvironment();
					assertThat(environment.getProperty("test.foo")).isEqualTo("2");
				});
	}

	@Test
	public void runWithConfigurationsShouldRegisterConfigurations() throws Exception {
		get().withUserConfiguration(FooConfig.class)
				.run((loaded) -> assertThat(loaded).hasBean("foo"));
	}

	@Test
	public void runWithMultipleConfigurationsShouldRegisterAllConfigurations()
			throws Exception {
		get().withUserConfiguration(FooConfig.class)
				.withConfiguration(UserConfigurations.of(BarConfig.class))
				.run((loaded) -> assertThat(loaded).hasBean("foo").hasBean("bar"));
	}

	@Test
	public void runWithFailedContextShouldReturnFailedAssertableContext()
			throws Exception {
		get().withUserConfiguration(FailingConfig.class)
				.run((loaded) -> assertThat(loaded).hasFailed());
	}

	@Test
	public void runWithClassLoaderShouldSetClassLoader() throws Exception {
		get().withClassLoader(
				new HidePackagesClassLoader(Gson.class.getPackage().getName()))
				.run((loaded) -> {
					try {
						ClassUtils.forName(Gson.class.getName(), loaded.getClassLoader());
						fail("Should have thrown a ClassNotFoundException");
					}
					catch (ClassNotFoundException e) {
						// expected
					}
				});
	}

	protected abstract T get();

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

}
