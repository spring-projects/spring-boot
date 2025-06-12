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
import java.util.TreeMap;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import org.springframework.boot.actuate.endpoint.ApiVersion;
import org.springframework.boot.health.contributor.Status;
import org.springframework.util.Assert;

/**
 * Description of health that is composed of other {@link HealthDescriptor health
 * descriptors}.
 *
 * @author Phillip Webb
 * @since 4.0.0
 */
public sealed class CompositeHealthDescriptor extends HealthDescriptor permits SystemHealthDescriptor {

	private final ApiVersion apiVersion;

	private final Status status;

	private final Map<String, HealthDescriptor> components;

	CompositeHealthDescriptor(ApiVersion apiVersion, Status status, Map<String, HealthDescriptor> components) {
		Assert.notNull(apiVersion, "'apiVersion' must not be null");
		Assert.notNull(status, "'status' must not be null");
		this.apiVersion = apiVersion;
		this.status = status;
		this.components = (components != null) ? new TreeMap<>(components) : components;
	}

	@Override
	public Status getStatus() {
		return this.status;
	}

	@JsonInclude(Include.NON_EMPTY)
	public Map<String, HealthDescriptor> getComponents() {
		return (this.apiVersion == ApiVersion.V3) ? this.components : null;
	}

	@JsonInclude(Include.NON_EMPTY)
	public Map<String, HealthDescriptor> getDetails() {
		return (this.apiVersion == ApiVersion.V2) ? this.components : null;
	}

}
