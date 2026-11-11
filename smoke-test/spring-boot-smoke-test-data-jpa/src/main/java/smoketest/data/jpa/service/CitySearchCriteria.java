/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.data.jpa.service;

import java.io.Serializable;

import org.springframework.util.Assert;

public class CitySearchCriteria implements Serializable {

	private static final long serialVersionUID = 1L;

	private final String name;

	public CitySearchCriteria(String name) {
		Assert.notNull(name, "'name' must not be null");
		this.name = name;
	}

	public String getName() {
		return this.name;
	}

}
