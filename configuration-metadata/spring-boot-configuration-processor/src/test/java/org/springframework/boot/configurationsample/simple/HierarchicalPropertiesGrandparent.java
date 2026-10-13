/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.simple;

/**
 * Grandparent for {@link HierarchicalProperties}.
 *
 * @author Stephane Nicoll
 */
public abstract class HierarchicalPropertiesGrandparent {

	/**
	 * Grandparent property.
	 */
	private String first = "one";

	public String getFirst() {
		return this.first;
	}

	public void setFirst(String first) {
		this.first = first;
	}

}
