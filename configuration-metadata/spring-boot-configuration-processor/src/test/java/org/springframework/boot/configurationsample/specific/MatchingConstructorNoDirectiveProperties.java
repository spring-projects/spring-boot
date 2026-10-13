/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.specific;

/**
 * Simple properties with a constructor but no binding directive.
 *
 * @author Stephane Nicoll
 */
public class MatchingConstructorNoDirectiveProperties {

	private String name;

	public MatchingConstructorNoDirectiveProperties(String name) {
		this.name = name;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
