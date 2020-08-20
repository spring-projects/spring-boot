/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.env;

import java.util.concurrent.atomic.AtomicInteger;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.AssertProvider;
import org.junit.jupiter.api.Test;

import org.springframework.boot.env.BootstrapRegistry.Registration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.StaticApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link DefaultBootstrapRegisty}.
 *
 * @author Phillip Webb
 */
class DefaultBootstrapRegistyTests {

	private DefaultBootstrapRegisty registy = new DefaultBootstrapRegisty();

	private AtomicInteger counter = new AtomicInteger();

	private StaticApplicationContext context = new StaticApplicationContext();

	@Test
	void getWhenNotRegisteredCreateInstance() {
		Integer result = this.registy.get(Integer.class, this.counter::getAndIncrement);
		assertThat(result).isEqualTo(0);
	}

	@Test
	void getWhenAlreadyRegisteredReturnsExisting() {
		this.registy.get(Integer.class, this.counter::getAndIncrement);
		Integer result = this.registy.get(Integer.class, this.counter::getAndIncrement);
		assertThat(result).isEqualTo(0);
	}

	@Test
	void getWithPreparedActionRegistersAction() {
		TestApplicationPreparedAction action = new TestApplicationPreparedAction();
		Integer result = this.registy.get(Integer.class, this.counter::getAndIncrement, action::run);
		this.registy.applicationContextPrepared(this.context);
		assertThat(result).isEqualTo(0);
		assertThat(action).wasCalledOnlyOnce().hasInstanceValue(0);
	}

	@Test
	void getWithPreparedActionWhenAlreadyRegisteredIgnoresRegistersAction() {
		TestApplicationPreparedAction action1 = new TestApplicationPreparedAction();
		TestApplicationPreparedAction action2 = new TestApplicationPreparedAction();
		this.registy.get(Integer.class, this.counter::getAndIncrement, action1::run);
		this.registy.get(Integer.class, this.counter::getAndIncrement, action2::run);
		this.registy.applicationContextPrepared(this.context);
		assertThat(action1).wasCalledOnlyOnce().hasInstanceValue(0);
		assertThat(action2).wasNotCalled();
	}

	@Test
	void registerAddsRegistration() {
		Registration<Integer> registration = this.registy.register(Integer.class, this.counter::getAndIncrement);
		assertThat(registration.get()).isEqualTo(0);
	}

	@Test
	void registerWhenAlreadyRegisteredReplacesPreviousRegistration() {
		Registration<Integer> registration1 = this.registy.register(Integer.class, this.counter::getAndIncrement);
		Registration<Integer> registration2 = this.registy.register(Integer.class, () -> -1);
		assertThat(registration2).isNotEqualTo(registration1);
		assertThat(registration1.get()).isEqualTo(0);
		assertThat(registration2.get()).isEqualTo(-1);
		assertThat(this.registy.get(Integer.class, this.counter::getAndIncrement)).isEqualTo(-1);
	}

	@Test
	void isRegisteredWhenNotRegisteredReturnsFalse() {
		assertThat(this.registy.isRegistered(Integer.class)).isFalse();
	}

	@Test
	void isRegisteredWhenRegisteredReturnsTrue() {
		this.registy.register(Integer.class, this.counter::getAndIncrement);
		assertThat(this.registy.isRegistered(Integer.class)).isTrue();
	}

	@Test
	void getRegistrationWhenNotRegisteredReturnsNull() {
		assertThat(this.registy.getRegistration(Integer.class)).isNull();
	}

	@Test
	void getRegistrationWhenRegisteredReturnsRegistration() {
		Registration<Integer> registration = this.registy.register(Integer.class, this.counter::getAndIncrement);
		assertThat(this.registy.getRegistration(Integer.class)).isSameAs(registration);
	}

	@Test
	void applicationContextPreparedTriggersActions() {
		TestApplicationPreparedAction action1 = new TestApplicationPreparedAction();
		TestApplicationPreparedAction action2 = new TestApplicationPreparedAction();
		Registration<Integer> registration = this.registy.register(Integer.class, this.counter::getAndIncrement);
		registration.onApplicationContextPrepared(action1::run);
		registration.onApplicationContextPrepared(action2::run);
		this.registy.applicationContextPrepared(this.context);
		assertThat(action1).wasCalledOnlyOnce().hasInstanceValue(0);
		assertThat(action2).wasCalledOnlyOnce().hasInstanceValue(0);
	}

	@Test
	void registrationGetReturnsInstance() {
		Registration<Integer> registration = this.registy.register(Integer.class, this.counter::getAndIncrement);
		assertThat(registration.get()).isEqualTo(0);
	}

	@Test
	void registrationGetWhenCalledMultipleTimesReturnsSingleInstance() {
		Registration<Integer> registration = this.registy.register(Integer.class, this.counter::getAndIncrement);
		assertThat(registration.get()).isEqualTo(0);
		assertThat(registration.get()).isEqualTo(0);
		assertThat(registration.get()).isEqualTo(0);
	}

	@Test
	void registrationOnApplicationContextPreparedAddsAction() {
		TestApplicationPreparedAction action = new TestApplicationPreparedAction();
		Registration<Integer> registration = this.registy.register(Integer.class, this.counter::getAndIncrement);
		registration.onApplicationContextPrepared(action::run);
		this.registy.applicationContextPrepared(this.context);
		assertThat(action).wasCalledOnlyOnce().hasInstanceValue(0);
	}

	@Test
	void registrationOnApplicationContextPreparedWhenActionIsNullDoesNotAddAction() {
		Registration<Integer> registration = this.registy.register(Integer.class, this.counter::getAndIncrement);
		registration.onApplicationContextPrepared(null);
		this.registy.applicationContextPrepared(this.context);
	}

	private static class TestApplicationPreparedAction implements AssertProvider<ApplicationPreparedActionAssert> {

		private Integer instance;

		private int called;

		void run(ConfigurableApplicationContext context, Integer instance) {
			this.instance = instance;
			this.called++;
		}

		@Override
		public ApplicationPreparedActionAssert assertThat() {
			return new ApplicationPreparedActionAssert(this);
		}

	}

	private static class ApplicationPreparedActionAssert
			extends AbstractAssert<ApplicationPreparedActionAssert, TestApplicationPreparedAction> {

		ApplicationPreparedActionAssert(TestApplicationPreparedAction actual) {
			super(actual, ApplicationPreparedActionAssert.class);
		}

		ApplicationPreparedActionAssert hasInstanceValue(Integer expected) {
			assertThat(this.actual.instance).isEqualTo(expected);
			return this;
		}

		ApplicationPreparedActionAssert wasCalledOnlyOnce() {
			assertThat(this.actual.called).as("action calls").isEqualTo(1);
			return this;
		}

		ApplicationPreparedActionAssert wasNotCalled() {
			assertThat(this.actual.called).as("action calls").isEqualTo(0);
			return this;
		}

	}

}
