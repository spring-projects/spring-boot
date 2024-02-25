/*
 * Copyright 2012-2021 the original author or authors.
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
import com.fasterxml.jackson.annotation.JsonProperty;

import org.springframework.boot.actuate.endpoint.ApiVersion;
import org.springframework.util.Assert;

/**
 * A {@link HealthComponent} that is composed of other {@link HealthComponent} instances.
 * Used to provide a unified view of related components. For example, a database health
 * indicator may be a composite containing the {@link Health} of each datasource
 * connection.
 *
 * @author Phillip Webb
 * @since 2.2.0
 */
public class CompositeHealth extends HealthComponent {

	private final Status status;

	private final Map<String, HealthComponent> components;

	private final Map<String, HealthComponent> details;

	/**
     * Constructs a new CompositeHealth object with the specified API version, status, and components.
     * 
     * @param apiVersion the API version of the health check
     * @param status the status of the health check
     * @param components the map of health components
     * @throws IllegalArgumentException if the status is null
     */
    CompositeHealth(ApiVersion apiVersion, Status status, Map<String, HealthComponent> components) {
		Assert.notNull(status, "Status must not be null");
		this.status = status;
		this.components = (apiVersion != ApiVersion.V3) ? null : sort(components);
		this.details = (apiVersion != ApiVersion.V2) ? null : sort(components);
	}

	/**
     * Sorts the given map of health components by their keys in ascending order.
     * 
     * @param components the map of health components to be sorted (can be null)
     * @return a new TreeMap containing the sorted health components, or the original map if it is null
     */
    private Map<String, HealthComponent> sort(Map<String, HealthComponent> components) {
		return (components != null) ? new TreeMap<>(components) : components;
	}

	/**
     * Returns the status of the CompositeHealth object.
     *
     * @return the status of the CompositeHealth object
     */
    @Override
	public Status getStatus() {
		return this.status;
	}

	/**
     * Returns the components of the CompositeHealth.
     * 
     * @return a map containing the components of the CompositeHealth. The keys are the names of the components and the values are the HealthComponent objects.
     *         If the map is empty, an empty map will be returned.
     */
    @JsonInclude(Include.NON_EMPTY)
	public Map<String, HealthComponent> getComponents() {
		return this.components;
	}

	/**
     * Retrieves the details of the health components.
     * 
     * @return A map containing the details of the health components. The map is empty if there are no details available.
     */
    @JsonInclude(Include.NON_EMPTY)
	@JsonProperty
	public Map<String, HealthComponent> getDetails() {
		return this.details;
	}

}
