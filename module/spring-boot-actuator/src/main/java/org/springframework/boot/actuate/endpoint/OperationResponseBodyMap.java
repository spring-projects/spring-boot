/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.endpoint;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * {@link LinkedHashMap} to support {@link OperationResponseBody#of(java.util.Map)}.
 *
 * @param <K> the key type
 * @param <V> the value type
 * @author Phillip Webb
 */
class OperationResponseBodyMap<K, V> extends LinkedHashMap<K, V> implements OperationResponseBody {

	OperationResponseBodyMap(Map<? extends K, ? extends V> map) {
		super(map);
	}

}
