/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.simple;

/**
 * Parent for {@link HierarchicalProperties}.
 *
 * @author Stephane Nicoll
 */
public abstract class HierarchicalPropertiesParent extends HierarchicalPropertiesGrandparent {

	/**
	 * Parent property.
	 */
	private String second = "two";

	public String getSecond() {
		return this.second;
	}

	public void setSecond(String second) {
		this.second = second;
	}

	// Overridden properties should belong to this class, not
	// HierarchicalPropertiesGrandparent

	@Override
	public String getFirst() {
		return super.getFirst();
	}

	@Override
	public void setFirst(String first) {
		super.setFirst(first);
	}

}
