/*
 * Copyright 2012-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
