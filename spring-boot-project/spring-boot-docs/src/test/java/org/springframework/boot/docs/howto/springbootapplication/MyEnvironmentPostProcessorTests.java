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

package org.springframework.boot.docs.howto.springbootapplication;

import org.junit.jupiter.api.Test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.docs.howto.application.customizetheenvironmentorapplicationcontext.MyEnvironmentPostProcessor;
import org.springframework.core.env.StandardEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MyEnvironmentPostProcessor}.
 *
 * @author Stephane Nicoll
 */
class MyEnvironmentPostProcessorTests {

	private final StandardEnvironment environment = new StandardEnvironment();

	@Test
	void applyEnvironmentPostProcessor() {
		assertThat(this.environment.containsProperty("test.foo.bar")).isFalse();
		new MyEnvironmentPostProcessor().postProcessEnvironment(this.environment, new SpringApplication());
		assertThat(this.environment.containsProperty("test.foo.bar")).isTrue();
		assertThat(this.environment.getProperty("test.foo.bar")).isEqualTo("value");
	}

}
