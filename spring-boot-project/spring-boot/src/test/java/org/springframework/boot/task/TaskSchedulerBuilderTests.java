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

import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * Tests for {@link TaskSchedulerBuilder}.
 *
 * @author Stephane Nicoll
 */
class TaskSchedulerBuilderTests {

	private TaskSchedulerBuilder builder = new TaskSchedulerBuilder();

	@Test
	void poolSettingsShouldApply() {
		ThreadPoolTaskScheduler scheduler = this.builder.poolSize(4).build();
		assertThat(scheduler.getPoolSize()).isEqualTo(4);
	}

	@Test
	void awaitTerminationShouldApply() {
		ThreadPoolTaskScheduler executor = this.builder.awaitTermination(true).build();
		assertThat(executor).hasFieldOrPropertyWithValue("waitForTasksToCompleteOnShutdown", true);
	}

	@Test
	void awaitTerminationPeriodShouldApply() {
		Duration period = Duration.ofMinutes(1);
		ThreadPoolTaskScheduler executor = this.builder.awaitTerminationPeriod(period).build();
		assertThat(executor).hasFieldOrPropertyWithValue("awaitTerminationMillis", period.toMillis());
	}

	@Test
	void threadNamePrefixShouldApply() {
		ThreadPoolTaskScheduler scheduler = this.builder.threadNamePrefix("test-").build();
		assertThat(scheduler.getThreadNamePrefix()).isEqualTo("test-");
	}

	@Test
	void customizersWhenCustomizersAreNullShouldThrowException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.builder.customizers((TaskSchedulerCustomizer[]) null))
				.withMessageContaining("Customizers must not be null");
	}

	@Test
	void customizersCollectionWhenCustomizersAreNullShouldThrowException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.builder.customizers((Set<TaskSchedulerCustomizer>) null))
				.withMessageContaining("Customizers must not be null");
	}

	@Test
	void customizersShouldApply() {
		TaskSchedulerCustomizer customizer = mock(TaskSchedulerCustomizer.class);
		ThreadPoolTaskScheduler scheduler = this.builder.customizers(customizer).build();
		then(customizer).should().customize(scheduler);
	}

	@Test
	void customizersShouldBeAppliedLast() {
		ThreadPoolTaskScheduler scheduler = spy(new ThreadPoolTaskScheduler());
		this.builder.poolSize(4).threadNamePrefix("test-").additionalCustomizers((taskScheduler) -> {
			then(taskScheduler).should().setPoolSize(4);
			then(taskScheduler).should().setThreadNamePrefix("test-");
		});
		this.builder.configure(scheduler);
	}

	@Test
	void customizersShouldReplaceExisting() {
		TaskSchedulerCustomizer customizer1 = mock(TaskSchedulerCustomizer.class);
		TaskSchedulerCustomizer customizer2 = mock(TaskSchedulerCustomizer.class);
		ThreadPoolTaskScheduler scheduler = this.builder.customizers(customizer1)
				.customizers(Collections.singleton(customizer2)).build();
		then(customizer1).shouldHaveNoInteractions();
		then(customizer2).should().customize(scheduler);
	}

	@Test
	void additionalCustomizersWhenCustomizersAreNullShouldThrowException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.builder.additionalCustomizers((TaskSchedulerCustomizer[]) null))
				.withMessageContaining("Customizers must not be null");
	}

	@Test
	void additionalCustomizersCollectionWhenCustomizersAreNullShouldThrowException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.builder.additionalCustomizers((Set<TaskSchedulerCustomizer>) null))
				.withMessageContaining("Customizers must not be null");
	}

	@Test
	void additionalCustomizersShouldAddToExisting() {
		TaskSchedulerCustomizer customizer1 = mock(TaskSchedulerCustomizer.class);
		TaskSchedulerCustomizer customizer2 = mock(TaskSchedulerCustomizer.class);
		ThreadPoolTaskScheduler scheduler = this.builder.customizers(customizer1).additionalCustomizers(customizer2)
				.build();
		then(customizer1).should().customize(scheduler);
		then(customizer2).should().customize(scheduler);
	}

}
