/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.mongodb.autoconfigure.domain.country;

import java.io.Serializable;

import org.springframework.data.mongodb.core.mapping.Document;

@Document
public class Country implements Serializable {

	private static final long serialVersionUID = 1L;

	@SuppressWarnings("NullAway.Init")
	private String name;

	protected Country() {
	}

	public Country(String name) {
		this.name = name;
	}

	public String getName() {
		return this.name;
	}

	@Override
	public String toString() {
		return getName();
	}

}
