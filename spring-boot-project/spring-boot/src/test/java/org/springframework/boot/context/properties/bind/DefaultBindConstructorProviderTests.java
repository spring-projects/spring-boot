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

package org.springframework.boot.context.properties.bind;

import java.lang.reflect.Constructor;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link DefaultBindConstructorProvider}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class DefaultBindConstructorProviderTests {

	private final DefaultBindConstructorProvider provider = new DefaultBindConstructorProvider();

	@Test
	void getBindConstructorWhenHasOnlyDefaultConstructorReturnsNull() {
		Constructor<?> constructor = this.provider.getBindConstructor(OnlyDefaultConstructor.class, false);
		assertThat(constructor).isNull();
	}

	@Test
	void getBindConstructorWhenHasMultipleAmbiguousConstructorsReturnsNull() {
		Constructor<?> constructor = this.provider.getBindConstructor(MultipleAmbiguousConstructors.class, false);
		assertThat(constructor).isNull();
	}

	@Test
	void getBindConstructorWhenHasTwoConstructorsWithOneConstructorBindingReturnsConstructor() {
		Constructor<?> constructor = this.provider.getBindConstructor(TwoConstructorsWithOneConstructorBinding.class,
				false);
		assertThat(constructor).isNotNull();
		assertThat(constructor.getParameterCount()).isEqualTo(1);
	}

	@Test
	void getBindConstructorWhenHasOneConstructorWithAutowiredReturnsNull() {
		Constructor<?> constructor = this.provider.getBindConstructor(OneConstructorWithAutowired.class, false);
		assertThat(constructor).isNull();
	}

	@Test
	void getBindConstructorWhenHasTwoConstructorsWithOneAutowiredReturnsNull() {
		Constructor<?> constructor = this.provider.getBindConstructor(TwoConstructorsWithOneAutowired.class, false);
		assertThat(constructor).isNull();
	}

	@Test
	void getBindConstructorWhenHasTwoConstructorsWithOneAutowiredAndOneConstructorBindingThrowsException() {
		assertThatIllegalStateException()
				.isThrownBy(() -> this.provider
						.getBindConstructor(TwoConstructorsWithOneAutowiredAndOneConstructorBinding.class, false))
				.withMessageContaining("declares @ConstructorBinding and @Autowired");
	}

	@Test
	void getBindConstructorWhenHasOneConstructorWithConstructorBindingReturnsConstructor() {
		Constructor<?> constructor = this.provider.getBindConstructor(OneConstructorWithConstructorBinding.class,
				false);
		assertThat(constructor).isNotNull();
	}

	@Test
	void getBindConstructorWhenHasTwoConstructorsWithBothConstructorBindingThrowsException() {
		assertThatIllegalStateException()
				.isThrownBy(
						() -> this.provider.getBindConstructor(TwoConstructorsWithBothConstructorBinding.class, false))
				.withMessageContaining("has more than one @ConstructorBinding");
	}

	@Test
	void getBindConstructorWhenIsMemberTypeWithPrivateConstructorReturnsNull() {
		Constructor<?> constructor = this.provider.getBindConstructor(MemberTypeWithPrivateConstructor.Member.class,
				false);
		assertThat(constructor).isNull();
	}

	@Test
	void getBindConstructorFromProxiedClassWithOneAutowiredConstructorReturnsNull() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				ProxiedWithOneConstructorWithAutowired.class)) {
			ProxiedWithOneConstructorWithAutowired bean = context.getBean(ProxiedWithOneConstructorWithAutowired.class);
			Constructor<?> bindConstructor = this.provider.getBindConstructor(bean.getClass(), false);
			assertThat(bindConstructor).isNull();
		}
	}

	static class OnlyDefaultConstructor {

	}

	static class MultipleAmbiguousConstructors {

		MultipleAmbiguousConstructors() {
		}

		MultipleAmbiguousConstructors(String name) {
		}

	}

	static class TwoConstructorsWithOneConstructorBinding {

		@ConstructorBinding
		TwoConstructorsWithOneConstructorBinding(String name) {
			this(name, 100);
		}

		TwoConstructorsWithOneConstructorBinding(String name, int age) {
		}

	}

	static class OneConstructorWithAutowired {

		@Autowired
		OneConstructorWithAutowired(String name, int age) {
		}

	}

	static class TwoConstructorsWithOneAutowired {

		@Autowired
		TwoConstructorsWithOneAutowired(String name) {
			this(name, 100);
		}

		TwoConstructorsWithOneAutowired(String name, int age) {
		}

	}

	static class TwoConstructorsWithOneAutowiredAndOneConstructorBinding {

		@Autowired
		TwoConstructorsWithOneAutowiredAndOneConstructorBinding(String name) {
			this(name, 100);
		}

		@ConstructorBinding
		TwoConstructorsWithOneAutowiredAndOneConstructorBinding(String name, int age) {
		}

	}

	static class OneConstructorWithConstructorBinding {

		@ConstructorBinding
		OneConstructorWithConstructorBinding(String name, int age) {
		}

	}

	static class TwoConstructorsWithBothConstructorBinding {

		@ConstructorBinding
		TwoConstructorsWithBothConstructorBinding(String name) {
			this(name, 100);
		}

		@ConstructorBinding
		TwoConstructorsWithBothConstructorBinding(String name, int age) {
		}

	}

	static class MemberTypeWithPrivateConstructor {

		static final class Member {

			private Member(String name) {
			}

		}

	}

	@Configuration
	static class ProxiedWithOneConstructorWithAutowired {

		@Autowired
		ProxiedWithOneConstructorWithAutowired(Environment environment) {
		}

	}

}
