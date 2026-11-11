/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.batch.jdbc.autoconfigure.domain;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class City implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue
	@SuppressWarnings("NullAway.Init")
	private Long id;

	@Column(nullable = false)
	@SuppressWarnings("NullAway.Init")
	private String name;

	@Column(nullable = false)
	@SuppressWarnings("NullAway.Init")
	private String state;

	@Column(nullable = false)
	@SuppressWarnings("NullAway.Init")
	private String country;

	@Column(nullable = false)
	@SuppressWarnings("NullAway.Init")
	private String map;

	protected City() {
	}

	public City(String name, String state, String country, String map) {
		this.name = name;
		this.state = state;
		this.country = country;
		this.map = map;
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

	public String getMap() {
		return this.map;
	}

	@Override
	public String toString() {
		return getName() + "," + getState() + "," + getCountry();
	}

}
