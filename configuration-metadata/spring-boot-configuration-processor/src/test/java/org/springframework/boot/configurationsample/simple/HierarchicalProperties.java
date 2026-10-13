/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.simple;

import org.springframework.boot.configurationsample.TestConfigurationProperties;

/**
 * Configuration properties with inherited values.
 *
 * @author Stephane Nicoll
 */
@TestConfigurationProperties("hierarchical")
public class HierarchicalProperties extends HierarchicalPropertiesParent {

	/**
	 * Concrete property.
	 */
	private String third = "three";

	public String getThird() {
		return this.third;
	}

	public void setThird(String third) {
		this.third = third;
	}

}
