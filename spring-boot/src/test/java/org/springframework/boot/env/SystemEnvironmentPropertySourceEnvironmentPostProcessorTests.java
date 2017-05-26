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

package org.springframework.boot.env;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import org.springframework.boot.origin.OriginTrackedValue;
import org.springframework.boot.origin.SystemEnvironmentOrigin;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SystemEnvironmentPropertySourceEnvironmentPostProcessor}.
 *
 * @author Madhura Bhave
 */
public class SystemEnvironmentPropertySourceEnvironmentPostProcessorTests {

	private ConfigurableEnvironment environment;

	@Before
	public void setUp() throws Exception {
		this.environment = new StandardEnvironment();
	}

	@Test
	public void postProcessShouldReplaceSystemEnviromentPropertySource() throws Exception {
		SystemEnvironmentPropertySourceEnvironmentPostProcessor postProcessor = new SystemEnvironmentPropertySourceEnvironmentPostProcessor();
		postProcessor.postProcessEnvironment(this.environment, null);
		PropertySource<?> replaced = this.environment.getPropertySources().get("systemEnvironment");
		assertThat(replaced).isInstanceOf(OriginTrackedSystemPropertySource.class);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void replacedPropertySourceShouldHaveOriginTrackedValues() throws Exception {
		SystemEnvironmentPropertySourceEnvironmentPostProcessor postProcessor = new SystemEnvironmentPropertySourceEnvironmentPostProcessor();
		PropertySource<?> original = this.environment.getPropertySources().get("systemEnvironment");
		postProcessor.postProcessEnvironment(this.environment, null);
		PropertySource<?> replaced = this.environment.getPropertySources().get("systemEnvironment");
		Map<String, Object> originalMap = (Map<String, Object>) original.getSource();
		Map<String, OriginTrackedValue> replacedMap = (Map<String, OriginTrackedValue>) replaced.getSource();
		for (Map.Entry<String, Object> entry : originalMap.entrySet()) {
			OriginTrackedValue actual = replacedMap.get(entry.getKey());
			assertThat(actual.getValue()).isEqualTo(entry.getValue());
			assertThat(actual.getOrigin()).isEqualTo(new SystemEnvironmentOrigin(entry.getKey()));
		}
	}

}
