package org.springframework.boot.configurationmetadata;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Define a component that is able to provide the values of a property.
 * <p>
 * Each provider is defined by a {@code name} and can have an arbitrary
 * number of {@code parameters}. The available providers are defined in
 * the Spring Boot documentation.
 *
 * @author Stephane Nicoll
 * @since 1.3.0
 */
public class ValueProvider {

	private String name;

	private final Map<String, Object> parameters = new LinkedHashMap<String, Object>();

	/**
	 * Return the name of the provider.
	 */
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Return the parameters.
	 */
	public Map<String, Object> getParameters() {
		return parameters;
	}

	@Override
	public String toString() {
		return "ValueProvider{" + "name='" + this.name + ", parameters=" + this.parameters +
				'}';
	}

}
