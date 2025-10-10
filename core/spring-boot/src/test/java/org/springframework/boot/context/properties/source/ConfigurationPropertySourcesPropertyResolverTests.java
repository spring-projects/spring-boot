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

package org.springframework.boot.context.properties.source;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.env.ConfigurablePropertyResolver;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.mock.env.MockPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link ConfigurationPropertySourcesPropertyResolver}.
 *
 * @author Phillip Webb
 */
class ConfigurationPropertySourcesPropertyResolverTests {

	@Test
	void standardPropertyResolverResolvesMultipleTimes() {
		StandardEnvironment environment = new StandardEnvironment();
		CountingMockPropertySource propertySource = createMockPropertySource(environment, true);
		environment.getProperty("missing");
		assertThat(propertySource.getCount("missing")).isEqualTo(2);
	}

	@Test
	void configurationPropertySourcesPropertyResolverResolvesSingleTime() {
		ResolverEnvironment environment = new ResolverEnvironment();
		CountingMockPropertySource propertySource = createMockPropertySource(environment, true);
		environment.getProperty("missing");
		assertThat(propertySource.getCount("missing")).isOne();
	}

	@Test
	void containsPropertyWhenValidConfigurationPropertyName() {
		ResolverEnvironment environment = new ResolverEnvironment();
		CountingMockPropertySource propertySource = createMockPropertySource(environment, true);
		assertThat(environment.containsProperty("spring")).isTrue();
		assertThat(environment.containsProperty("sprong")).isFalse();
		assertThat(propertySource.getCount("spring")).isOne();
		assertThat(propertySource.getCount("sprong")).isOne();
	}

	@Test
	void containsPropertyWhenNotValidConfigurationPropertyName() {
		ResolverEnvironment environment = new ResolverEnvironment();
		CountingMockPropertySource propertySource = createMockPropertySource(environment, true);
		assertThat(environment.containsProperty("spr!ng")).isTrue();
		assertThat(environment.containsProperty("spr*ng")).isFalse();
		assertThat(propertySource.getCount("spr!ng")).isOne();
		assertThat(propertySource.getCount("spr*ng")).isOne();
	}

	@Test
	void getPropertyWhenValidConfigurationPropertyName() {
		ResolverEnvironment environment = new ResolverEnvironment();
		CountingMockPropertySource propertySource = createMockPropertySource(environment, true);
		assertThat(environment.getProperty("spring")).isEqualTo("boot");
		assertThat(environment.getProperty("sprong")).isNull();
		assertThat(propertySource.getCount("spring")).isOne();
		assertThat(propertySource.getCount("sprong")).isOne();
	}

	@Test
	void getPropertyWhenNotValidConfigurationPropertyName() {
		ResolverEnvironment environment = new ResolverEnvironment();
		CountingMockPropertySource propertySource = createMockPropertySource(environment, true);
		assertThat(environment.getProperty("spr!ng")).isEqualTo("boot");
		assertThat(environment.getProperty("spr*ng")).isNull();
		assertThat(propertySource.getCount("spr!ng")).isOne();
		assertThat(propertySource.getCount("spr*ng")).isOne();
	}

	@Test
	void getPropertyWhenNotAttached() {
		ResolverEnvironment environment = new ResolverEnvironment();
		CountingMockPropertySource propertySource = createMockPropertySource(environment, false);
		assertThat(environment.getProperty("spring")).isEqualTo("boot");
		assertThat(environment.getProperty("sprong")).isNull();
		assertThat(propertySource.getCount("spring")).isOne();
		assertThat(propertySource.getCount("sprong")).isOne();
	}

	@Test // gh-26732
	void getPropertyAsTypeWhenHasPlaceholder() {
		ResolverEnvironment environment = new ResolverEnvironment();
		MockPropertySource propertySource = new MockPropertySource();
		propertySource.withProperty("v1", "1");
		propertySource.withProperty("v2", "${v1}");
		environment.getPropertySources().addFirst(propertySource);
		assertThat(environment.getProperty("v2")).isEqualTo("1");
		assertThat(environment.getProperty("v2", Integer.class)).isOne();
	}

	@Test
	void throwsInvalidConfigurationPropertyValueExceptionWhenGetPropertyAsTypeFailsToConvert() {
		ResolverEnvironment environment = new ResolverEnvironment();
		MockPropertySource propertySource = new MockPropertySource();
		propertySource.withProperty("v1", "one");
		propertySource.withProperty("v2", "${v1}");
		environment.getPropertySources().addFirst(propertySource);
		assertThat(environment.getProperty("v2")).isEqualTo("one");
		assertThatExceptionOfType(ConversionFailedException.class)
			.isThrownBy(() -> environment.getProperty("v2", Integer.class))
			.satisfies((ex) -> {
				assertThat(ex.getValue()).isEqualTo("one");
				assertThat(ex.getSourceType()).isEqualTo(TypeDescriptor.valueOf(String.class));
				assertThat(ex.getTargetType()).isEqualTo(TypeDescriptor.valueOf(Integer.class));
			})
			.havingCause()
			.satisfies((ex) -> {
				InvalidConfigurationPropertyValueException invalidValueEx = (InvalidConfigurationPropertyValueException) ex;
				assertThat(invalidValueEx.getName()).isEqualTo("v2");
				assertThat(invalidValueEx.getValue()).isEqualTo("one");
				assertThat(ex).cause().isInstanceOf(NumberFormatException.class);
			});
	}

	private CountingMockPropertySource createMockPropertySource(StandardEnvironment environment, boolean attach) {
		CountingMockPropertySource propertySource = new CountingMockPropertySource();
		propertySource.withProperty("spring", "boot");
		propertySource.withProperty("spr!ng", "boot");
		environment.getPropertySources().addFirst(propertySource);
		if (attach) {
			ConfigurationPropertySources.attach(environment);
		}
		return propertySource;
	}

	static class ResolverEnvironment extends StandardEnvironment {

		@Override
		protected ConfigurablePropertyResolver createPropertyResolver(MutablePropertySources propertySources) {
			return new ConfigurationPropertySourcesPropertyResolver(propertySources);
		}

	}

	static class CountingMockPropertySource extends MockPropertySource {

		private final Map<String, AtomicInteger> counts = new HashMap<>();

		@Override
		public @Nullable Object getProperty(String name) {
			incrementCount(name);
			return super.getProperty(name);
		}

		@Override
		public boolean containsProperty(String name) {
			incrementCount(name);
			return super.containsProperty(name);
		}

		private void incrementCount(String name) {
			this.counts.computeIfAbsent(name, (k) -> new AtomicInteger()).incrementAndGet();
		}

		int getCount(String name) {
			AtomicInteger count = this.counts.get(name);
			return (count != null) ? count.get() : 0;
		}

	}

}
