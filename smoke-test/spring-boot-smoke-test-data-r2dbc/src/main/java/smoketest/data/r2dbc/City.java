/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.data.r2dbc;

import org.jspecify.annotations.Nullable;

import org.springframework.data.annotation.Id;

public class City {

	@Id
	private @Nullable Long id;

	@SuppressWarnings("NullAway.Init")
	private String name;

	@SuppressWarnings("NullAway.Init")
	private String state;

	@SuppressWarnings("NullAway.Init")
	private String country;

	protected City() {
	}

	public City(String name, String state, String country) {
		this.name = name;
		this.state = state;
		this.country = country;
	}

	public @Nullable Long getId() {
		return this.id;
	}

	public String getName() {
		return this.name;
	}

	public String getState() {
		return this.state;
	}

	public String getCountry() {
		return this.country;
	}

	@Override
	public String toString() {
		return getName() + "," + getState() + "," + getCountry();
	}

}
