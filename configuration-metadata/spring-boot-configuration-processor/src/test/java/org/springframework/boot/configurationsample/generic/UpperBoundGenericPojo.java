/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.generic;

import java.util.HashMap;
import java.util.Map;

/**
 * A pojo with a complex generic signature.
 *
 * @param <T> the generic type
 * @author Stephane Nicoll
 */
public class UpperBoundGenericPojo<T extends Enum<T>> {

	private final Map<T, String> mappings = new HashMap<>();

	public Map<T, String> getMappings() {
		return this.mappings;
	}

}
