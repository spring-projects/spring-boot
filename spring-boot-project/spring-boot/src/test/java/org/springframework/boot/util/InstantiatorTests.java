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

package org.springframework.boot.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.boot.util.Instantiator.FailureHandler;
import org.springframework.core.Ordered;
import org.springframework.core.OverridingClassLoader;
import org.springframework.core.annotation.Order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link Instantiator}.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 */
class InstantiatorTests {

	private final ParamA paramA = new ParamA();

	private final ParamB paramB = new ParamB();

	private ParamC paramC;

	@Test
	void instantiateWhenOnlyDefaultConstructorCreatesInstance() {
		WithDefaultConstructor instance = createInstance(WithDefaultConstructor.class);
		assertThat(instance).isInstanceOf(WithDefaultConstructor.class);
	}

	@Test
	void instantiateWhenMultipleConstructorPicksMostArguments() {
		WithMultipleConstructors instance = createInstance(WithMultipleConstructors.class);
		assertThat(instance).isInstanceOf(WithMultipleConstructors.class);
	}

	@Test
	void instantiateWhenAdditionalConstructorPicksMostSuitable() {
		WithAdditionalConstructor instance = createInstance(WithAdditionalConstructor.class);
		assertThat(instance).isInstanceOf(WithAdditionalConstructor.class);

	}

	@Test
	void instantiateOrdersInstances() {
		List<Object> instances = createInstantiator(Object.class).instantiate(
				Arrays.asList(WithMultipleConstructors.class.getName(), WithAdditionalConstructor.class.getName()));
		assertThat(instances).hasSize(2);
		assertThat(instances.get(0)).isInstanceOf(WithAdditionalConstructor.class);
		assertThat(instances.get(1)).isInstanceOf(WithMultipleConstructors.class);
	}

	@Test
	void instantiateWithFactory() {
		assertThat(this.paramC).isNull();
		WithFactory instance = createInstance(WithFactory.class);
		assertThat(instance.getParamC()).isEqualTo(this.paramC);
	}

	@Test
	void instantiateTypesCreatesInstance() {
		WithDefaultConstructor instance = createInstantiator(WithDefaultConstructor.class)
				.instantiateTypes(Collections.singleton(WithDefaultConstructor.class)).get(0);
		assertThat(instance).isInstanceOf(WithDefaultConstructor.class);
	}

	@Test
	void instantiateWithClassLoaderCreatesInstance() {
		OverridingClassLoader classLoader = new OverridingClassLoader(getClass().getClassLoader()) {

			@Override
			protected boolean isEligibleForOverriding(String className) {
				return super.isEligibleForOverriding(className)
						&& className.equals(WithDefaultConstructorSubclass.class.getName());
			}

		};
		WithDefaultConstructor instance = createInstantiator(WithDefaultConstructor.class)
				.instantiate(classLoader, Collections.singleton(WithDefaultConstructorSubclass.class.getName())).get(0);
		assertThat(instance.getClass().getClassLoader()).isSameAs(classLoader);
	}

	@Test
	void createWhenWrongTypeThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> createInstantiator(WithDefaultConstructor.class)
						.instantiate(Collections.singleton(WithAdditionalConstructor.class.getName())))
				.withMessageContaining("Unable to instantiate");
	}

	@Test
	void createWithFailureHandlerInvokesFailureHandler() {
		assertThatIllegalStateException()
				.isThrownBy(() -> new Instantiator<>(WithDefaultConstructor.class, (availableParameters) -> {
				}, new CustomFailureHandler())
						.instantiate(Collections.singleton(WithAdditionalConstructor.class.getName())))
				.withMessageContaining("custom failure handler message");
	}

	private <T> T createInstance(Class<T> type) {
		return createInstantiator(type).instantiate(Collections.singleton(type.getName())).get(0);
	}

	private <T> Instantiator<T> createInstantiator(Class<T> type) {
		return new Instantiator<>(type, (availableParameters) -> {
			availableParameters.add(ParamA.class, this.paramA);
			availableParameters.add(ParamB.class, this.paramB);
			availableParameters.add(ParamC.class, ParamC::new);
		});
	}

	static class WithDefaultConstructorSubclass extends WithDefaultConstructor {

	}

	@Order(Ordered.LOWEST_PRECEDENCE)
	static class WithMultipleConstructors {

		WithMultipleConstructors() {
			throw new IllegalStateException();
		}

		WithMultipleConstructors(ParamA paramA) {
			throw new IllegalStateException();
		}

		WithMultipleConstructors(ParamA paramA, ParamB paramB) {
		}

	}

	@Order(Ordered.HIGHEST_PRECEDENCE)
	static class WithAdditionalConstructor {

		WithAdditionalConstructor(ParamA paramA, ParamB paramB) {
		}

		WithAdditionalConstructor(ParamA paramA, ParamB paramB, String extra) {
			throw new IllegalStateException();
		}

	}

	static class WithFactory {

		private ParamC paramC;

		WithFactory(ParamC paramC) {
			this.paramC = paramC;
		}

		ParamC getParamC() {
			return this.paramC;
		}

	}

	class ParamA {

	}

	class ParamB {

	}

	class ParamC {

		ParamC(Class<?> type) {
			InstantiatorTests.this.paramC = this;
		}

	}

	class CustomFailureHandler implements FailureHandler {

		@Override
		public void handleFailure(Class<?> type, String implementationName, Throwable failure) {
			throw new IllegalStateException("custom failure handler message");
		}

	}

}
