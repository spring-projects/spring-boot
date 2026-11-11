/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.context.runner;

import java.util.function.IntPredicate;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ContextConsumer}.
 *
 * @author Stephane Nicoll
 */
class ContextConsumerTests {

	@Test
	void andThenInvokeInOrder() throws Throwable {
		IntPredicate predicate = mock(IntPredicate.class);
		given(predicate.test(42)).willReturn(true);
		given(predicate.test(24)).willReturn(false);
		ContextConsumer<ApplicationContext> firstConsumer = (context) -> assertThat(predicate.test(42)).isTrue();
		ContextConsumer<ApplicationContext> secondConsumer = (context) -> assertThat(predicate.test(24)).isFalse();
		firstConsumer.andThen(secondConsumer).accept(mock(ApplicationContext.class));
		InOrder ordered = inOrder(predicate);
		ordered.verify(predicate).test(42);
		ordered.verify(predicate).test(24);
		ordered.verifyNoMoreInteractions();
	}

	@Test
	void andThenNoInvokedIfThisFails() {
		IntPredicate predicate = mock(IntPredicate.class);
		given(predicate.test(42)).willReturn(true);
		given(predicate.test(24)).willReturn(false);
		ContextConsumer<ApplicationContext> firstConsumer = (context) -> assertThat(predicate.test(42)).isFalse();
		ContextConsumer<ApplicationContext> secondConsumer = (context) -> assertThat(predicate.test(24)).isFalse();
		assertThatExceptionOfType(AssertionError.class)
			.isThrownBy(() -> firstConsumer.andThen(secondConsumer).accept(mock(ApplicationContext.class)));
		then(predicate).should().test(42);
		then(predicate).shouldHaveNoMoreInteractions();
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void andThenWithNull() {
		ContextConsumer<?> consumer = (context) -> {
		};
		assertThatIllegalArgumentException().isThrownBy(() -> consumer.andThen(null))
			.withMessage("'after' must not be null");
	}

}
