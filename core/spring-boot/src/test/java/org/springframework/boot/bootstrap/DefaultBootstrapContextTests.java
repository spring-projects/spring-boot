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

package org.springframework.boot.bootstrap;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.AssertProvider;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.boot.bootstrap.BootstrapRegistry.InstanceSupplier;
import org.springframework.boot.bootstrap.BootstrapRegistry.Scope;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.StaticApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DefaultBootstrapContext}.
 *
 * @author Phillip Webb
 */
class DefaultBootstrapContextTests {

	private final DefaultBootstrapContext context = new DefaultBootstrapContext();

	private final AtomicInteger counter = new AtomicInteger();

	private final StaticApplicationContext applicationContext = new StaticApplicationContext();

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void registerWhenTypeIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.context.register(null, InstanceSupplier.of(1)))
			.withMessage("'type' must not be null");
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void registerWhenRegistrationIsNullThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.context.register(Integer.class, null))
			.withMessage("'instanceSupplier' must not be null");
	}

	@Test
	void registerWhenNotAlreadyRegisteredRegistersInstance() {
		this.context.register(Integer.class, InstanceSupplier.from(this.counter::getAndIncrement));
		assertThat(this.context.get(Integer.class)).isZero();
		assertThat(this.context.get(Integer.class)).isZero();
	}

	@Test
	void registerWhenAlreadyRegisteredRegistersReplacedInstance() {
		this.context.register(Integer.class, InstanceSupplier.from(this.counter::getAndIncrement));
		this.context.register(Integer.class, InstanceSupplier.of(100));
		assertThat(this.context.get(Integer.class)).isEqualTo(100);
		assertThat(this.context.get(Integer.class)).isEqualTo(100);
	}

	@Test
	void registerWhenSingletonAlreadyCreatedThrowsException() {
		this.context.register(Integer.class, InstanceSupplier.from(this.counter::getAndIncrement));
		this.context.get(Integer.class);
		assertThatIllegalStateException()
			.isThrownBy(() -> this.context.register(Integer.class, InstanceSupplier.of(100)))
			.withMessage("java.lang.Integer has already been created");
	}

	@Test
	void registerWhenPrototypeAlreadyCreatedReplacesInstance() {
		this.context.register(Integer.class,
				InstanceSupplier.from(this.counter::getAndIncrement).withScope(Scope.PROTOTYPE));
		this.context.get(Integer.class);
		this.context.register(Integer.class, InstanceSupplier.of(100));
		assertThat(this.context.get(Integer.class)).isEqualTo(100);
	}

	@Test
	void registerWhenAlreadyCreatedThrowsException() {
		this.context.register(Integer.class, InstanceSupplier.from(this.counter::getAndIncrement));
		this.context.get(Integer.class);
		assertThatIllegalStateException()
			.isThrownBy(() -> this.context.register(Integer.class, InstanceSupplier.of(100)))
			.withMessage("java.lang.Integer has already been created");
	}

	@Test
	void registerWithDependencyRegistersInstance() {
		this.context.register(Integer.class, InstanceSupplier.of(100));
		this.context.register(String.class, this::integerAsString);
		assertThat(this.context.get(String.class)).isEqualTo("100");
	}

	private String integerAsString(BootstrapContext context) {
		return String.valueOf(context.get(Integer.class));
	}

	@Test
	void registerIfAbsentWhenAbsentRegisters() {
		this.context.registerIfAbsent(Long.class, InstanceSupplier.of(100L));
		assertThat(this.context.get(Long.class)).isEqualTo(100L);
	}

	@Test
	void registerIfAbsentWhenPresentDoesNotRegister() {
		this.context.registerIfAbsent(Long.class, InstanceSupplier.of(1L));
		this.context.registerIfAbsent(Long.class, InstanceSupplier.of(100L));
		assertThat(this.context.get(Long.class)).isOne();
	}

	@Test
	void isRegisteredWhenNotRegisteredReturnsFalse() {
		this.context.register(Number.class, InstanceSupplier.of(1));
		assertThat(this.context.isRegistered(Long.class)).isFalse();
	}

	@Test
	void isRegisteredWhenRegisteredReturnsTrue() {
		this.context.register(Number.class, InstanceSupplier.of(1));
		assertThat(this.context.isRegistered(Number.class)).isTrue();
	}

	@Test
	void getRegisteredInstanceSupplierWhenNotRegisteredReturnsNull() {
		this.context.register(Number.class, InstanceSupplier.of(1));
		assertThat(this.context.getRegisteredInstanceSupplier(Long.class)).isNull();
	}

	@Test
	void getRegisteredInstanceSupplierWhenRegisteredReturnsRegistration() {
		InstanceSupplier<Number> instanceSupplier = InstanceSupplier.of(1);
		this.context.register(Number.class, instanceSupplier);
		assertThat(this.context.getRegisteredInstanceSupplier(Number.class)).isSameAs(instanceSupplier);
	}

	@Test
	void getWhenNoRegistrationThrowsIllegalStateException() {
		this.context.register(Number.class, InstanceSupplier.of(1));
		assertThatIllegalStateException().isThrownBy(() -> this.context.get(Long.class))
			.withMessageContaining("has not been registered");
	}

	@Test
	void getWhenRegisteredAsNullReturnsNull() {
		this.context.register(Number.class, InstanceSupplier.of(null));
		assertThat(this.context.get(Number.class)).isNull();
	}

	@Test
	void getWhenSingletonCreatesOnlyOneInstance() {
		this.context.register(Integer.class, InstanceSupplier.from(this.counter::getAndIncrement));
		assertThat(this.context.get(Integer.class)).isZero();
		assertThat(this.context.get(Integer.class)).isZero();
	}

	@Test
	void getWhenPrototypeCreatesOnlyNewInstances() {
		this.context.register(Integer.class,
				InstanceSupplier.from(this.counter::getAndIncrement).withScope(Scope.PROTOTYPE));
		assertThat(this.context.get(Integer.class)).isZero();
		assertThat(this.context.get(Integer.class)).isOne();
	}

	@Test
	void testName() {

	}

	@Test
	void getOrElseWhenNoRegistrationReturnsOther() {
		this.context.register(Number.class, InstanceSupplier.of(1));
		assertThat(this.context.getOrElse(Long.class, -1L)).isEqualTo(-1);
	}

	@Test
	void getOrElseWhenRegisteredAsNullReturnsNull() {
		this.context.register(Number.class, InstanceSupplier.of(null));
		assertThat(this.context.getOrElse(Number.class, -1)).isNull();
	}

	@Test
	void getOrElseCreatesReturnsOnlyOneInstance() {
		this.context.register(Integer.class, InstanceSupplier.from(this.counter::getAndIncrement));
		assertThat(this.context.getOrElse(Integer.class, -1)).isZero();
		assertThat(this.context.getOrElse(Integer.class, -1)).isZero();
	}

	@Test
	void getOrElseSupplyWhenNoRegistrationReturnsSupplied() {
		this.context.register(Number.class, InstanceSupplier.of(1));
		Supplier<@Nullable Long> supplier = () -> -1L;
		assertThat(this.context.getOrElseSupply(Long.class, supplier)).isEqualTo(-1);
	}

	@Test
	void getOrElseSupplyWhenRegisteredAsNullReturnsNull() {
		this.context.register(Number.class, InstanceSupplier.of(null));
		Supplier<@Nullable Number> supplier = () -> -1L;
		assertThat(this.context.getOrElseSupply(Number.class, supplier)).isNull();
	}

	@Test
	void getOrElseSupplyCreatesOnlyOneInstance() {
		this.context.register(Integer.class, InstanceSupplier.from(this.counter::getAndIncrement));
		Supplier<@Nullable Integer> supplier = () -> -1;
		assertThat(this.context.getOrElseSupply(Integer.class, supplier)).isZero();
		assertThat(this.context.getOrElseSupply(Integer.class, supplier)).isZero();
	}

	@Test
	void getOrElseThrowWhenNoRegistrationThrowsSuppliedException() {
		this.context.register(Number.class, InstanceSupplier.of(1));
		assertThatIOException().isThrownBy(() -> this.context.getOrElseThrow(Long.class, IOException::new));
	}

	@Test
	void getOrElseThrowWhenRegisteredAsNullReturnsNull() {
		this.context.register(Number.class, InstanceSupplier.of(null));
		assertThat(this.context.getOrElseThrow(Number.class, RuntimeException::new)).isNull();
	}

	@Test
	void getOrElseThrowCreatesOnlyOneInstance() {
		this.context.register(Integer.class, InstanceSupplier.from(this.counter::getAndIncrement));
		assertThat(this.context.getOrElseThrow(Integer.class, RuntimeException::new)).isZero();
		assertThat(this.context.getOrElseThrow(Integer.class, RuntimeException::new)).isZero();
	}

	@Test
	void closeMulticastsEventToListeners() {
		TestCloseListener listener = new TestCloseListener();
		this.context.addCloseListener(listener);
		assertThat(listener).wasNotCalled();
		this.context.close(this.applicationContext);
		assertThat(listener).wasCalledOnlyOnce()
			.hasBootstrapContextSameAs(this.context)
			.hasApplicationContextSameAs(this.applicationContext);
	}

	@Test
	void addCloseListenerIgnoresMultipleCallsWithSameListener() {
		TestCloseListener listener = new TestCloseListener();
		this.context.addCloseListener(listener);
		this.context.addCloseListener(listener);
		this.context.close(this.applicationContext);
		assertThat(listener).wasCalledOnlyOnce();
	}

	@Test
	void instanceSupplierGetScopeWhenNotConfiguredReturnsSingleton() {
		InstanceSupplier<String> supplier = InstanceSupplier.of("test");
		assertThat(supplier.getScope()).isEqualTo(Scope.SINGLETON);
		assertThat(supplier.get(mock(BootstrapContext.class))).isEqualTo("test");
	}

	@Test
	void instanceSupplierWithScopeChangesScope() {
		InstanceSupplier<String> supplier = InstanceSupplier.of("test").withScope(Scope.PROTOTYPE);
		assertThat(supplier.getScope()).isEqualTo(Scope.PROTOTYPE);
		assertThat(supplier.get(mock(BootstrapContext.class))).isEqualTo("test");
	}

	private static final class TestCloseListener
			implements ApplicationListener<BootstrapContextClosedEvent>, AssertProvider<CloseListenerAssert> {

		private int called;

		private @Nullable BootstrapContext bootstrapContext;

		private @Nullable ConfigurableApplicationContext applicationContext;

		@Override
		public void onApplicationEvent(BootstrapContextClosedEvent event) {
			this.called++;
			this.bootstrapContext = event.getBootstrapContext();
			this.applicationContext = event.getApplicationContext();
		}

		@Override
		public CloseListenerAssert assertThat() {
			return new CloseListenerAssert(this);
		}

	}

	private static class CloseListenerAssert extends AbstractAssert<CloseListenerAssert, TestCloseListener> {

		CloseListenerAssert(TestCloseListener actual) {
			super(actual, CloseListenerAssert.class);
		}

		CloseListenerAssert wasCalledOnlyOnce() {
			assertThat(this.actual.called).as("action calls").isOne();
			return this;
		}

		CloseListenerAssert wasNotCalled() {
			assertThat(this.actual.called).as("action calls").isZero();
			return this;
		}

		CloseListenerAssert hasBootstrapContextSameAs(BootstrapContext bootstrapContext) {
			assertThat(this.actual.bootstrapContext).isSameAs(bootstrapContext);
			return this;
		}

		CloseListenerAssert hasApplicationContextSameAs(ApplicationContext applicationContext) {
			assertThat(this.actual.applicationContext).isSameAs(applicationContext);
			return this;
		}

	}

}
