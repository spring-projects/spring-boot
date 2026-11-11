/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.json;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.springframework.boot.json.JsonWriter.MemberPath;
import org.springframework.boot.json.JsonWriter.NameProcessor;
import org.springframework.boot.json.JsonWriter.ValueProcessor;

/**
 * Internal record used to hold {@link NameProcessor} and {@link ValueProcessor}
 * instances.
 *
 * @author Phillip Webb
 * @param pathFilters the path filters
 * @param nameProcessors the name processors
 * @param valueProcessors the value processors
 */
record JsonWriterFiltersAndProcessors(List<Predicate<MemberPath>> pathFilters, List<NameProcessor> nameProcessors,
		List<ValueProcessor<?>> valueProcessors) {

	JsonWriterFiltersAndProcessors() {
		this(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
	}

}
