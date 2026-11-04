/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.metrics.export.prometheus;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import io.prometheus.metrics.exporter.pushgateway.PushGateway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.boot.micrometer.metrics.export.prometheus.PrometheusPushGatewayManager.PushGatewayTaskScheduler;
import org.springframework.boot.micrometer.metrics.export.prometheus.PrometheusPushGatewayManager.ShutdownOperation;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;

/**
 * Tests for {@link PrometheusPushGatewayManager}.
 *
 * @author Phillip Webb
 * @author Rami Darras
 */
@ExtendWith(MockitoExtension.class)
class PrometheusPushGatewayManagerTests {

	@Mock
	private PushGateway pushGateway;

	@Mock
	private TaskScheduler scheduler;

	private final Duration pushRate = Duration.ofSeconds(1);

	@Captor
	private ArgumentCaptor<Runnable> task;

	@Mock
	private ScheduledFuture<Object> future;

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void createWhenPushGatewayIsNullThrowsException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new PrometheusPushGatewayManager(null, this.scheduler, this.pushRate, null))
			.withMessage("'pushGateway' must not be null");
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void createWhenSchedulerIsNullThrowsException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new PrometheusPushGatewayManager(this.pushGateway, null, this.pushRate, null))
			.withMessage("'scheduler' must not be null");
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void createWhenPushRateIsNullThrowsException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new PrometheusPushGatewayManager(this.pushGateway, this.scheduler, null, null))
			.withMessage("'pushRate' must not be null");
	}

	@Test
	void createShouldSchedulePushAsFixedRate() throws Exception {
		new PrometheusPushGatewayManager(this.pushGateway, this.scheduler, this.pushRate, null);
		then(this.scheduler).should().scheduleAtFixedRate(this.task.capture(), eq(this.pushRate));
		this.task.getValue().run();
		then(this.pushGateway).should().pushAdd();
	}

	@Test
	void shutdownWhenOwnsSchedulerDoesShutDownScheduler() throws Exception {
		PushGatewayTaskScheduler ownedScheduler = spy(new PushGatewayTaskScheduler());
		List<ScheduledExecutorService> executors = new ArrayList<>();
		willAnswer((invocation) -> {
			ScheduledExecutorService executor = (ScheduledExecutorService) invocation.callRealMethod();
			executors.add(executor);
			return executor;
		}).given(ownedScheduler).getScheduledExecutor();
		try {
			PrometheusPushGatewayManager manager = new PrometheusPushGatewayManager(this.pushGateway, ownedScheduler,
					Duration.ofDays(1), ShutdownOperation.NONE);
			ScheduledExecutorService executor = executors.get(0);
			assertThat(executor.submit(() -> Thread.currentThread().isDaemon()).get(10, TimeUnit.SECONDS)).isTrue();
			then(this.pushGateway).should().pushAdd();
			manager.shutdown();
			then(ownedScheduler).should().shutdown();
			assertThat(executor.isShutdown()).isTrue();
			assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
		}
		finally {
			executors.forEach(ScheduledExecutorService::shutdownNow);
		}
	}

	@Test
	void shutdownWhenDoesNotOwnSchedulerDoesNotShutDownScheduler() throws Exception {
		ThreadPoolTaskScheduler otherScheduler = spy(new ThreadPoolTaskScheduler());
		otherScheduler.initialize();
		try {
			PrometheusPushGatewayManager manager = new PrometheusPushGatewayManager(this.pushGateway, otherScheduler,
					Duration.ofDays(1), ShutdownOperation.NONE);
			manager.shutdown();
			then(otherScheduler).should(never()).shutdown();
			ScheduledExecutorService executor = otherScheduler.getScheduledExecutor();
			assertThat(executor.isShutdown()).isFalse();
			assertThat(executor.submit(() -> "running").get(10, TimeUnit.SECONDS)).isEqualTo("running");
		}
		finally {
			otherScheduler.shutdown();
		}
	}

	@Test
	void shutdownWhenShutdownOperationIsPostPerformsPushAddOnShutdown() throws Exception {
		givenScheduleAtFixedRateWithReturnFuture();
		PrometheusPushGatewayManager manager = new PrometheusPushGatewayManager(this.pushGateway, this.scheduler,
				this.pushRate, ShutdownOperation.POST);
		manager.shutdown();
		then(this.future).should().cancel(false);
		then(this.pushGateway).should().pushAdd();
	}

	@Test
	void shutdownWhenShutdownOperationIsPutPerformsPushOnShutdown() throws Exception {
		givenScheduleAtFixedRateWithReturnFuture();
		PrometheusPushGatewayManager manager = new PrometheusPushGatewayManager(this.pushGateway, this.scheduler,
				this.pushRate, ShutdownOperation.PUT);
		manager.shutdown();
		then(this.future).should().cancel(false);
		then(this.pushGateway).should().push();
	}

	@Test
	void shutdownWhenShutdownOperationIsDeletePerformsDeleteOnShutdown() throws Exception {
		givenScheduleAtFixedRateWithReturnFuture();
		PrometheusPushGatewayManager manager = new PrometheusPushGatewayManager(this.pushGateway, this.scheduler,
				this.pushRate, ShutdownOperation.DELETE);
		manager.shutdown();
		then(this.future).should().cancel(false);
		then(this.pushGateway).should().delete();
	}

	@Test
	void shutdownWhenShutdownOperationIsNoneDoesNothing() {
		givenScheduleAtFixedRateWithReturnFuture();
		PrometheusPushGatewayManager manager = new PrometheusPushGatewayManager(this.pushGateway, this.scheduler,
				this.pushRate, ShutdownOperation.NONE);
		manager.shutdown();
		then(this.future).should().cancel(false);
		then(this.pushGateway).shouldHaveNoInteractions();
	}

	@Test
	void pushDoesNotThrowException() throws Exception {
		new PrometheusPushGatewayManager(this.pushGateway, this.scheduler, this.pushRate, null);
		then(this.scheduler).should().scheduleAtFixedRate(this.task.capture(), eq(this.pushRate));
		willThrow(RuntimeException.class).given(this.pushGateway).pushAdd();
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
