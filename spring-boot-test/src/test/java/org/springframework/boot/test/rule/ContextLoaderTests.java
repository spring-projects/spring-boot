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

package org.springframework.boot.test.rule;

import java.util.UUID;

import com.google.gson.Gson;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.test.context.ContextLoader;
import org.springframework.boot.test.context.HidePackagesClassLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Tests for {@link ContextLoader}.
 *
 * @author Stephane Nicoll
 */
public class ContextLoaderTests {

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	private final ContextLoader contextLoader = new ContextLoader();

	@Test
	public void systemPropertyIsSetAndRemoved() {
		String key = "test." + UUID.randomUUID().toString();
		assertThat(System.getProperties().containsKey(key)).isFalse();
		this.contextLoader.systemProperty(key, "value").load(context -> {
			assertThat(System.getProperties().containsKey(key)).isTrue();
			assertThat(System.getProperties().getProperty(key)).isEqualTo("value");
		});
		assertThat(System.getProperties().containsKey(key)).isFalse();
	}

	@Test
	public void systemPropertyIsRemovedIfContextFailed() {
		String key = "test." + UUID.randomUUID().toString();
		assertThat(System.getProperties().containsKey(key)).isFalse();
		this.contextLoader.systemProperty(key, "value")
				.config(ConfigC.class).loadAndFail(e -> {
		});
		assertThat(System.getProperties().containsKey(key)).isFalse();
	}

	@Test
	public void systemPropertyIsRestoredToItsOriginalValue() {
		String key = "test." + UUID.randomUUID().toString();
		System.setProperty(key, "value");
		try {
			assertThat(System.getProperties().getProperty(key)).isEqualTo("value");
			this.contextLoader.systemProperty(key, "newValue").load(context -> {
				assertThat(System.getProperties().getProperty(key)).isEqualTo("newValue");
			});
			assertThat(System.getProperties().getProperty(key)).isEqualTo("value");
		}
		finally {
			System.clearProperty(key);
		}
	}

	@Test
	public void systemPropertyCanBeSetToNullValue() {
		String key = "test." + UUID.randomUUID().toString();
		assertThat(System.getProperties().containsKey(key)).isFalse();
		this.contextLoader.systemProperty(key, "value")
				.systemProperty(key, null).load(context -> {
			assertThat(System.getProperties().containsKey(key)).isFalse();
		});
	}

	@Test
	public void systemPropertyNeedNonNullKey() {
		this.thrown.expect(IllegalArgumentException.class);
		this.contextLoader.systemProperty(null, "value");
	}

	@Test
	public void envIsAdditive() {
		this.contextLoader.env("test.foo=1").env("test.bar=2").load(context -> {
			ConfigurableEnvironment environment = context.getBean(
					ConfigurableEnvironment.class);
			assertThat(environment.getProperty("test.foo", Integer.class)).isEqualTo(1);
			assertThat(environment.getProperty("test.bar", Integer.class)).isEqualTo(2);
		});
	}

	@Test
	public void envOverridesExistingKey() {
		this.contextLoader.env("test.foo=1").env("test.foo=2").load(context ->
				assertThat(context.getBean(ConfigurableEnvironment.class)
						.getProperty("test.foo", Integer.class)).isEqualTo(2));
	}

	@Test
	public void configurationIsProcessedInOrder() {
		this.contextLoader.config(ConfigA.class, AutoConfigA.class).load(context ->
				assertThat(context.getBean("a")).isEqualTo("autoconfig-a"));
	}

	@Test
	public void configurationIsProcessedBeforeAutoConfiguration() {
		this.contextLoader.autoConfig(AutoConfigA.class)
				.config(ConfigA.class).load(context ->
				assertThat(context.getBean("a")).isEqualTo("autoconfig-a"));
	}

	@Test
	public void configurationIsAdditive() {
		this.contextLoader.config(AutoConfigA.class)
				.config(AutoConfigB.class).load(context -> {
			assertThat(context.containsBean("a")).isTrue();
			assertThat(context.containsBean("b")).isTrue();
		});
	}

	@Test
	public void autoConfigureFirstIsAppliedProperly() {
		this.contextLoader.autoConfig(ConfigA.class)
				.autoConfigFirst(AutoConfigA.class).load(context ->
				assertThat(context.getBean("a")).isEqualTo("a"));
	}

	@Test
	public void autoConfigureFirstWithSeveralConfigsIsAppliedProperly() {
		this.contextLoader.autoConfig(ConfigA.class, ConfigB.class)
				.autoConfigFirst(AutoConfigA.class, AutoConfigB.class)
				.load(context -> {
					assertThat(context.getBean("a")).isEqualTo("a");
					assertThat(context.getBean("b")).isEqualTo(1);
				});
	}

	@Test
	public void autoConfigurationIsAdditive() {
		this.contextLoader.autoConfig(AutoConfigA.class)
				.autoConfig(AutoConfigB.class).load(context -> {
			assertThat(context.containsBean("a")).isTrue();
			assertThat(context.containsBean("b")).isTrue();
		});
	}

	@Test
	public void loadAndFailWithExpectedException() {
		this.contextLoader.config(ConfigC.class)
				.loadAndFail(BeanCreationException.class, ex ->
						assertThat(ex.getMessage()).contains("Error creating bean with name 'c'"));
	}

	@Test
	public void loadAndFailWithWrongException() {
		this.thrown.expect(AssertionError.class);
		this.thrown.expectMessage("Wrong application context failure exception");
		this.contextLoader.config(ConfigC.class)
				.loadAndFail(IllegalArgumentException.class, ex -> {
				});
	}

	@Test
	public void classLoaderIsUsed() {
		this.contextLoader.classLoader(new HidePackagesClassLoader(
				Gson.class.getPackage().getName())).load(context -> {
			try {
				ClassUtils.forName(Gson.class.getName(), context.getClassLoader());
				fail("Should have thrown a ClassNotFoundException");
			}
			catch (ClassNotFoundException e) {
				// expected
			}
		});
	}

	@Configuration
	static class ConfigA {

		@Bean
		public String a() {
			return "a";
		}

	}

	@Configuration
	static class ConfigB {

		@Bean
		public Integer b() {
			return 1;
		}

	}

	@Configuration
	static class AutoConfigA {

		@Bean
		public String a() {
			return "autoconfig-a";
		}

	}

	@Configuration
	static class AutoConfigB {

		@Bean
		public Integer b() {
			return 42;
		}

	}

	@Configuration
	static class ConfigC {

		@Bean
		public String c(Integer value) {
			return String.valueOf(value);
		}
	}

}
