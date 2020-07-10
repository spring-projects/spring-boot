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

import org.springframework.boot.test.context.SpringBootTestContextBootstrapper;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.test.context.TestContextBootstrapper;

/**
 * {@link TestContextBootstrapper} for
 * {@link ReactiveDataNeo4jTest @ReactiveDataNeo4jTest} support.
 *
 * @author Michael J. Simons
 */
final class ReactiveDataNeo4jTestContextBootstrapper extends SpringBootTestContextBootstrapper {

	@Override
	protected String[] getProperties(Class<?> testClass) {
		String[] properties = MergedAnnotations.from(testClass, SearchStrategy.INHERITED_ANNOTATIONS)
				.get(ReactiveDataNeo4jTest.class).getValue("properties", String[].class).orElseGet(() -> new String[0]);

		String[] finalProperties = new String[properties.length + 1];
		System.arraycopy(properties, 0, finalProperties, 0, properties.length);
		finalProperties[finalProperties.length - 1] = "spring.data.neo4j.repositories.type=reactive";
		return finalProperties;
	}

}
