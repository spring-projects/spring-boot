/*
 * Copyright 2012-2021 the original author or authors.
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

import org.junit.jupiter.api.Test;

import org.springframework.core.env.ConfigurablePropertyResolver;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.mock.env.MockPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

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
		assertThat(propertySource.getCount("missing")).isEqualTo(1);
	}

	@Test
	void containsPropertyWhenValidConfigurationPropertyName() {
		ResolverEnvironment environment = new ResolverEnvironment();
		CountingMockPropertySource propertySource = createMockPropertySource(environment, true);
		assertThat(environment.containsProperty("spring")).isTrue();
		assertThat(environment.containsProperty("sprong")).isFalse();
		assertThat(propertySource.getCount("spring")).isEqualTo(1);
		assertThat(propertySource.getCount("sprong")).isEqualTo(1);
	}

	@Test
	void containsPropertyWhenNotValidConfigurationPropertyName() {
		ResolverEnvironment environment = new ResolverEnvironment();
		CountingMockPropertySource propertySource = createMockPropertySource(environment, true);
		assertThat(environment.containsProperty("spr!ng")).isTrue();
		assertThat(environment.containsProperty("spr*ng")).isFalse();
		assertThat(propertySource.getCount("spr!ng")).isEqualTo(1);
		assertThat(propertySource.getCount("spr*ng")).isEqualTo(1);
	}

	@Test
	void getPropertyWhenValidConfigurationPropertyName() {
		ResolverEnvironment environment = new ResolverEnvironment();
		CountingMockPropertySource propertySource = createMockPropertySource(environment, true);
		assertThat(environment.getProperty("spring")).isEqualTo("boot");
		assertThat(environment.getProperty("sprong")).isNull();
		assertThat(propertySource.getCount("spring")).isEqualTo(1);
		assertThat(propertySource.getCount("sprong")).isEqualTo(1);
	}

	@Test
	void getPropertyWhenNotValidConfigurationPropertyName() {
		ResolverEnvironment environment = new ResolverEnvironment();
		CountingMockPropertySource propertySource = createMockPropertySource(environment, true);
		assertThat(environment.getProperty("spr!ng")).isEqualTo("boot");
		assertThat(environment.getProperty("spr*ng")).isNull();
		assertThat(propertySource.getCount("spr!ng")).isEqualTo(1);
		assertThat(propertySource.getCount("spr*ng")).isEqualTo(1);
	}

	@Test
	void getPropertyWhenNotAttached() {
		ResolverEnvironment environment = new ResolverEnvironment();
		CountingMockPropertySource propertySource = createMockPropertySource(environment, false);
		assertThat(environment.getProperty("spring")).isEqualTo("boot");
		assertThat(environment.getProperty("sprong")).isNull();
		assertThat(propertySource.getCount("spring")).isEqualTo(1);
		assertThat(propertySource.getCount("sprong")).isEqualTo(1);
	}

	@Test // gh-26732
	void getPropertyAsTypeWhenHasPlaceholder() {
		ResolverEnvironment environment = new ResolverEnvironment();
		MockPropertySource propertySource = new MockPropertySource();
		propertySource.withProperty("v1", "1");
		propertySource.withProperty("v2", "${v1}");
		environment.getPropertySources().addFirst(propertySource);
		assertThat(environment.getProperty("v2")).isEqualTo("1");
		assertThat(environment.getProperty("v2", Integer.class)).isEqualTo(1);
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
		public Object getProperty(String name) {
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
