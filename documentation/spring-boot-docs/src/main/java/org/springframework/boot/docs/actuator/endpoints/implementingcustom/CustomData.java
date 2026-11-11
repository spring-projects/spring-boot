/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.actuator.endpoints.implementingcustom;

class CustomData {

	private final String name;

	private final int counter;

	CustomData(String name, int counter) {
		this.name = name;
		this.counter = counter;
	}

	String getName() {
		return this.name;
	}

	int getCounter() {
		return this.counter;
	}

}
