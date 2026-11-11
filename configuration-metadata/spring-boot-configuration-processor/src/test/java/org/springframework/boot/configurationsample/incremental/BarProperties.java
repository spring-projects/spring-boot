/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.incremental;

import org.springframework.boot.configurationsample.TestConfigurationProperties;

@TestConfigurationProperties("bar")
public class BarProperties {

	private String name;

	private String description;

	/**
	 * A nice counter description.
	 */
	private Integer counter = 0;

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return this.description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Integer getCounter() {
		return this.counter;
	}

	public void setCounter(Integer counter) {
		this.counter = counter;
	}

}
