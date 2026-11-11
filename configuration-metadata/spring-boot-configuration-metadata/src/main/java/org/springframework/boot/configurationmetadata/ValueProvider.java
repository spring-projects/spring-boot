/*
 * Copyright 2012-present the original author or authors.
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

	private final Map<String, Object> parameters = new LinkedHashMap<>();

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
		return "ValueProvider{name='" + this.name + ", parameters=" + this.parameters + '}';
	}

}
