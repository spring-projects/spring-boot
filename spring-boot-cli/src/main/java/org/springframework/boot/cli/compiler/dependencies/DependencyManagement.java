/*
 * Copyright 2012-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.cli.compiler.dependencies;

import java.util.List;

/**
 * An encapsulation of dependency management information
 *
 * @author Andy Wilkinson
 * @since 1.3.0
 */
public interface DependencyManagement {

	/**
	 * Returns the managed dependencies.
	 *
	 * @return the managed dependencies
	 */
	List<Dependency> getDependencies();

	/**
	 * Returns the managed version of Spring Boot. May be {@code null}.
	 *
	 * @return the Spring Boot version, or {@code null}
	 */
	String getSpringBootVersion();

	/**
	 * Finds the managed dependency with the given {@code artifactId}.
	 *
	 * @param artifactId The artifact ID of the dependency to find
	 * @return the dependency, or {@code null}
	 */
	Dependency find(String artifactId);
}
