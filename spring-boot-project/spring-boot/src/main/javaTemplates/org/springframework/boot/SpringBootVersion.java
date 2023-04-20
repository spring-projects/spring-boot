/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot;

/**
 * Exposes the Spring Boot version.
 *
 * @author Drummond Dawson
 * @author Hendrig Sellik
 * @author Andy Wilkinson
 * @author Scott Frederick
 * @since 1.3.0
 */
public final class SpringBootVersion {

	private SpringBootVersion() {
	}

	/**
	 * Return the full version string of the present Spring Boot codebase.
	 * @return the version of Spring Boot
	 */
	public static String getVersion() {
		return "${springBootVersion}";
	}

}
