/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.actuate.availability;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health.Builder;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.AvailabilityState;
import org.springframework.util.Assert;

/**
 * A {@link HealthIndicator} that checks a specific {@link AvailabilityState} of the
 * application.
 *
 * @author Phillip Webb
 * @author Brian Clozel
 * @since 2.3.0
 */
public class AvailabilityStateHealthIndicator extends AbstractHealthIndicator {

	private final ApplicationAvailability applicationAvailability;

	private final Class<? extends AvailabilityState> stateType;

	private final Map<AvailabilityState, Status> statusMappings = new HashMap<>();

	/**
	 * Create a new {@link AvailabilityStateHealthIndicator} instance.
	 * @param <S> the availability state type
	 * @param applicationAvailability the application availability
	 * @param stateType the availability state type
	 * @param statusMappings consumer used to setup the status mappings
	 */
	public <S extends AvailabilityState> AvailabilityStateHealthIndicator(
			ApplicationAvailability applicationAvailability, Class<S> stateType,
			Consumer<StatusMappings<S>> statusMappings) {
		Assert.notNull(applicationAvailability, "ApplicationAvailability must not be null");
		Assert.notNull(stateType, "StateType must not be null");
		Assert.notNull(statusMappings, "StatusMappings must not be null");
		this.applicationAvailability = applicationAvailability;
		this.stateType = stateType;
		statusMappings.accept(this.statusMappings::put);
		assertAllEnumsMapped(stateType);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private <S extends AvailabilityState> void assertAllEnumsMapped(Class<S> stateType) {
		if (!this.statusMappings.containsKey(null) && Enum.class.isAssignableFrom(stateType)) {
			EnumSet elements = EnumSet.allOf((Class) stateType);
			for (Object element : elements) {
				Assert.isTrue(this.statusMappings.containsKey(element),
						() -> "StatusMappings does not include " + element);
			}
		}
	}

	@Override
	protected void doHealthCheck(Builder builder) throws Exception {
		AvailabilityState state = getState(this.applicationAvailability);
		Status status = this.statusMappings.get(state);
		if (status == null) {
			status = this.statusMappings.get(null);
		}
		Assert.state(status != null, () -> "No mapping provided for " + state);
		builder.status(status);
	}

	/**
	 * Return the current availability state. Subclasses can override this method if a
	 * different retrieval mechanism is needed.
	 * @param applicationAvailability the application availability
	 * @return the current availability state
	 */
	protected AvailabilityState getState(ApplicationAvailability applicationAvailability) {
		return applicationAvailability.getState(this.stateType);
	}

	/**
	 * Callback used to add status mappings.
	 *
	 * @param <S> the availability state type
	 */
	public interface StatusMappings<S extends AvailabilityState> {

		/**
		 * Add the status that should be used if no explicit mapping is defined.
		 * @param status the default status
		 */
		default void addDefaultStatus(Status status) {
			add(null, status);
		}

		/**
		 * Add a new status mapping .
		 * @param availabilityState the availability state
		 * @param status the mapped status
		 */
		void add(S availabilityState, Status status);

	}

}
