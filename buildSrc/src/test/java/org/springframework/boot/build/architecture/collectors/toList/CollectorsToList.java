/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.architecture.collectors.toList;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class CollectorsToList {

	void exampleMethod() {
		List<String> strings = Stream.of("a", "b", "c").collect(Collectors.toList());
	}

}
