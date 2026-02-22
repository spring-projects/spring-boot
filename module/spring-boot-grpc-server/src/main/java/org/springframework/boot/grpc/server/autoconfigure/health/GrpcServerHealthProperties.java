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

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.grpc.health.v1.HealthCheckResponse.ServingStatus;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.util.CollectionUtils;

/**
 * {@link ConfigurationProperties Properties} for Spring gRPC server health.
 *
 * @author Phillip Webb
 * @author Chris Bono
 * @since 4.1.0
 */
@ConfigurationProperties("spring.grpc.server.health")
public class GrpcServerHealthProperties {

	/**
	 * Whether to auto-configure Health feature on the gRPC server.
	 */
	private @Nullable Boolean enabled;

	/**
	 * Whether to include the overall server health.
	 */
	private boolean includeOverallHealth = true;

	/**
	 * Properties that apply to all services.
	 */
	private final Services services = new Services();

	/**
	 * Service specific health reporting.
	 */
	private final Map<String, Service> service = new LinkedHashMap<>();

	/**
	 * Status configuration.
	 */
	private final Status status = new Status();

	/**
	 * Schedule configuration.
	 */
	private final Schedule schedule = new Schedule();

	public @Nullable Boolean getEnabled() {
		return this.enabled;
	}

	public void setEnabled(@Nullable Boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isIncludeOverallHealth() {
		return this.includeOverallHealth;
	}

	public void setIncludeOverallHealth(boolean includeOverallHealth) {
		this.includeOverallHealth = includeOverallHealth;
	}

	public Services getServices() {
		return this.services;
	}

	public Map<String, Service> getService() {
		return this.service;
	}

	public Status getStatus() {
		return this.status;
	}

	public Schedule getSchedule() {
		return this.schedule;
	}

	/**
	 * Properties applying to all services.
	 */
	public static class Services {

		/**
		 * Whether to validate health membership for services on startup. Validation fails
		 * if a service includes or excludes a health contributor that does not exist.
		 */
		private boolean validateMembership = true;

		public boolean isValidateMembership() {
			return this.validateMembership;
		}

		public void setValidateMembership(boolean validateMembership) {
			this.validateMembership = validateMembership;
		}

	}

	/**
	 * A health for a specific service.
	 */
	public static class Service {

		/**
		 * Health indicator IDs that should be included or '*' for all.
		 */
		private @Nullable Set<String> include;

		/**
		 * Health indicator IDs that should be excluded or '*' for all.
		 */
		private @Nullable Set<String> exclude;

		/**
		 * Status configuration.
		 */
		@NestedConfigurationProperty
		private final Status status = new Status();

		public Status getStatus() {
			return this.status;
		}

		public @Nullable Set<String> getInclude() {
			return this.include;
		}

		public void setInclude(@Nullable Set<String> include) {
			this.include = include;
		}

		public @Nullable Set<String> getExclude() {
			return this.exclude;
		}

		public void setExclude(@Nullable Set<String> exclude) {
			this.exclude = exclude;
		}

	}

	/**
	 * Status properties for the group.
	 */
	public static class Status {

		/**
		 * List of health statuses in order of severity.
		 */
		private List<String> order = new ArrayList<>();

		/**
		 * Mapping of health statuses to gRPC service status. By default, registered
		 * health statuses map to sensible defaults (for example, UP maps to SERVING).
		 */
		private final Map<String, ServingStatus> mapping = new HashMap<>();

		public List<String> getOrder() {
			return this.order;
		}

		public void setOrder(List<String> statusOrder) {
			if (!CollectionUtils.isEmpty(statusOrder)) {
				this.order = statusOrder;
			}
		}

		public Map<String, ServingStatus> getMapping() {
			return this.mapping;
		}

	}

	/**
	 * Health task scheduling.
	 */
	public static class Schedule {

		/**
		 * Whether to schedule updates to gRPC server health based on application health.
		 */
		private boolean enabled = true;

		/**
		 * How often to update the health status.
		 */
		private Duration period = Duration.ofSeconds(5);

		/**
		 * The initial delay before updating the health status the very first time.
		 */
		private Duration delay = Duration.ofSeconds(5);

		public boolean isEnabled() {
			return this.enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public Duration getPeriod() {
			return this.period;
		}

		public void setPeriod(Duration period) {
			this.period = period;
		}

		public Duration getDelay() {
			return this.delay;
		}

		public void setDelay(Duration delay) {
			this.delay = delay;
		}

	}

}
