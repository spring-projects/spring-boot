/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.testcontainers.properties;

import java.util.function.Supplier;

import org.springframework.boot.testcontainers.lifecycle.BeforeTestcontainerUsedEvent;

/**
 * Event published just before the {@link Supplier value supplier} of a
 * {@link TestcontainersPropertySource} property is called.
 *
 * @author Phillip Webb
 * @since 3.2.2
 * @deprecated since 3.2.6 for removal in 3.4.0 in favor of
 * {@link BeforeTestcontainerUsedEvent}
 */
@Deprecated(since = "3.2.6", forRemoval = true)
public class BeforeTestcontainersPropertySuppliedEvent extends BeforeTestcontainerUsedEvent {

	private final String propertyName;

	BeforeTestcontainersPropertySuppliedEvent(TestcontainersPropertySource source, String propertyName) {
		super(source);
		this.propertyName = propertyName;
	}

	/**
	 * Return the name of the property about to be supplied.
	 * @return the propertyName the property name
	 */
	public String getPropertyName() {
		return this.propertyName;
	}

}
