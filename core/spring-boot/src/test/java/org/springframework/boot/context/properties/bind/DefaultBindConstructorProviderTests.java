/*
 * Copyright 2012-present the original author or authors.
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
 * @author Yanming Zhou
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
		assertThat(constructor.getParameterCount()).isOne();
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
			.isThrownBy(() -> this.provider.getBindConstructor(TwoConstructorsWithBothConstructorBinding.class, false))
			.withMessageContaining("has more than one @ConstructorBinding");
	}

	@Test
	void getBindConstructorWhenIsTypeWithPrivateConstructorReturnsNull() {
		Constructor<?> constructor = this.provider.getBindConstructor(TypeWithPrivateConstructor.class, false);
		assertThat(constructor).isNull();
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

	@Test
	void getBindConstructorWhenHasExistingValueAndOneConstructorWithoutAnnotationsReturnsNull() {
		OneConstructorWithoutAnnotations existingValue = new OneConstructorWithoutAnnotations("name", 123);
		Bindable<?> bindable = Bindable.of(OneConstructorWithoutAnnotations.class).withExistingValue(existingValue);
		Constructor<?> bindConstructor = this.provider.getBindConstructor(bindable, false);
		assertThat(bindConstructor).isNull();
	}

	@Test
	void getBindConstructorWhenHasExistingValueAndOneConstructorWithConstructorBindingReturnsConstructor() {
		OneConstructorWithConstructorBinding existingValue = new OneConstructorWithConstructorBinding("name", 123);
		Bindable<?> bindable = Bindable.of(OneConstructorWithConstructorBinding.class).withExistingValue(existingValue);
		Constructor<?> bindConstructor = this.provider.getBindConstructor(bindable, false);
		assertThat(bindConstructor).isNotNull();
	}

	@Test
	void getBindConstructorWhenHasExistingValueAndValueIsRecordReturnsConstructor() {
		OneConstructorOnRecord existingValue = new OneConstructorOnRecord("name", 123);
		Bindable<?> bindable = Bindable.of(OneConstructorOnRecord.class).withExistingValue(existingValue);
		Constructor<?> bindConstructor = this.provider.getBindConstructor(bindable, false);
		assertThat(bindConstructor).isNotNull();
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

	static class OneConstructorWithoutAnnotations {

		OneConstructorWithoutAnnotations(String name, int age) {
		}

	}

	record OneConstructorOnRecord(String name, int age) {

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

	static final class TypeWithPrivateConstructor {

		private TypeWithPrivateConstructor(Environment environment) {
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
