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

package org.springframework.boot.grpc.server.autoconfigure.health;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import io.grpc.protobuf.services.HealthStatusManager;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.boot.grpc.server.health.GrpcServerHealth;
import org.springframework.grpc.server.lifecycle.GrpcServerStartedEvent;
import org.springframework.scheduling.TaskScheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

/**
 * Tests for {@link GrpcServerHealthScheduler}.
 *
 * @author Phillip Webb
 */
class GrpcServerHealthSchedulerTests {

	@Test
	void onApplicationEventWhenEventIsGrpcStartStartsHealth() {
		Clock clock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
		GrpcServerHealth serverHealth = mock();
		HealthStatusManager statusManager = mock();
		TaskScheduler taskScheduler = mock();
		Duration period = Duration.ofSeconds(10);
		Duration delay = Duration.ofSeconds(30);
		GrpcServerHealthScheduler healthScheduler = new GrpcServerHealthScheduler(clock, serverHealth, statusManager,
				taskScheduler, period, delay);
		then(serverHealth).should(never()).update(statusManager);
		healthScheduler.onApplicationEvent(new GrpcServerStartedEvent(mock(), mock(), "localhost", 123));
		Instant startTime = Instant.now(clock).plus(delay);
		ArgumentCaptor<Runnable> runnable = ArgumentCaptor.captor();
		then(taskScheduler).should().scheduleAtFixedRate(runnable.capture(), eq(startTime), eq(period));
		then(serverHealth).should(never()).update(statusManager);
		runnable.getValue().run();
		then(serverHealth).should().update(statusManager);
	}

	@Test
	void onApplicationEventWhenEventIsGrpcStartAndCalledTwiceStartsHealthOnlyOnce() {
		GrpcServerHealth serverHealth = mock();
		HealthStatusManager statusManager = mock();
		TaskScheduler taskScheduler = mock();
		GrpcServerHealthScheduler healthScheduler = new GrpcServerHealthScheduler(serverHealth, statusManager,
				taskScheduler, Duration.ofSeconds(10), Duration.ofSeconds(30));
		then(serverHealth).should(never()).update(statusManager);
		healthScheduler.onApplicationEvent(new GrpcServerStartedEvent(mock(), mock(), "localhost", 123));
		healthScheduler.onApplicationEvent(new GrpcServerStartedEvent(mock(), mock(), "localhost", 345));
		then(taskScheduler).should(times(1)).scheduleAtFixedRate(any(), any(), any());
	}

}
