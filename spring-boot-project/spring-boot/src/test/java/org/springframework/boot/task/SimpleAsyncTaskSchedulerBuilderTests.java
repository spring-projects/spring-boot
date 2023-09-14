/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.task;

import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

import org.springframework.scheduling.concurrent.SimpleAsyncTaskScheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * Tests for {@link SimpleAsyncTaskSchedulerBuilder}.
 *
 * @author Stephane Nicoll
 * @author Moritz Halbritter
 */
class SimpleAsyncTaskSchedulerBuilderTests {

	private final SimpleAsyncTaskSchedulerBuilder builder = new SimpleAsyncTaskSchedulerBuilder();

	@Test
	void threadNamePrefixShouldApply() {
		SimpleAsyncTaskScheduler scheduler = this.builder.threadNamePrefix("test-").build();
		assertThat(scheduler.getThreadNamePrefix()).isEqualTo("test-");
	}

	@Test
	void concurrencyLimitShouldApply() {
		SimpleAsyncTaskScheduler scheduler = this.builder.concurrencyLimit(1).build();
		assertThat(scheduler.getConcurrencyLimit()).isEqualTo(1);
	}

	@Test
	@EnabledForJreRange(min = JRE.JAVA_21)
	void virtualThreadsShouldApply() {
		SimpleAsyncTaskScheduler scheduler = this.builder.virtualThreads(true).build();
		assertThat(scheduler).extracting("virtualThreadDelegate").isNotNull();
	}

	@Test
	void customizersWhenCustomizersAreNullShouldThrowException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> this.builder.customizers((SimpleAsyncTaskSchedulerCustomizer[]) null))
			.withMessageContaining("Customizers must not be null");
	}

	@Test
	void customizersCollectionWhenCustomizersAreNullShouldThrowException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> this.builder.customizers((Set<SimpleAsyncTaskSchedulerCustomizer>) null))
			.withMessageContaining("Customizers must not be null");
	}

	@Test
	void customizersShouldApply() {
		SimpleAsyncTaskSchedulerCustomizer customizer = mock(SimpleAsyncTaskSchedulerCustomizer.class);
		SimpleAsyncTaskScheduler scheduler = this.builder.customizers(customizer).build();
		then(customizer).should().customize(scheduler);
	}

	@Test
	void customizersShouldBeAppliedLast() {
		SimpleAsyncTaskScheduler scheduler = spy(new SimpleAsyncTaskScheduler());
		this.builder.concurrencyLimit(1).threadNamePrefix("test-").additionalCustomizers((taskScheduler) -> {
			then(taskScheduler).should().setConcurrencyLimit(1);
			then(taskScheduler).should().setThreadNamePrefix("test-");
		});
		this.builder.configure(scheduler);
	}

	@Test
	void customizersShouldReplaceExisting() {
		SimpleAsyncTaskSchedulerCustomizer customizer1 = mock(SimpleAsyncTaskSchedulerCustomizer.class);
		SimpleAsyncTaskSchedulerCustomizer customizer2 = mock(SimpleAsyncTaskSchedulerCustomizer.class);
		SimpleAsyncTaskScheduler scheduler = this.builder.customizers(customizer1)
			.customizers(Collections.singleton(customizer2))
			.build();
		then(customizer1).shouldHaveNoInteractions();
		then(customizer2).should().customize(scheduler);
	}

	@Test
	void additionalCustomizersWhenCustomizersAreNullShouldThrowException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> this.builder.additionalCustomizers((SimpleAsyncTaskSchedulerCustomizer[]) null))
			.withMessageContaining("Customizers must not be null");
	}

	@Test
	void additionalCustomizersCollectionWhenCustomizersAreNullShouldThrowException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> this.builder.additionalCustomizers((Set<SimpleAsyncTaskSchedulerCustomizer>) null))
			.withMessageContaining("Customizers must not be null");
	}

	@Test
	void additionalCustomizersShouldAddToExisting() {
		SimpleAsyncTaskSchedulerCustomizer customizer1 = mock(SimpleAsyncTaskSchedulerCustomizer.class);
		SimpleAsyncTaskSchedulerCustomizer customizer2 = mock(SimpleAsyncTaskSchedulerCustomizer.class);
		SimpleAsyncTaskScheduler scheduler = this.builder.customizers(customizer1)
			.additionalCustomizers(customizer2)
			.build();
		then(customizer1).should().customize(scheduler);
		then(customizer2).should().customize(scheduler);
	}

}
