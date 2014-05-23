/*
 * Copyright 2012-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.health;

/**
 * Base {@link HealthIndicator} implementations that encapsulates creation of
 * {@link Health} instance and error handling.
 * <p>
 * This implementation is only suitable if an {@link Exception} raised from
 * {@link #doHealthCheck()} should create a {@link Status#DOWN} health status.
 * 
 * @author Christian Dupuis
 * @since 1.1.0
 */
public abstract class AbstractHealthIndicator implements HealthIndicator {

	@Override
	public final Health health() {
		try {
			return doHealthCheck();
		}
		catch (Exception ex) {
			return Health.down(ex);
		}
	}

	/**
	 * Actual health check logic.
	 * @return the {@link Health}
	 * @throws Exception any {@link Exception} that should create a {@link Status#DOWN}
	 * system status.
	 */
	protected abstract Health doHealthCheck() throws Exception;
}
