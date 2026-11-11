/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.neo4j.domain.city;

import java.io.Serializable;

import org.springframework.boot.data.neo4j.domain.country.Country;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

@Node
public class City implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue
	@SuppressWarnings("NullAway.Init")
	private Long id;

	@SuppressWarnings("NullAway.Init")
	private final String name;

	@SuppressWarnings("NullAway.Init")
	private String state;

	@SuppressWarnings("NullAway.Init")
	private final Country country;

	@SuppressWarnings("NullAway.Init")
	private String map;

	public City(String name, Country country) {
		this.name = name;
		this.country = country;
	}

	public String getName() {
		return this.name;
	}

	public String getState() {
		return this.state;
	}

	public Country getCountry() {
		return this.country;
	}

	public String getMap() {
		return this.map;
	}

	@Override
	public String toString() {
		return getName() + "," + getState() + "," + getCountry();
	}

}
