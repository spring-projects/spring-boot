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

import org.springframework.boot.testsupport.assertj.SimpleAsyncTaskExecutorAssert;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskDecorator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * Tests for {@link SimpleAsyncTaskExecutorBuilder}.
 *
 * @author Stephane Nicoll
 * @author Filip Hrisafov
 * @author Moritz Halbritter
 */
class SimpleAsyncTaskExecutorBuilderTests {

	private final SimpleAsyncTaskExecutorBuilder builder = new SimpleAsyncTaskExecutorBuilder();

	@Test
	void threadNamePrefixShouldApply() {
		SimpleAsyncTaskExecutor executor = this.builder.threadNamePrefix("test-").build();
		assertThat(executor.getThreadNamePrefix()).isEqualTo("test-");
	}

	@Test
	@EnabledForJreRange(min = JRE.JAVA_21)
	void virtualThreadsShouldApply() {
		SimpleAsyncTaskExecutor executor = this.builder.virtualThreads(true).build();
		SimpleAsyncTaskExecutorAssert.assertThat(executor).usesVirtualThreads();
	}

	@Test
	void concurrencyLimitShouldApply() {
		SimpleAsyncTaskExecutor executor = this.builder.concurrencyLimit(1).build();
		assertThat(executor.getConcurrencyLimit()).isEqualTo(1);
	}

	@Test
	void taskDecoratorShouldApply() {
		TaskDecorator taskDecorator = mock(TaskDecorator.class);
		SimpleAsyncTaskExecutor executor = this.builder.taskDecorator(taskDecorator).build();
		assertThat(executor).extracting("taskDecorator").isSameAs(taskDecorator);
	}

	@Test
	void customizersWhenCustomizersAreNullShouldThrowException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> this.builder.customizers((SimpleAsyncTaskExecutorCustomizer[]) null))
			.withMessageContaining("Customizers must not be null");
	}

	@Test
	void customizersCollectionWhenCustomizersAreNullShouldThrowException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> this.builder.customizers((Set<SimpleAsyncTaskExecutorCustomizer>) null))
			.withMessageContaining("Customizers must not be null");
	}

	@Test
	void customizersShouldApply() {
		SimpleAsyncTaskExecutorCustomizer customizer = mock(SimpleAsyncTaskExecutorCustomizer.class);
		SimpleAsyncTaskExecutor executor = this.builder.customizers(customizer).build();
		then(customizer).should().customize(executor);
	}

	@Test
	void customizersShouldBeAppliedLast() {
		TaskDecorator taskDecorator = mock(TaskDecorator.class);
		SimpleAsyncTaskExecutor executor = spy(new SimpleAsyncTaskExecutor());
		this.builder.threadNamePrefix("test-")
			.virtualThreads(true)
			.concurrencyLimit(1)
			.taskDecorator(taskDecorator)
			.additionalCustomizers((taskExecutor) -> {
				then(taskExecutor).should().setConcurrencyLimit(1);
				then(taskExecutor).should().setVirtualThreads(true);
				then(taskExecutor).should().setThreadNamePrefix("test-");
				then(taskExecutor).should().setTaskDecorator(taskDecorator);
			});
		this.builder.configure(executor);
	}

	@Test
	void customizersShouldReplaceExisting() {
		SimpleAsyncTaskExecutorCustomizer customizer1 = mock(SimpleAsyncTaskExecutorCustomizer.class);
		SimpleAsyncTaskExecutorCustomizer customizer2 = mock(SimpleAsyncTaskExecutorCustomizer.class);
		SimpleAsyncTaskExecutor executor = this.builder.customizers(customizer1)
			.customizers(Collections.singleton(customizer2))
			.build();
		then(customizer1).shouldHaveNoInteractions();
		then(customizer2).should().customize(executor);
	}

	@Test
	void additionalCustomizersWhenCustomizersAreNullShouldThrowException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> this.builder.additionalCustomizers((SimpleAsyncTaskExecutorCustomizer[]) null))
			.withMessageContaining("Customizers must not be null");
	}

	@Test
	void additionalCustomizersCollectionWhenCustomizersAreNullShouldThrowException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> this.builder.additionalCustomizers((Set<SimpleAsyncTaskExecutorCustomizer>) null))
			.withMessageContaining("Customizers must not be null");
	}

	@Test
	void additionalCustomizersShouldAddToExisting() {
		SimpleAsyncTaskExecutorCustomizer customizer1 = mock(SimpleAsyncTaskExecutorCustomizer.class);
		SimpleAsyncTaskExecutorCustomizer customizer2 = mock(SimpleAsyncTaskExecutorCustomizer.class);
		SimpleAsyncTaskExecutor executor = this.builder.customizers(customizer1)
			.additionalCustomizers(customizer2)
			.build();
		then(customizer1).should().customize(executor);
		then(customizer2).should().customize(executor);
	}

}
