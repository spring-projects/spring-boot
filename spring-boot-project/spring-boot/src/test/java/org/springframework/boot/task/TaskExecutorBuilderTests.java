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

package org.springframework.boot.task;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * Tests for {@link TaskExecutorBuilder}.
 *
 * @author Stephane Nicoll
 * @author Filip Hrisafov
 */
class TaskExecutorBuilderTests {

	private TaskExecutorBuilder builder = new TaskExecutorBuilder();

	@Test
	void poolSettingsShouldApply() {
		ThreadPoolTaskExecutor executor = this.builder.queueCapacity(10).corePoolSize(4).maxPoolSize(8)
				.allowCoreThreadTimeOut(true).keepAlive(Duration.ofMinutes(1)).build();
		assertThat(executor).hasFieldOrPropertyWithValue("queueCapacity", 10);
		assertThat(executor.getCorePoolSize()).isEqualTo(4);
		assertThat(executor.getMaxPoolSize()).isEqualTo(8);
		assertThat(executor).hasFieldOrPropertyWithValue("allowCoreThreadTimeOut", true);
		assertThat(executor.getKeepAliveSeconds()).isEqualTo(60);
	}

	@Test
	void awaitTerminationShouldApply() {
		ThreadPoolTaskExecutor executor = this.builder.awaitTermination(true).build();
		assertThat(executor).hasFieldOrPropertyWithValue("waitForTasksToCompleteOnShutdown", true);
	}

	@Test
	void awaitTerminationPeriodShouldApplyWithMillisecondPrecision() {
		Duration period = Duration.ofMillis(50);
		ThreadPoolTaskExecutor executor = this.builder.awaitTerminationPeriod(period).build();
		assertThat(executor).hasFieldOrPropertyWithValue("awaitTerminationMillis", period.toMillis());
	}

	@Test
	void threadNamePrefixShouldApply() {
		ThreadPoolTaskExecutor executor = this.builder.threadNamePrefix("test-").build();
		assertThat(executor.getThreadNamePrefix()).isEqualTo("test-");
	}

	@Test
	void taskDecoratorShouldApply() {
		TaskDecorator taskDecorator = mock(TaskDecorator.class);
		ThreadPoolTaskExecutor executor = this.builder.taskDecorator(taskDecorator).build();
		assertThat(executor).extracting("taskDecorator").isSameAs(taskDecorator);
	}

	@Test
	void customizersWhenCustomizersAreNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.builder.customizers((TaskExecutorCustomizer[]) null))
				.withMessageContaining("Customizers must not be null");
	}

	@Test
	void customizersCollectionWhenCustomizersAreNullShouldThrowException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.builder.customizers((Set<TaskExecutorCustomizer>) null))
				.withMessageContaining("Customizers must not be null");
	}

	@Test
	void customizersShouldApply() {
		TaskExecutorCustomizer customizer = mock(TaskExecutorCustomizer.class);
		ThreadPoolTaskExecutor executor = this.builder.customizers(customizer).build();
		then(customizer).should().customize(executor);
	}

	@Test
	void customizersShouldBeAppliedLast() {
		TaskDecorator taskDecorator = mock(TaskDecorator.class);
		ThreadPoolTaskExecutor executor = spy(new ThreadPoolTaskExecutor());
		this.builder.queueCapacity(10).corePoolSize(4).maxPoolSize(8).allowCoreThreadTimeOut(true)
				.keepAlive(Duration.ofMinutes(1)).awaitTermination(true).awaitTerminationPeriod(Duration.ofSeconds(30))
				.threadNamePrefix("test-").taskDecorator(taskDecorator).additionalCustomizers((taskExecutor) -> {
					then(taskExecutor).should().setQueueCapacity(10);
					then(taskExecutor).should().setCorePoolSize(4);
					then(taskExecutor).should().setMaxPoolSize(8);
					then(taskExecutor).should().setAllowCoreThreadTimeOut(true);
					then(taskExecutor).should().setKeepAliveSeconds(60);
					then(taskExecutor).should().setWaitForTasksToCompleteOnShutdown(true);
					then(taskExecutor).should().setAwaitTerminationSeconds(30);
					then(taskExecutor).should().setThreadNamePrefix("test-");
					then(taskExecutor).should().setTaskDecorator(taskDecorator);
				});
		this.builder.configure(executor);
	}

	@Test
	void customizersShouldReplaceExisting() {
		TaskExecutorCustomizer customizer1 = mock(TaskExecutorCustomizer.class);
		TaskExecutorCustomizer customizer2 = mock(TaskExecutorCustomizer.class);
		ThreadPoolTaskExecutor executor = this.builder.customizers(customizer1)
				.customizers(Collections.singleton(customizer2)).build();
		then(customizer1).shouldHaveNoInteractions();
		then(customizer2).should().customize(executor);
	}

	@Test
	void additionalCustomizersWhenCustomizersAreNullShouldThrowException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.builder.additionalCustomizers((TaskExecutorCustomizer[]) null))
				.withMessageContaining("Customizers must not be null");
	}

	@Test
	void additionalCustomizersCollectionWhenCustomizersAreNullShouldThrowException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.builder.additionalCustomizers((Set<TaskExecutorCustomizer>) null))
				.withMessageContaining("Customizers must not be null");
	}

	@Test
	void additionalCustomizersShouldAddToExisting() {
		TaskExecutorCustomizer customizer1 = mock(TaskExecutorCustomizer.class);
		TaskExecutorCustomizer customizer2 = mock(TaskExecutorCustomizer.class);
		ThreadPoolTaskExecutor executor = this.builder.customizers(customizer1).additionalCustomizers(customizer2)
				.build();
		then(customizer1).should().customize(executor);
		then(customizer2).should().customize(executor);
	}

}
