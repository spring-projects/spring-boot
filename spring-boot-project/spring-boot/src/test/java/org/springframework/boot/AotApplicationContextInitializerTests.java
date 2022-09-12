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

package org.springframework.boot;

import org.junit.jupiter.api.Test;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link AotApplicationContextInitializer}.
 *
 * @author Phillip Webb
 */
class AotApplicationContextInitializerTests {

	@Test
	void forMainApplicationClassUsesReflection() {
		AotApplicationContextInitializer<ConfigurableApplicationContext> initializer = AotApplicationContextInitializer
				.forMainApplicationClass(ExampleMain.class);
		GenericApplicationContext applicationContext = new GenericApplicationContext();
		initializer.initialize(applicationContext);
		assertThat(initializer.getName()).isEqualTo(ExampleMain__ApplicationContextInitializer.class.getName());
		assertThat(applicationContext.getId()).isEqualTo("ExampleMain");
	}

	@Test
	void ofWhenInitializerIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> AotApplicationContextInitializer.of(null))
				.withMessage("Initializer must not be null");
	}

	@Test
	void ofWithNameWhenInitializerIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> AotApplicationContextInitializer.of("test", null))
				.withMessage("Initializer must not be null");
	}

	@Test
	void ofWithNameWhenNameIsNullThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> AotApplicationContextInitializer.of(null, new CustomApplicationContextInitializer()))
				.withMessage("Name must not be null");
	}

	@Test
	void ofUsesDelegation() {
		AotApplicationContextInitializer<ConfigurableApplicationContext> initializer = AotApplicationContextInitializer
				.of(new CustomApplicationContextInitializer());
		GenericApplicationContext applicationContext = new GenericApplicationContext();
		initializer.initialize(applicationContext);
		assertThat(initializer.getName()).isEqualTo(CustomApplicationContextInitializer.class.getName());
		assertThat(applicationContext.getId()).isEqualTo("Custom");

	}

	static class ExampleMain {

	}

	static class ExampleMain__ApplicationContextInitializer
			implements ApplicationContextInitializer<ConfigurableApplicationContext> {

		@Override
		public void initialize(ConfigurableApplicationContext applicationContext) {
			applicationContext.setId("ExampleMain");
		}

	}

	static class CustomApplicationContextInitializer
			implements ApplicationContextInitializer<ConfigurableApplicationContext> {

		@Override
		public void initialize(ConfigurableApplicationContext applicationContext) {
			applicationContext.setId("Custom");
		}

	}

}
