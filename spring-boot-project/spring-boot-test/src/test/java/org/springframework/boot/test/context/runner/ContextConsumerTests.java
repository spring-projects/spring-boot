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

package org.springframework.boot.test.context.runner;

import java.util.function.IntPredicate;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
		assertThatThrownBy(() -> firstConsumer.andThen(secondConsumer).accept(mock(ApplicationContext.class)))
				.isInstanceOf(AssertionError.class);
		then(predicate).should().test(42);
		then(predicate).shouldHaveNoMoreInteractions();
	}

	@Test
	void andThenWithNull() {
		ContextConsumer<?> consumer = (context) -> {
		};
		assertThatIllegalArgumentException().isThrownBy(() -> consumer.andThen(null))
				.withMessage("After must not be null");
	}

}
