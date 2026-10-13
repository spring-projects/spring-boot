/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.testing.springbootapplications.jsontests;

class VehicleDetails {

	private final String make;

	private final String model;

	VehicleDetails(String make, String model) {
		this.make = make;
		this.model = model;
	}

	String getMake() {
		return this.make;
	}

	String getModel() {
		return this.model;
	}

}
