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
import java.util.Map;

import io.grpc.protobuf.services.HealthStatusManager;
import io.grpc.servlet.jakarta.GrpcServlet;
import jakarta.servlet.ServletRegistration;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.boot.grpc.server.GrpcServletRegistration;
import org.springframework.boot.grpc.server.health.GrpcServerHealth;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.grpc.server.lifecycle.GrpcServerStartedEvent;
import org.springframework.mock.web.MockServletContext;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.context.support.GenericWebApplicationContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
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
	void onGrpcServerStartedEventWhenEventIsGrpcStartStartsHealth() {
		Clock clock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
		GrpcServerHealth serverHealth = mock();
		HealthStatusManager statusManager = mock();
		TaskScheduler taskScheduler = mock();
		Duration period = Duration.ofSeconds(10);
		Duration delay = Duration.ofSeconds(30);
		ConfigurableApplicationContext context = new GenericApplicationContext();
		context.refresh();
		new GrpcServerHealthScheduler(context, serverHealth, statusManager, taskScheduler, period, delay, clock);
		then(serverHealth).should(never()).update(statusManager);
		context.publishEvent(new GrpcServerStartedEvent(mock(), mock(), "localhost", 123));
		Instant startTime = Instant.now(clock).plus(delay);
		ArgumentCaptor<Runnable> runnable = ArgumentCaptor.captor();
		then(taskScheduler).should().scheduleAtFixedRate(runnable.capture(), eq(startTime), eq(period));
		then(serverHealth).should(never()).update(statusManager);
		runnable.getValue().run();
		then(serverHealth).should().update(statusManager);
	}

	@Test
	void onGrpcServerStartedEventWhenEventIsGrpcStartAndCalledTwiceStartsHealthOnlyOnce() {
		GrpcServerHealth serverHealth = mock();
		HealthStatusManager statusManager = mock();
		TaskScheduler taskScheduler = mock();
		ConfigurableApplicationContext context = new GenericApplicationContext();
		context.refresh();
		new GrpcServerHealthScheduler(context, serverHealth, statusManager, taskScheduler, Duration.ofSeconds(10),
				Duration.ofSeconds(30));
		then(serverHealth).should(never()).update(statusManager);
		context.publishEvent(new GrpcServerStartedEvent(mock(), mock(), "localhost", 123));
		context.publishEvent(new GrpcServerStartedEvent(mock(), mock(), "localhost", 345));
		then(taskScheduler).should(times(1)).scheduleAtFixedRate(any(), any(), any());
	}

	@Test
	void webApplicationWithGrpcServletRegistrationBeanStartsHealth() {
		GrpcServerHealth serverHealth = mock();
		HealthStatusManager statusManager = mock();
		TaskScheduler taskScheduler = mock();
		GenericWebApplicationContext context = new GenericWebApplicationContext();
		context.registerBean("registartion", GrpcServletRegistration.class, () -> mock());
		context.refresh();
		new GrpcServerHealthScheduler(context, serverHealth, statusManager, taskScheduler, Duration.ofSeconds(10),
				Duration.ofSeconds(30));
		then(taskScheduler).should(times(1)).scheduleAtFixedRate(any(), any(), any());
	}

	@Test
	void webApplicationWithGrpcServletRegistrationStartsHealth() {
		GrpcServerHealth serverHealth = mock();
		HealthStatusManager statusManager = mock();
		TaskScheduler taskScheduler = mock();
		MockServletContext servletContext = new MockServletContext() {

			@Override
			public java.util.Map<String, ServletRegistration> getServletRegistrations() {
				ServletRegistration registration = mock();
				given(registration.getClassName()).willReturn(GrpcServlet.class.getName());
				return Map.of("grpc", registration);
			}

		};
		GenericWebApplicationContext context = new GenericWebApplicationContext();
		context.setServletContext(servletContext);
		context.refresh();
		new GrpcServerHealthScheduler(context, serverHealth, statusManager, taskScheduler, Duration.ofSeconds(10),
				Duration.ofSeconds(30));
		then(taskScheduler).should(times(1)).scheduleAtFixedRate(any(), any(), any());

	}

}
