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

import io.grpc.Grpc;
import io.grpc.protobuf.services.HealthStatusManager;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.task.DefaultTaskSchedulerConfiguration;
import org.springframework.boot.grpc.server.autoconfigure.health.GrpcServerHealthProperties.Schedule;
import org.springframework.boot.grpc.server.health.GrpcServerHealth;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.grpc.server.GrpcServerFactory;
import org.springframework.scheduling.TaskScheduler;

/**
 * {@link EnableAutoConfiguration Auto-configuration} to invoke {@link GrpcServerHealth}
 * updates using a {@link TaskScheduler}.
 *
 * @author Phillip Webb
 * @since 4.1.0
 */
@AutoConfiguration(after = GrpcServerHealthAutoConfiguration.class)
@ConditionalOnBooleanProperty(name = "spring.grpc.server.health.schedule.enabled", matchIfMissing = true)
@Import(DefaultTaskSchedulerConfiguration.class)
@ConditionalOnClass({ GrpcServerFactory.class, Grpc.class, HealthStatusManager.class })
public final class GrpcServerHealthSchedulerAutoConfiguration {

	@Bean
	@ConditionalOnBean({ TaskScheduler.class, GrpcServerHealth.class })
	GrpcServerHealthScheduler grpcServerHealthScheduler(GrpcServerHealth grpcServerHealth,
			HealthStatusManager grpcServerHealthStatusManager, TaskScheduler taskScheduler,
			GrpcServerHealthProperties properties) {
		Schedule schedule = properties.getSchedule();
		return new GrpcServerHealthScheduler(grpcServerHealth, grpcServerHealthStatusManager, taskScheduler,
				schedule.getPeriod(), schedule.getDelay());
	}

}
