/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.packaging.nativeimage.advanced.nestedconfigurationproperties;

public class Nested {

	private int number;

	// @fold:on // getters / setters...
	public int getNumber() {
		return this.number;
	}

	public void setNumber(int number) {
		this.number = number;
	}
	// @fold:off

}
