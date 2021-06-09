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

package org.springframework.boot;

import org.assertj.core.api.AbstractBooleanAssert;
import org.assertj.core.api.AssertProvider;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ObjectAssert;

import org.springframework.boot.SpringApplicationShutdownHookInstance.Assert;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Test access to the static {@link SpringApplicationShutdownHook} instance in
 * {@link SpringApplication}.
 *
 * @author Phillip Webb
 */
public final class SpringApplicationShutdownHookInstance implements AssertProvider<Assert> {

	private final SpringApplicationShutdownHook shutdownHook;

	private SpringApplicationShutdownHookInstance(SpringApplicationShutdownHook shutdownHook) {
		this.shutdownHook = shutdownHook;
	}

	SpringApplicationShutdownHook getShutdownHook() {
		return this.shutdownHook;
	}

	@Override
	public Assert assertThat() {
		return new Assert(this.shutdownHook);
	}

	public static void reset() {
		get().getShutdownHook().reset();
	}

	public static SpringApplicationShutdownHookInstance get() {
		return new SpringApplicationShutdownHookInstance(SpringApplication.shutdownHook);
	}

	/**
	 * Assertions that can be performed on the {@link SpringApplicationShutdownHook}.
	 */
	public static class Assert extends ObjectAssert<SpringApplicationShutdownHook> {

		Assert(SpringApplicationShutdownHook actual) {
			super(actual);
		}

		public Assert registeredApplicationContext(ConfigurableApplicationContext context) {
			assertThatIsApplicationContextRegistered(context).isTrue();
			return this;
		}

		public Assert didNotRegisterApplicationContext(ConfigurableApplicationContext context) {
			assertThatIsApplicationContextRegistered(context).isFalse();
			return this;
		}

		private AbstractBooleanAssert<?> assertThatIsApplicationContextRegistered(
				ConfigurableApplicationContext context) {
			return Assertions.assertThat(this.actual.isApplicationContextRegistered(context))
					.as("ApplicationContext registered with shutdown hook");
		}

	}

}
