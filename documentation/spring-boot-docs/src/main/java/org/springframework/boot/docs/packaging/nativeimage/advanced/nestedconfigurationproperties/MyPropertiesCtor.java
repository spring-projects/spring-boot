/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.packaging.nativeimage.advanced.nestedconfigurationproperties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@ConfigurationProperties("my.properties")
public class MyPropertiesCtor {

	private final String name;

	@NestedConfigurationProperty
	private final Nested nested;

	public MyPropertiesCtor(String name, Nested nested) {
		this.name = name;
		this.nested = nested;
	}

	// @fold:on // getters / setters...
	public String getName() {
		return this.name;
	}

	public Nested getNested() {
		return this.nested;
	}
	// @fold:off

}
