/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.elasticsearch.domain.city;

import java.io.Serializable;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Setting;

@Document(indexName = "city")
@Setting(shards = 1, replicas = 0, refreshInterval = "-1")
public class City implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@SuppressWarnings("NullAway.Init")
	private Long id;

	@SuppressWarnings("NullAway.Init")
	private String name;

	@SuppressWarnings("NullAway.Init")
	private String state;

	@SuppressWarnings("NullAway.Init")
	private String country;

	@SuppressWarnings("NullAway.Init")
	private String map;

	protected City() {
	}

	public City(String name, String country) {
		this.name = name;
		this.country = country;
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
