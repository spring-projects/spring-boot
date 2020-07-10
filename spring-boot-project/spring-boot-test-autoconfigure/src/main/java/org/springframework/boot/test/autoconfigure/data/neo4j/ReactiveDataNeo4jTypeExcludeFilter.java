/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.test.autoconfigure.data.neo4j;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.neo4j.driver.Driver;

import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.boot.test.autoconfigure.filter.StandardAnnotationCustomizableTypeExcludeFilter;
import org.springframework.data.neo4j.core.ReactiveNeo4jClient;
import org.springframework.data.neo4j.core.ReactiveNeo4jTemplate;
import org.springframework.data.neo4j.repository.ReactiveNeo4jRepository;

/**
 * {@link TypeExcludeFilter} for {@link ReactiveDataNeo4jTest @ReactiveDataNeo4jTest}.
 *
 * @author Michael J. Simons
 */
final class ReactiveDataNeo4jTypeExcludeFilter
		extends StandardAnnotationCustomizableTypeExcludeFilter<ReactiveDataNeo4jTest> {

	ReactiveDataNeo4jTypeExcludeFilter(Class<?> testClass) {
		super(testClass);
	}

	private static final Set<Class<?>> DEFAULT_INCLUDES;

	static {
		Set<Class<?>> includes = new LinkedHashSet<>();
		includes.add(Driver.class);
		includes.add(ReactiveNeo4jClient.class);
		includes.add(ReactiveNeo4jTemplate.class);
		includes.add(ReactiveNeo4jRepository.class);
		DEFAULT_INCLUDES = Collections.unmodifiableSet(includes);
	}

	@Override
	protected Set<Class<?>> getDefaultIncludes() {
		return DEFAULT_INCLUDES;
	}

}
