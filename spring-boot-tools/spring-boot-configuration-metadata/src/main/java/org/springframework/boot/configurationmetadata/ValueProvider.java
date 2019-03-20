/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.configurationmetadata;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Define a component that is able to provide the values of a property.
 * <p>
 * Each provider is defined by a {@code name} and can have an arbitrary number of
 * {@code parameters}. The available providers are defined in the Spring Boot
 * documentation.
 *
 * @author Stephane Nicoll
 * @since 1.3.0
 */
@SuppressWarnings("serial")
public class ValueProvider implements Serializable {

	private String name;

	private final Map<String, Object> parameters = new LinkedHashMap<String, Object>();

	/**
	 * Return the name of the provider.
	 * @return the name
	 */
	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Return the parameters.
	 * @return the parameters
	 */
	public Map<String, Object> getParameters() {
		return this.parameters;
	}

	@Override
	public String toString() {
		return "ValueProvider{" + "name='" + this.name + ", parameters=" + this.parameters
				+ '}';
	}

}
