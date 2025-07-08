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

package org.springframework.boot.actuate.health;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.health.contributor.ReactiveHealthIndicator;
import org.springframework.boot.health.contributor.Status;

/**
 * Description of health obtained from a {@link HealthIndicator} or
 * {@link ReactiveHealthIndicator}.
 *
 * @author Phillip Webb
 * @since 4.0.0
 */
public final class IndicatedHealthDescriptor extends HealthDescriptor {

	static final IndicatedHealthDescriptor UP = new IndicatedHealthDescriptor(Health.up().build());

	private final Health health;

	IndicatedHealthDescriptor(Health health) {
		this.health = health;
	}

	@Override
	public Status getStatus() {
		return this.health.getStatus();
	}

	@JsonInclude(Include.NON_EMPTY)
	public Map<String, Object> getDetails() {
		return this.health.getDetails();
	}

}
