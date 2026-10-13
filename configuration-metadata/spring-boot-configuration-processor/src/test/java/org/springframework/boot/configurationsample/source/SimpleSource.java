/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.source;

/**
 * Type with manual metadata. This illustrates the case where the type of a property is
 * defined in a separate class and source-based metadata cannot be discovered.
 */
public class SimpleSource {

	private String name;

	private String description;

	private String type;

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return this.description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getType() {
		return this.type;
	}

	public void setType(String type) {
		this.type = type;
	}

}
