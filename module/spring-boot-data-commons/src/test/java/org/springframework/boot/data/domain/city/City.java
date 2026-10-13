/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.domain.city;

import org.jspecify.annotations.Nullable;

import org.springframework.data.annotation.Id;

public class City {

	@Id
	private @Nullable Long id;

	private String name;

	private String country;

	public City(String name, String country) {
		this.name = name;
		this.country = country;
	}

	public String getName() {
		return this.name;
	}

	public String getCountry() {
		return this.country;
	}

	@Override
	public String toString() {
		return getName() + "," + getCountry();
	}

}
