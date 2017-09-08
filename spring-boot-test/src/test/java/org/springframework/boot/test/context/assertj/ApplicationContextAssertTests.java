/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.test.context.assertj;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.StaticApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ApplicationContextAssert}.
 *
 * @author Phillip Webb
 */
public class ApplicationContextAssertTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private StaticApplicationContext context = new StaticApplicationContext();

	private RuntimeException failure = new RuntimeException();

	@Test
	public void createWhenApplicationContextIsNullShouldThrowException()
			throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("ApplicationContext must not be null");
		new ApplicationContextAssert<>(null, null);
	}

	@Test
	public void createWhenHasApplicationContextShouldSetActual() throws Exception {
		assertThat(getAssert(this.context).getSourceApplicationContext())
				.isSameAs(this.context);
	}

	@Test
	public void createWhenHasExceptionShouldSetFailure() throws Exception {
		assertThat(getAssert(this.failure)).getFailure().isSameAs(this.failure);
	}

	@Test
	public void hasBeanWhenHasBeanShouldPass() throws Exception {
		this.context.registerSingleton("foo", Foo.class);
		assertThat(getAssert(this.context)).hasBean("foo");
	}

	@Test
	public void hasBeanWhenHasNoBeanShouldFail() throws Exception {
		this.thrown.expect(AssertionError.class);
		this.thrown.expectMessage("no such bean");
		assertThat(getAssert(this.context)).hasBean("foo");
	}

	@Test
	public void hasBeanWhenNotStartedShouldFail() throws Exception {
		this.thrown.expect(AssertionError.class);
		this.thrown.expectMessage("failed to start");
		assertThat(getAssert(this.failure)).hasBean("foo");
	}

	@Test
	public void hasSingleBeanWhenHasSingleBeanShouldPass() throws Exception {
		this.context.registerSingleton("foo", Foo.class);
		assertThat(getAssert(this.context)).hasSingleBean(Foo.class);
	}

	@Test
	public void hasSingleBeanWhenHasNoBeansShouldFail() throws Exception {
		this.thrown.expect(AssertionError.class);
		this.thrown.expectMessage("no beans of that type");
		assertThat(getAssert(this.context)).hasSingleBean(Foo.class);
	}

	@Test
	public void hasSingleBeanWhenHasMultipleShouldFail() throws Exception {
		this.context.registerSingleton("foo", Foo.class);
		this.context.registerSingleton("bar", Foo.class);
		this.thrown.expect(AssertionError.class);
		this.thrown.expectMessage("but found:");
		assertThat(getAssert(this.context)).hasSingleBean(Foo.class);
	}

	@Test
	public void hasSingleBeanWhenFailedToStartShouldFail() throws Exception {
		this.thrown.expect(AssertionError.class);
		this.thrown.expectMessage("failed to start");
		assertThat(getAssert(this.failure)).hasSingleBean(Foo.class);
	}

	@Test
	public void doesNotHaveBeanOfTypeWhenHasNoBeanOfTypeShouldPass() throws Exception {
		assertThat(getAssert(this.context)).doesNotHaveBean(Foo.class);
	}

	@Test
	public void doesNotHaveBeanOfTypeWhenHasBeanOfTypeShouldFail() throws Exception {
		this.thrown.expect(AssertionError.class);
		this.thrown.expectMessage("but found");
		this.context.registerSingleton("foo", Foo.class);
		assertThat(getAssert(this.context)).doesNotHaveBean(Foo.class);
	}

	@Test
	public void doesNotHaveBeanOfTypeWhenFailedToStartShouldFail() throws Exception {
		this.thrown.expect(AssertionError.class);
		this.thrown.expectMessage("failed to start");
		assertThat(getAssert(this.failure)).doesNotHaveBean(Foo.class);
	}

	@Test
	public void doesNotHaveBeanOfNameWhenHasNoBeanOfTypeShouldPass() throws Exception {
		assertThat(getAssert(this.context)).doesNotHaveBean("foo");
	}

	@Test
	public void doesNotHaveBeanOfNameWhenHasBeanOfTypeShouldFail() throws Exception {
		this.thrown.expect(AssertionError.class);
		this.thrown.expectMessage("but found");
		this.context.registerSingleton("foo", Foo.class);
		assertThat(getAssert(this.context)).doesNotHaveBean("foo");
	}

	@Test
	public void doesNotHaveBeanOfNameWhenFailedToStartShouldFail() throws Exception {
		this.thrown.expect(AssertionError.class);
		this.thrown.expectMessage("failed to start");
		assertThat(getAssert(this.failure)).doesNotHaveBean("foo");
	}

	@Test
	public void getBeanNamesWhenHasNamesShouldReturnNamesAssert() throws Exception {
		this.context.registerSingleton("foo", Foo.class);
		this.context.registerSingleton("bar", Foo.class);
		assertThat(getAssert(this.context)).getBeanNames(Foo.class).containsOnly("foo",
				"bar");
	}

	@Test
	public void getBeanNamesWhenHasNoNamesShouldReturnEmptyAssert() throws Exception {
		assertThat(getAssert(this.context)).getBeanNames(Foo.class).isEmpty();
	}

	@Test
	public void getBeanNamesWhenFailedToStartShouldFail() throws Exception {
		this.thrown.expect(AssertionError.class);
		this.thrown.expectMessage("failed to start");
		assertThat(getAssert(this.failure)).doesNotHaveBean("foo");
	}

	@Test
	public void getBeanOfTypeWhenHasBeanShouldReturnBeanAssert() throws Exception {
		this.context.registerSingleton("foo", Foo.class);
		assertThat(getAssert(this.context)).getBean(Foo.class).isNotNull();
	}

	@Test
	public void getBeanOfTypeWhenHasNoBeanShouldReturnNullAssert() throws Exception {
		assertThat(getAssert(this.context)).getBean(Foo.class).isNull();
	}

	@Test
	public void getBeanOfTypeWhenHasMultipleBeansShouldFail() throws Exception {
		this.context.registerSingleton("foo", Foo.class);
		this.context.registerSingleton("bar", Foo.class);
		this.thrown.expect(AssertionError.class);
		this.thrown.expectMessage("but found");
		assertThat(getAssert(this.context)).getBean(Foo.class);
	}

	@Test
	public void getBeanOfTypeWhenFailedToStartShouldFail() throws Exception {
		this.thrown.expect(AssertionError.class);
		this.thrown.expectMessage("failed to start");
		assertThat(getAssert(this.failure)).getBean(Foo.class);
	}

	@Test
	public void getBeanOfNameWhenHasBeanShouldReturnBeanAssert() throws Exception {
		this.context.registerSingleton("foo", Foo.class);
		assertThat(getAssert(this.context)).getBean("foo").isNotNull();
	}

	@Test
	public void getBeanOfNameWhenHasNoBeanOfNameShouldReturnNullAssert()
			throws Exception {
		assertThat(getAssert(this.context)).getBean("foo").isNull();
	}

	@Test
	public void getBeanOfNameWhenFailedToStartShouldFail() throws Exception {
		this.thrown.expect(AssertionError.class);
		this.thrown.expectMessage("failed to start");
		assertThat(getAssert(this.failure)).getBean("foo");
	}

	@Test
	public void getBeanOfNameAndTypeWhenHasBeanShouldReturnBeanAssert() throws Exception {
		this.context.registerSingleton("foo", Foo.class);
		assertThat(getAssert(this.context)).getBean("foo", Foo.class).isNotNull();
	}

	@Test
	public void getBeanOfNameAndTypeWhenHasNoBeanOfNameShouldReturnNullAssert()
			throws Exception {
		assertThat(getAssert(this.context)).getBean("foo", Foo.class).isNull();
	}

	@Test
	public void getBeanOfNameAndTypeWhenHasNoBeanOfNameButDifferentTypeShouldFail()
			throws Exception {
		this.context.registerSingleton("foo", Foo.class);
		this.thrown.expect(AssertionError.class);
		this.thrown.expectMessage("of type");
		assertThat(getAssert(this.context)).getBean("foo", String.class);
	}

	@Test
	public void getBeanOfNameAndTypeWhenFailedToStartShouldFail() throws Exception {
		this.thrown.expect(AssertionError.class);
		this.thrown.expectMessage("failed to start");
		assertThat(getAssert(this.failure)).getBean("foo", Foo.class);
	}

	@Test
	public void getBeansWhenHasBeansShouldReturnMapAssert() throws Exception {
		this.context.registerSingleton("foo", Foo.class);
		this.context.registerSingleton("bar", Foo.class);
		assertThat(getAssert(this.context)).getBeans(Foo.class).hasSize(2)
				.containsKeys("foo", "bar");
	}

	@Test
	public void getBeansWhenHasNoBeansShouldReturnEmptyMapAssert() throws Exception {
		assertThat(getAssert(this.context)).getBeans(Foo.class).isEmpty();
	}

	@Test
	public void getBeansWhenFailedToStartShouldFail() throws Exception {
		this.thrown.expect(AssertionError.class);
		this.thrown.expectMessage("failed to start");
		assertThat(getAssert(this.failure)).getBeans(Foo.class);
	}

	@Test
	public void getFailureWhenFailedShouldReturnFailure() throws Exception {
		assertThat(getAssert(this.failure)).getFailure().isSameAs(this.failure);
	}

	@Test
	public void getFailureWhenDidNotFailShouldFail() throws Exception {
		this.thrown.expect(AssertionError.class);
		this.thrown.expectMessage("context started");
		assertThat(getAssert(this.context)).getFailure();
	}

	@Test
	public void hasFailedWhenFailedShouldPass() throws Exception {
		assertThat(getAssert(this.failure)).hasFailed();
	}

	@Test
	public void hasFailedWhenNotFailedShouldFail() throws Exception {
		this.thrown.expect(AssertionError.class);
		this.thrown.expectMessage("to have failed");
		assertThat(getAssert(this.context)).hasFailed();
	}

	@Test
	public void hasNotFailedWhenFailedShouldFail() throws Exception {
		this.thrown.expect(AssertionError.class);
		this.thrown.expectMessage("to have not failed");
		assertThat(getAssert(this.failure)).hasNotFailed();
	}

	@Test
	public void hasNotFailedWhenNotFailedShouldPass() throws Exception {
		assertThat(getAssert(this.context)).hasNotFailed();
	}

	private AssertableApplicationContext getAssert(
			ConfigurableApplicationContext applicationContext) {
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
