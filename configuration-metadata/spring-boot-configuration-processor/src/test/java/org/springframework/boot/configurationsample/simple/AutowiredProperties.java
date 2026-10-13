/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.simple;

import org.springframework.boot.configurationsample.TestAutowired;

/**
 * Properties with autowired constructor.
 *
 * @author Madhura Bhave
 */
public class AutowiredProperties {

	/**
	 * The name of this simple properties.
	 */
	private String theName;

	@TestAutowired
	public AutowiredProperties(String theName) {
		this.theName = theName;
	}

	public String getTheName() {
		return this.theName;
	}

	public void setTheName(String name) {
		this.theName = name;
	}

}
