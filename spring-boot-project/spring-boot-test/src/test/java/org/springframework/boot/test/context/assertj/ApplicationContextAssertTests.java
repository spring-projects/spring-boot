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

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.boot.test.context.assertj.ApplicationContextAssert.Scope;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.StaticApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ApplicationContextAssert}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
public class ApplicationContextAssertTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private StaticApplicationContext parent;

	private StaticApplicationContext context;

	private RuntimeException failure = new RuntimeException();

	@Before
	public void setup() {
		this.parent = new StaticApplicationContext();
		this.context = new StaticApplicationContext();
		this.context.setParent(this.parent);
	}

	@After
	public void cleanup() {
		this.context.close();
		this.parent.close();
	}

	@Test
	public void createWhenApplicationContextIsNullShouldThrowException() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("ApplicationContext must not be null");
		new ApplicationContextAssert<>(null, null);
	}

	@Test
	public void createWhenHasApplicationContextShouldSetActual() {
		assertThat(getAssert(this.context).getSourceApplicationContext()).isSameAs(this.context);
	}

	@Test
	public void createWhenHasExceptionShouldSetFailure() {
		assertThat(getAssert(this.failure)).getFailure().isSameAs(this.failure);
	}

	@Test
	public void hasBeanWhenHasBeanShouldPass() {
		this.context.registerSingleton("foo", Foo.class);
		assertThat(getAssert(this.context)).hasBean("foo");
	}

	@Test
	public void hasBeanWhenHasNoBeanShouldFail() {
		this.thrown.expect(AssertionError.class);
		this.thrown.expectMessage("no such bean");
		assertThat(getAssert(this.context)).hasBean("foo");
	}

	@Test
	public void hasBeanWhenNotStartedShouldFail() {
		this.thrown.expect(AssertionError.class);
		this.thrown.expectMessage(String.format("but context failed to start:%n java.lang.RuntimeException"));
		assertThat(getAssert(this.failure)).hasBean("foo");
	}

	@Test
	public void hasSingleBeanWhenHasSingleBeanShouldPass() {
		this.context.registerSingleton("foo", Foo.class);
		assertThat(getAssert(this.context)).hasSingleBean(Foo.class);
	}

	@Test
	public void hasSingleBeanWhenHasNoBeansShouldFail() {
		this.thrown.expect(AssertionError.class);
		this.thrown.expectMessage("to have a single bean of type");
		assertThat(getAssert(this.context)).hasSingleBean(Foo.class);
	}

	@Test
	public void hasSingleBeanWhenHasMultipleShouldFail() {
		this.context.registerSingleton("foo", Foo.class);
		this.context.registerSingleton("bar", Foo.class);
		this.thrown.expect(AssertionError.class);
		this.thrown.expectMessage("but found:");
		assertThat(getAssert(this.context)).hasSingleBean(Foo.class);
	}

	@Test
	public void hasSingleBeanWhenFailedToStartShouldFail() {
		this.thrown.expect(AssertionError.class);
		this.thrown.expectMessage("to have a single bean of type");
		this.thrown.expectMessage(String.format("but context failed to start:%n java.lang.RuntimeException"));
		assertThat(getAssert(this.failure)).hasSingleBean(Foo.class);
	}

	@Test
	public void hasSingleBeanWhenInParentShouldFail() {
		this.parent.registerSingleton("foo", Foo.class);
		this.context.registerSingleton("bar", Foo.class);
		this.thrown.expect(AssertionError.class);
		this.thrown.expectMessage("but found:");
		assertThat(getAssert(this.context)).hasSingleBean(Foo.class);
	}

	@Test
	public void hasSingleBeanWithLimitedScopeWhenInParentShouldPass() {
		this.parent.registerSingleton("foo", Foo.class);
		this.context.registerSingleton("bar", Foo.class);
		assertThat(getAssert(this.context)).hasSingleBean(Foo.class, Scope.NO_ANCESTORS);
	}

	@Test
	public void doesNotHaveBeanOfTypeWhenHasNoBeanOfTypeShouldPass() {
		assertThat(getAssert(this.context)).doesNotHaveBean(Foo.class);
	}

	@Test
	public void doesNotHaveBeanOfTypeWhenHasBeanOfTypeShouldFail() {
		this.thrown.expect(AssertionError.class);
		this.thrown.expectMessage("but found");
		this.context.registerSingleton("foo", Foo.class);
		assertThat(getAssert(this.context)).doesNotHaveBean(Foo.class);
	}

	@Test
	public void doesNotHaveBeanOfTypeWhenFailedToStartShouldFail() {
		this.thrown.expect(AssertionError.class);
		this.thrown.expectMessage("not to have any beans of type");
		this.thrown.expectMessage(String.format("but context failed to start:%n java.lang.RuntimeException"));
		assertThat(getAssert(this.failure)).doesNotHaveBean(Foo.class);
	}

	@Test
	public void doesNotHaveBeanOfTypeWhenInParentShouldFail() {
		this.thrown.expect(AssertionError.class);
		this.thrown.expectMessage("but found");
		this.parent.registerSingleton("foo", Foo.class);
		assertThat(getAssert(this.context)).doesNotHaveBean(Foo.class);
	}

	@Test
	public void doesNotHaveBeanOfTypeWithLimitedScopeWhenInParentShouldPass() {
		this.parent.registerSingleton("foo", Foo.class);
		assertThat(getAssert(this.context)).doesNotHaveBean(Foo.class, Scope.NO_ANCESTORS);
	}

	@Test
	public void doesNotHaveBeanOfNameWhenHasNoBeanOfTypeShouldPass() {
		assertThat(getAssert(this.context)).doesNotHaveBean("foo");
	}

	@Test
	public void doesNotHaveBeanOfNameWhenHasBeanOfTypeShouldFail() {
		this.thrown.expect(AssertionError.class);
		this.thrown.expectMessage("but found");
		this.context.registerSingleton("foo", Foo.class);
		assertThat(getAssert(this.context)).doesNotHaveBean("foo");
	}

	@Test
	public void doesNotHaveBeanOfNameWhenFailedToStartShouldFail() {
		this.thrown.expect(AssertionError.class);
		this.thrown.expectMessage("not to have any beans of name");
		this.thrown.expectMessage("failed to start");
		assertThat(getAssert(this.failure)).doesNotHaveBean("foo");
	}

	@Test
	public void getBeanNamesWhenHasNamesShouldReturnNamesAssert() {
		this.context.registerSingleton("foo", Foo.class);
		this.context.registerSingleton("bar", Foo.class);
		assertThat(getAssert(this.context)).getBeanNames(Foo.class).containsOnly("foo", "bar");
	}

	@Test
	public void getBeanNamesWhenHasNoNamesShouldReturnEmptyAssert() {
		assertThat(getAssert(this.context)).getBeanNames(Foo.class).isEmpty();
	}

	@Test
	public void getBeanNamesWhenFailedToStartShouldFail() {
		this.thrown.expect(AssertionError.class);
		this.thrown.expectMessage("not to have any beans of name");
		this.thrown.expectMessage(String.format("but context failed to start:%n java.lang.RuntimeException"));
		assertThat(getAssert(this.failure)).doesNotHaveBean("foo");
	}

	@Test
	public void getBeanOfTypeWhenHasBeanShouldReturnBeanAssert() {
		this.context.registerSingleton("foo", Foo.class);
		assertThat(getAssert(this.context)).getBean(Foo.class).isNotNull();
	}

	@Test
	public void getBeanOfTypeWhenHasNoBeanShouldReturnNullAssert() {
		assertThat(getAssert(this.context)).getBean(Foo.class).isNull();
	}

	@Test
	public void getBeanOfTypeWhenHasMultipleBeansShouldFail() {
		this.context.registerSingleton("foo", Foo.class);
		this.context.registerSingleton("bar", Foo.class);
		this.thrown.expect(AssertionError.class);
		this.thrown.expectMessage("but found");
		assertThat(getAssert(this.context)).getBean(Foo.class);
	}

	@Test
	public void getBeanOfTypeWhenFailedToStartShouldFail() {
		this.thrown.expect(AssertionError.class);
		this.thrown.expectMessage("to contain bean of type");
		this.thrown.expectMessage(String.format("but context failed to start:%n java.lang.RuntimeException"));
		assertThat(getAssert(this.failure)).getBean(Foo.class);
	}

	@Test
	public void getBeanOfTypeWhenInParentShouldReturnBeanAssert() {
		this.parent.registerSingleton("foo", Foo.class);
		assertThat(getAssert(this.context)).getBean(Foo.class).isNotNull();
	}

	@Test
	public void getBeanOfTypeWhenInParentWithLimitedScopeShouldReturnNullAssert() {
		this.parent.registerSingleton("foo", Foo.class);
		assertThat(getAssert(this.context)).getBean(Foo.class, Scope.NO_ANCESTORS).isNull();
	}

	@Test
	public void getBeanOfTypeWhenHasMultipleBeansIncludingParentShouldFail() {
		this.parent.registerSingleton("foo", Foo.class);
		this.context.registerSingleton("bar", Foo.class);
		this.thrown.expect(AssertionError.class);
		this.thrown.expectMessage("but found");
		assertThat(getAssert(this.context)).getBean(Foo.class);
	}

	@Test
	public void getBeanOfTypeWithLimitedScopeWhenHasMultipleBeansIncludingParentShouldReturnBeanAssert() {
		this.parent.registerSingleton("foo", Foo.class);
		this.context.registerSingleton("bar", Foo.class);
		assertThat(getAssert(this.context)).getBean(Foo.class, Scope.NO_ANCESTORS).isNotNull();
	}

	@Test
	public void getBeanOfNameWhenHasBeanShouldReturnBeanAssert() {
		this.context.registerSingleton("foo", Foo.class);
		assertThat(getAssert(this.context)).getBean("foo").isNotNull();
	}

	@Test
	public void getBeanOfNameWhenHasNoBeanOfNameShouldReturnNullAssert() {
		assertThat(getAssert(this.context)).getBean("foo").isNull();
	}

	@Test
	public void getBeanOfNameWhenFailedToStartShouldFail() {
		this.thrown.expect(AssertionError.class);
		this.thrown.expectMessage("to contain a bean of name");
		this.thrown.expectMessage(String.format("but context failed to start:%n java.lang.RuntimeException"));
		assertThat(getAssert(this.failure)).getBean("foo");
	}

	@Test
	public void getBeanOfNameAndTypeWhenHasBeanShouldReturnBeanAssert() {
		this.context.registerSingleton("foo", Foo.class);
		assertThat(getAssert(this.context)).getBean("foo", Foo.class).isNotNull();
	}

	@Test
	public void getBeanOfNameAndTypeWhenHasNoBeanOfNameShouldReturnNullAssert() {
		assertThat(getAssert(this.context)).getBean("foo", Foo.class).isNull();
	}

	@Test
	public void getBeanOfNameAndTypeWhenHasNoBeanOfNameButDifferentTypeShouldFail() {
		this.context.registerSingleton("foo", Foo.class);
		this.thrown.expect(AssertionError.class);
		this.thrown.expectMessage("of type");
		assertThat(getAssert(this.context)).getBean("foo", String.class);
	}

	@Test
	public void getBeanOfNameAndTypeWhenFailedToStartShouldFail() {
		this.thrown.expect(AssertionError.class);
		this.thrown.expectMessage("to contain a bean of name");
		this.thrown.expectMessage(String.format("but context failed to start:%n java.lang.RuntimeException"));
		assertThat(getAssert(this.failure)).getBean("foo", Foo.class);
	}

	@Test
	public void getBeansWhenHasBeansShouldReturnMapAssert() {
		this.context.registerSingleton("foo", Foo.class);
		this.context.registerSingleton("bar", Foo.class);
		assertThat(getAssert(this.context)).getBeans(Foo.class).hasSize(2).containsKeys("foo", "bar");
	}

	@Test
	public void getBeansWhenHasNoBeansShouldReturnEmptyMapAssert() {
		assertThat(getAssert(this.context)).getBeans(Foo.class).isEmpty();
	}

	@Test
	public void getBeansWhenFailedToStartShouldFail() {
		this.thrown.expect(AssertionError.class);
		this.thrown.expectMessage("to get beans of type");
		this.thrown.expectMessage(String.format("but context failed to start:%n java.lang.RuntimeException"));
		assertThat(getAssert(this.failure)).getBeans(Foo.class);
	}

	@Test
	public void getBeansShouldIncludeBeansFromParentScope() {
		this.parent.registerSingleton("foo", Foo.class);
		this.context.registerSingleton("bar", Foo.class);
		assertThat(getAssert(this.context)).getBeans(Foo.class).hasSize(2).containsKeys("foo", "bar");
	}

	@Test
	public void getBeansWithLimitedScopeShouldNotIncludeBeansFromParentScope() {
		this.parent.registerSingleton("foo", Foo.class);
		this.context.registerSingleton("bar", Foo.class);
		assertThat(getAssert(this.context)).getBeans(Foo.class, Scope.NO_ANCESTORS).hasSize(1).containsKeys("bar");
	}

	@Test
	public void getFailureWhenFailedShouldReturnFailure() {
		assertThat(getAssert(this.failure)).getFailure().isSameAs(this.failure);
	}

	@Test
	public void getFailureWhenDidNotFailShouldFail() {
		this.thrown.expect(AssertionError.class);
		this.thrown.expectMessage("context started");
		assertThat(getAssert(this.context)).getFailure();
	}

	@Test
	public void hasFailedWhenFailedShouldPass() {
		assertThat(getAssert(this.failure)).hasFailed();
	}

	@Test
	public void hasFailedWhenNotFailedShouldFail() {
		this.thrown.expect(AssertionError.class);
		this.thrown.expectMessage("to have failed");
		assertThat(getAssert(this.context)).hasFailed();
	}

	@Test
	public void hasNotFailedWhenFailedShouldFail() {
		this.thrown.expect(AssertionError.class);
		this.thrown.expectMessage("to have not failed");
		this.thrown.expectMessage(String.format("but context failed to start:%n java.lang.RuntimeException"));
		assertThat(getAssert(this.failure)).hasNotFailed();
	}

	@Test
	public void hasNotFailedWhenNotFailedShouldPass() {
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

}
