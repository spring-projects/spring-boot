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

package org.springframework.boot.actuate.metrics.export.prometheus;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.PushGateway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.boot.actuate.metrics.export.prometheus.PrometheusPushGatewayManager.PushGatewayTaskScheduler;
import org.springframework.boot.actuate.metrics.export.prometheus.PrometheusPushGatewayManager.ShutdownOperation;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

/**
 * Tests for {@link PrometheusPushGatewayManager}.
 *
 * @author Phillip Webb
 */
@ExtendWith(MockitoExtension.class)
class PrometheusPushGatewayManagerTests {

	@Mock
	private PushGateway pushGateway;

	@Mock
	private CollectorRegistry registry;

	@Mock
	private TaskScheduler scheduler;

	private Duration pushRate = Duration.ofSeconds(1);

	private Map<String, String> groupingKey = Collections.singletonMap("foo", "bar");

	@Captor
	private ArgumentCaptor<Runnable> task;

	@Mock
	private ScheduledFuture<Object> future;

	@Test
	void createWhenPushGatewayIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new PrometheusPushGatewayManager(null, this.registry,
				this.scheduler, this.pushRate, "job", this.groupingKey, null))
				.withMessage("PushGateway must not be null");
	}

	@Test
	void createWhenCollectorRegistryIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new PrometheusPushGatewayManager(this.pushGateway, null,
				this.scheduler, this.pushRate, "job", this.groupingKey, null)).withMessage("Registry must not be null");
	}

	@Test
	void createWhenSchedulerIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new PrometheusPushGatewayManager(this.pushGateway,
				this.registry, null, this.pushRate, "job", this.groupingKey, null))
				.withMessage("Scheduler must not be null");
	}

	@Test
	void createWhenPushRateIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new PrometheusPushGatewayManager(this.pushGateway,
				this.registry, this.scheduler, null, "job", this.groupingKey, null))
				.withMessage("PushRate must not be null");
	}

	@Test
	void createWhenJobIsEmptyThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new PrometheusPushGatewayManager(this.pushGateway,
				this.registry, this.scheduler, this.pushRate, "", this.groupingKey, null))
				.withMessage("Job must not be empty");
	}

	@Test
	void createShouldSchedulePushAsFixedRate() throws Exception {
		new PrometheusPushGatewayManager(this.pushGateway, this.registry, this.scheduler, this.pushRate, "job",
				this.groupingKey, null);
		then(this.scheduler).should().scheduleAtFixedRate(this.task.capture(), eq(this.pushRate));
		this.task.getValue().run();
		then(this.pushGateway).should().pushAdd(this.registry, "job", this.groupingKey);
	}

	@Test
	void shutdownWhenOwnsSchedulerDoesShutdownScheduler() {
		PushGatewayTaskScheduler ownedScheduler = givenScheduleAtFixedRateWillReturnFuture(
				mock(PushGatewayTaskScheduler.class));
		PrometheusPushGatewayManager manager = new PrometheusPushGatewayManager(this.pushGateway, this.registry,
				ownedScheduler, this.pushRate, "job", this.groupingKey, null);
		manager.shutdown();
		then(ownedScheduler).should().shutdown();
	}

	@Test
	void shutdownWhenDoesNotOwnSchedulerDoesNotShutdownScheduler() {
		ThreadPoolTaskScheduler otherScheduler = givenScheduleAtFixedRateWillReturnFuture(
				mock(ThreadPoolTaskScheduler.class));
		PrometheusPushGatewayManager manager = new PrometheusPushGatewayManager(this.pushGateway, this.registry,
				otherScheduler, this.pushRate, "job", this.groupingKey, null);
		manager.shutdown();
		then(otherScheduler).should(never()).shutdown();
	}

	@Test
	void shutdownWhenShutdownOperationIsPushPerformsPushOnShutdown() throws Exception {
		givenScheduleAtFixedRateWithReturnFuture();
		PrometheusPushGatewayManager manager = new PrometheusPushGatewayManager(this.pushGateway, this.registry,
				this.scheduler, this.pushRate, "job", this.groupingKey, ShutdownOperation.PUSH);
		manager.shutdown();
		then(this.future).should().cancel(false);
		then(this.pushGateway).should().pushAdd(this.registry, "job", this.groupingKey);
	}

	@Test
	void shutdownWhenShutdownOperationIsDeletePerformsDeleteOnShutdown() throws Exception {
		givenScheduleAtFixedRateWithReturnFuture();
		PrometheusPushGatewayManager manager = new PrometheusPushGatewayManager(this.pushGateway, this.registry,
				this.scheduler, this.pushRate, "job", this.groupingKey, ShutdownOperation.DELETE);
		manager.shutdown();
		then(this.future).should().cancel(false);
		then(this.pushGateway).should().delete("job", this.groupingKey);
	}

	@Test
	void shutdownWhenShutdownOperationIsNoneDoesNothing() {
		givenScheduleAtFixedRateWithReturnFuture();
		PrometheusPushGatewayManager manager = new PrometheusPushGatewayManager(this.pushGateway, this.registry,
				this.scheduler, this.pushRate, "job", this.groupingKey, ShutdownOperation.NONE);
		manager.shutdown();
		then(this.future).should().cancel(false);
		then(this.pushGateway).shouldHaveNoInteractions();
	}

	@Test
	void pushDoesNotThrowException() throws Exception {
		new PrometheusPushGatewayManager(this.pushGateway, this.registry, this.scheduler, this.pushRate, "job",
				this.groupingKey, null);
		then(this.scheduler).should().scheduleAtFixedRate(this.task.capture(), eq(this.pushRate));
		willThrow(RuntimeException.class).given(this.pushGateway).pushAdd(this.registry, "job", this.groupingKey);
		this.task.getValue().run();
	}

	private void givenScheduleAtFixedRateWithReturnFuture() {
		givenScheduleAtFixedRateWillReturnFuture(this.scheduler);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private <T extends TaskScheduler> T givenScheduleAtFixedRateWillReturnFuture(T scheduler) {
		given(scheduler.scheduleAtFixedRate(isA(Runnable.class), isA(Duration.class)))
				.willReturn((ScheduledFuture) this.future);
		return scheduler;
	}

}
