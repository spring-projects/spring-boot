/*
 * Copyright 2012-2022 the original author or authors.
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

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest.UseMainMethod;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SpringBootTest} when using {@link UseMainMethod#ALWAYS} and setting
 * properties.
 *
 * @author Phillip Webb
 */
@SpringBootTest(properties = "test=123", useMainMethod = UseMainMethod.ALWAYS)
class SpringBootTestUseMainMethodWithPropertiesTests {

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void propertyIsSet() {
		assertThat(this.applicationContext.getEnvironment().getProperty("test")).isEqualTo("123");
	}

	@SpringBootConfiguration(proxyBeanMethods = false)
	public static class ConfigWithMain {

		public static void main(String[] args) {
			new SpringApplication(ConfigWithMain.class).run();
		}

	}

}
