/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.test.context.assertj;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.assertj.ApplicationContextAssert.Scope;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.support.StaticApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link ApplicationContextAssert}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class ApplicationContextAssertTests {

	private StaticApplicationContext parent;

	private StaticApplicationContext context;

	private RuntimeException failure = new RuntimeException();

	@BeforeEach
	public void setup() {
		this.parent = new StaticApplicationContext();
		this.context = new StaticApplicationContext();
		this.context.setParent(this.parent);
	}

	@AfterEach
	public void cleanup() {
		this.context.close();
		this.parent.close();
	}

	@Test
	void createWhenApplicationContextIsNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new ApplicationContextAssert<>(null, null))
				.withMessageContaining("ApplicationContext must not be null");
	}

	@Test
	void createWhenHasApplicationContextShouldSetActual() {
		assertThat(getAssert(this.context).getSourceApplicationContext()).isSameAs(this.context);
	}

	@Test
	void createWhenHasExceptionShouldSetFailure() {
		assertThat(getAssert(this.failure)).getFailure().isSameAs(this.failure);
	}

	@Test
	void hasBeanWhenHasBeanShouldPass() {
		this.context.registerSingleton("foo", Foo.class);
		assertThat(getAssert(this.context)).hasBean("foo");
	}

	@Test
	void hasBeanWhenHasNoBeanShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(getAssert(this.context)).hasBean("foo"))
				.withMessageContaining("no such bean");
	}

	@Test
	void hasBeanWhenNotStartedShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(getAssert(this.failure)).hasBean("foo"))
				.withMessageContaining(String.format("but context failed to start:%n java.lang.RuntimeException"));
	}

	@Test
	void hasSingleBeanWhenHasSingleBeanShouldPass() {
		this.context.registerSingleton("foo", Foo.class);
		assertThat(getAssert(this.context)).hasSingleBean(Foo.class);
	}

	@Test
	void hasSingleBeanWhenHasNoBeansShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(getAssert(this.context)).hasSingleBean(Foo.class))
				.withMessageContaining("to have a single bean of type");
	}

	@Test
	void hasSingleBeanWhenHasMultipleShouldFail() {
		this.context.registerSingleton("foo", Foo.class);
		this.context.registerSingleton("bar", Foo.class);
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(getAssert(this.context)).hasSingleBean(Foo.class))
				.withMessageContaining("but found:");
	}

	@Test
	void hasSingleBeanWhenFailedToStartShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(getAssert(this.failure)).hasSingleBean(Foo.class))
				.withMessageContaining("to have a single bean of type")
				.withMessageContaining(String.format("but context failed to start:%n java.lang.RuntimeException"));
	}

	@Test
	void hasSingleBeanWhenInParentShouldFail() {
		this.parent.registerSingleton("foo", Foo.class);
		this.context.registerSingleton("bar", Foo.class);
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(getAssert(this.context)).hasSingleBean(Foo.class))
				.withMessageContaining("but found:");
	}

	@Test
	void hasSingleBeanWithLimitedScopeWhenInParentShouldPass() {
		this.parent.registerSingleton("foo", Foo.class);
		this.context.registerSingleton("bar", Foo.class);
		assertThat(getAssert(this.context)).hasSingleBean(Foo.class, Scope.NO_ANCESTORS);
	}

	@Test
	void doesNotHaveBeanOfTypeWhenHasNoBeanOfTypeShouldPass() {
		assertThat(getAssert(this.context)).doesNotHaveBean(Foo.class);
	}

	@Test
	void doesNotHaveBeanOfTypeWhenHasBeanOfTypeShouldFail() {
		this.context.registerSingleton("foo", Foo.class);
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(getAssert(this.context)).doesNotHaveBean(Foo.class))
				.withMessageContaining("but found");
	}

	@Test
	void doesNotHaveBeanOfTypeWhenFailedToStartShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(getAssert(this.failure)).doesNotHaveBean(Foo.class))
				.withMessageContaining("not to have any beans of type")
				.withMessageContaining(String.format("but context failed to start:%n java.lang.RuntimeException"));
	}

	@Test
	void doesNotHaveBeanOfTypeWhenInParentShouldFail() {
		this.parent.registerSingleton("foo", Foo.class);
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(getAssert(this.context)).doesNotHaveBean(Foo.class))
				.withMessageContaining("but found");
	}

	@Test
	void doesNotHaveBeanOfTypeWithLimitedScopeWhenInParentShouldPass() {
		this.parent.registerSingleton("foo", Foo.class);
		assertThat(getAssert(this.context)).doesNotHaveBean(Foo.class, Scope.NO_ANCESTORS);
	}

	@Test
	void doesNotHaveBeanOfNameWhenHasNoBeanOfTypeShouldPass() {
		assertThat(getAssert(this.context)).doesNotHaveBean("foo");
	}

	@Test
	void doesNotHaveBeanOfNameWhenHasBeanOfTypeShouldFail() {
		this.context.registerSingleton("foo", Foo.class);
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(getAssert(this.context)).doesNotHaveBean("foo"))
				.withMessageContaining("but found");
	}

	@Test
	void doesNotHaveBeanOfNameWhenFailedToStartShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(getAssert(this.failure)).doesNotHaveBean("foo"))
				.withMessageContaining("not to have any beans of name").withMessageContaining("failed to start");
	}

	@Test
	void getBeanNamesWhenHasNamesShouldReturnNamesAssert() {
		this.context.registerSingleton("foo", Foo.class);
		this.context.registerSingleton("bar", Foo.class);
		assertThat(getAssert(this.context)).getBeanNames(Foo.class).containsOnly("foo", "bar");
	}

	@Test
	void getBeanNamesWhenHasNoNamesShouldReturnEmptyAssert() {
		assertThat(getAssert(this.context)).getBeanNames(Foo.class).isEmpty();
	}

	@Test
	void getBeanNamesWhenFailedToStartShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(getAssert(this.failure)).doesNotHaveBean("foo"))
				.withMessageContaining("not to have any beans of name")
				.withMessageContaining(String.format("but context failed to start:%n java.lang.RuntimeException"));
	}

	@Test
	void getBeanOfTypeWhenHasBeanShouldReturnBeanAssert() {
		this.context.registerSingleton("foo", Foo.class);
		assertThat(getAssert(this.context)).getBean(Foo.class).isNotNull();
	}

	@Test
	void getBeanOfTypeWhenHasNoBeanShouldReturnNullAssert() {
		assertThat(getAssert(this.context)).getBean(Foo.class).isNull();
	}

	@Test
	void getBeanOfTypeWhenHasMultipleBeansShouldFail() {
		this.context.registerSingleton("foo", Foo.class);
		this.context.registerSingleton("bar", Foo.class);
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(getAssert(this.context)).getBean(Foo.class))
				.withMessageContaining("but found");
	}

	@Test
	void getBeanOfTypeWhenHasPrimaryBeanShouldReturnPrimary() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(PrimaryFooConfig.class);
		assertThat(getAssert(context)).getBean(Foo.class).isInstanceOf(Bar.class);
		context.close();
	}

	@Test
	void getBeanOfTypeWhenFailedToStartShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(getAssert(this.failure)).getBean(Foo.class))
				.withMessageContaining("to contain bean of type")
				.withMessageContaining(String.format("but context failed to start:%n java.lang.RuntimeException"));
	}

	@Test
	void getBeanOfTypeWhenInParentShouldReturnBeanAssert() {
		this.parent.registerSingleton("foo", Foo.class);
		assertThat(getAssert(this.context)).getBean(Foo.class).isNotNull();
	}

	@Test
	void getBeanOfTypeWhenInParentWithLimitedScopeShouldReturnNullAssert() {
		this.parent.registerSingleton("foo", Foo.class);
		assertThat(getAssert(this.context)).getBean(Foo.class, Scope.NO_ANCESTORS).isNull();
	}

	@Test
	void getBeanOfTypeWhenHasMultipleBeansIncludingParentShouldFail() {
		this.parent.registerSingleton("foo", Foo.class);
		this.context.registerSingleton("bar", Foo.class);
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(getAssert(this.context)).getBean(Foo.class))
				.withMessageContaining("but found");
	}

	@Test
	void getBeanOfTypeWithLimitedScopeWhenHasMultipleBeansIncludingParentShouldReturnBeanAssert() {
		this.parent.registerSingleton("foo", Foo.class);
		this.context.registerSingleton("bar", Foo.class);
		assertThat(getAssert(this.context)).getBean(Foo.class, Scope.NO_ANCESTORS).isNotNull();
	}

	@Test
	void getBeanOfNameWhenHasBeanShouldReturnBeanAssert() {
		this.context.registerSingleton("foo", Foo.class);
		assertThat(getAssert(this.context)).getBean("foo").isNotNull();
	}

	@Test
	void getBeanOfNameWhenHasNoBeanOfNameShouldReturnNullAssert() {
		assertThat(getAssert(this.context)).getBean("foo").isNull();
	}

	@Test
	void getBeanOfNameWhenFailedToStartShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(getAssert(this.failure)).getBean("foo"))
				.withMessageContaining("to contain a bean of name")
				.withMessageContaining(String.format("but context failed to start:%n java.lang.RuntimeException"));
	}

	@Test
	void getBeanOfNameAndTypeWhenHasBeanShouldReturnBeanAssert() {
		this.context.registerSingleton("foo", Foo.class);
		assertThat(getAssert(this.context)).getBean("foo", Foo.class).isNotNull();
	}

	@Test
	void getBeanOfNameAndTypeWhenHasNoBeanOfNameShouldReturnNullAssert() {
		assertThat(getAssert(this.context)).getBean("foo", Foo.class).isNull();
	}

	@Test
	void getBeanOfNameAndTypeWhenHasNoBeanOfNameButDifferentTypeShouldFail() {
		this.context.registerSingleton("foo", Foo.class);
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(getAssert(this.context)).getBean("foo", String.class))
				.withMessageContaining("of type");
	}

	@Test
	void getBeanOfNameAndTypeWhenFailedToStartShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(getAssert(this.failure)).getBean("foo", Foo.class))
				.withMessageContaining("to contain a bean of name")
				.withMessageContaining(String.format("but context failed to start:%n java.lang.RuntimeException"));
	}

	@Test
	void getBeansWhenHasBeansShouldReturnMapAssert() {
		this.context.registerSingleton("foo", Foo.class);
		this.context.registerSingleton("bar", Foo.class);
		assertThat(getAssert(this.context)).getBeans(Foo.class).hasSize(2).containsKeys("foo", "bar");
	}

	@Test
	void getBeansWhenHasNoBeansShouldReturnEmptyMapAssert() {
		assertThat(getAssert(this.context)).getBeans(Foo.class).isEmpty();
	}

	@Test
	void getBeansWhenFailedToStartShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(getAssert(this.failure)).getBeans(Foo.class))
				.withMessageContaining("to get beans of type")
				.withMessageContaining(String.format("but context failed to start:%n java.lang.RuntimeException"));
	}

	@Test
	void getBeansShouldIncludeBeansFromParentScope() {
		this.parent.registerSingleton("foo", Foo.class);
		this.context.registerSingleton("bar", Foo.class);
		assertThat(getAssert(this.context)).getBeans(Foo.class).hasSize(2).containsKeys("foo", "bar");
	}

	@Test
	void getBeansWithLimitedScopeShouldNotIncludeBeansFromParentScope() {
		this.parent.registerSingleton("foo", Foo.class);
		this.context.registerSingleton("bar", Foo.class);
		assertThat(getAssert(this.context)).getBeans(Foo.class, Scope.NO_ANCESTORS).hasSize(1).containsKeys("bar");
	}

	@Test
	void getFailureWhenFailedShouldReturnFailure() {
		assertThat(getAssert(this.failure)).getFailure().isSameAs(this.failure);
	}

	@Test
	void getFailureWhenDidNotFailShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(getAssert(this.context)).getFailure())
				.withMessageContaining("context started");
	}

	@Test
	void hasFailedWhenFailedShouldPass() {
		assertThat(getAssert(this.failure)).hasFailed();
	}

	@Test
	void hasFailedWhenNotFailedShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(getAssert(this.context)).hasFailed())
				.withMessageContaining("to have failed");
	}

	@Test
	void hasNotFailedWhenFailedShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(getAssert(this.failure)).hasNotFailed())
				.withMessageContaining("to have not failed")
				.withMessageContaining(String.format("but context failed to start:%n java.lang.RuntimeException"));
	}

	@Test
	void hasNotFailedWhenNotFailedShouldPass() {
		assertThat(getAssert(this.context)).hasNotFailed();
	}

	private AssertableApplicationContext getAssert(ConfigurableApplicationContext applicationContext) {
		return AssertableApplicationContext.get(() -> applicationContext);
	}

	private AssertableApplicationContext getAssert(RuntimeException failure) {
		return AssertableApplicationContext.get(() -> {
			throw failure;
		});
	}

	private static class Foo {

	}

	private static class Bar extends Foo {

	}

	@Configuration(proxyBeanMethods = false)
	static class PrimaryFooConfig {

		@Bean
		public Foo foo() {
			return new Foo();
		}

		@Bean
		@Primary
		public Bar bar() {
			return new Bar();
		}

	}

}
