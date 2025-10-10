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

package org.springframework.boot.gson.autoconfigure;

import com.google.gson.Gson;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.testsupport.classpath.ClassPathExclusions;
import org.springframework.boot.testsupport.classpath.ClassPathOverrides;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link GsonAutoConfiguration} with Gson 2.10.
 *
 * @author Andy Wilkinson
 */
@ClassPathExclusions("gson-*.jar")
@ClassPathOverrides("com.google.code.gson:gson:2.10")
class Gson210AutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(GsonAutoConfiguration.class));

	@Test
	void gsonRegistration() {
		this.contextRunner.run((context) -> {
			Gson gson = context.getBean(Gson.class);
			assertThat(gson.toJson(new DataObject())).isEqualTo("{\"data\":1}");
		});
	}

	public class DataObject {

		@SuppressWarnings("unused")
		private Long data = 1L;

		@SuppressWarnings("unused")
		private final @Nullable String owner = null;

		public void setData(Long data) {
			this.data = data;
		}

	}

}
