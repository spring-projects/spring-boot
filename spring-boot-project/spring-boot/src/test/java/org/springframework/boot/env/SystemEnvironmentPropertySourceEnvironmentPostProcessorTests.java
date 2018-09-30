/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.env;

import java.util.Collections;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import org.springframework.boot.env.SystemEnvironmentPropertySourceEnvironmentPostProcessor.OriginAwareSystemEnvironmentPropertySource;
import org.springframework.boot.origin.SystemEnvironmentOrigin;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SystemEnvironmentPropertySourceEnvironmentPostProcessor}.
 *
 * @author Madhura Bhave
 */
public class SystemEnvironmentPropertySourceEnvironmentPostProcessorTests {

	private ConfigurableEnvironment environment;

	@Before
	public void setUp() {
		this.environment = new StandardEnvironment();
	}

	@Test
	public void postProcessShouldReplaceSystemEnvironmentPropertySource() {
		SystemEnvironmentPropertySourceEnvironmentPostProcessor postProcessor = new SystemEnvironmentPropertySourceEnvironmentPostProcessor();
		postProcessor.postProcessEnvironment(this.environment, null);
		PropertySource<?> replaced = this.environment.getPropertySources()
				.get("systemEnvironment");
		assertThat(replaced)
				.isInstanceOf(OriginAwareSystemEnvironmentPropertySource.class);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void replacedPropertySourceShouldBeOriginAware() {
		SystemEnvironmentPropertySourceEnvironmentPostProcessor postProcessor = new SystemEnvironmentPropertySourceEnvironmentPostProcessor();
		PropertySource<?> original = this.environment.getPropertySources()
				.get("systemEnvironment");
		postProcessor.postProcessEnvironment(this.environment, null);
		OriginAwareSystemEnvironmentPropertySource replaced = (OriginAwareSystemEnvironmentPropertySource) this.environment
				.getPropertySources().get("systemEnvironment");
		Map<String, Object> originalMap = (Map<String, Object>) original.getSource();
		Map<String, Object> replacedMap = replaced.getSource();
		originalMap.forEach((key, value) -> {
			Object actual = replacedMap.get(key);
			assertThat(actual).isEqualTo(value);
			assertThat(replaced.getOrigin(key))
					.isInstanceOf(SystemEnvironmentOrigin.class);
		});
	}

	@Test
	public void replacedPropertySourceWhenPropertyAbsentShouldReturnNullOrigin() {
		SystemEnvironmentPropertySourceEnvironmentPostProcessor postProcessor = new SystemEnvironmentPropertySourceEnvironmentPostProcessor();
		postProcessor.postProcessEnvironment(this.environment, null);
		OriginAwareSystemEnvironmentPropertySource replaced = (OriginAwareSystemEnvironmentPropertySource) this.environment
				.getPropertySources().get("systemEnvironment");
		assertThat(replaced.getOrigin("NON_EXISTENT")).isNull();
	}

	@Test
	public void replacedPropertySourceShouldResolveProperty() {
		SystemEnvironmentPropertySourceEnvironmentPostProcessor postProcessor = new SystemEnvironmentPropertySourceEnvironmentPostProcessor();
		Map<String, Object> source = Collections.singletonMap("FOO_BAR_BAZ", "hello");
		this.environment.getPropertySources().replace("systemEnvironment",
				new SystemEnvironmentPropertySource("systemEnvironment", source));
		postProcessor.postProcessEnvironment(this.environment, null);
		OriginAwareSystemEnvironmentPropertySource replaced = (OriginAwareSystemEnvironmentPropertySource) this.environment
				.getPropertySources().get("systemEnvironment");
		SystemEnvironmentOrigin origin = (SystemEnvironmentOrigin) replaced
				.getOrigin("foo.bar.baz");
		assertThat(origin.getProperty()).isEqualTo("FOO_BAR_BAZ");
		assertThat(replaced.getProperty("foo.bar.baz")).isEqualTo("hello");
	}

}
