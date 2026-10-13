/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.generic;

import java.util.HashMap;
import java.util.Map;

/**
 * A base properties class with generics.
 *
 * @param <A> name type
 * @param <B> mapping key type
 * @param <C> mapping value type
 * @author Stephane Nicoll
 */
public class AbstractGenericProperties<A, B, C> {

	/**
	 * Generic name.
	 */
	private A name;

	/**
	 * Generic mappings.
	 */
	private final Map<B, C> mappings = new HashMap<>();

	public A getName() {
		return this.name;
	}

	public void setName(A name) {
		this.name = name;
	}

	public Map<B, C> getMappings() {
		return this.mappings;
	}

}
