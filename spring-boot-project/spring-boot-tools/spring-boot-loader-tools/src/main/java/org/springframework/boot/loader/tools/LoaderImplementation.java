/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.loader.tools;

/**
 * Supported loader implementations.
 *
 * @author Phillip Webb
 * @since 3.2.0
 */
public enum LoaderImplementation {

	/**
	 * The default recommended loader implementation.
	 */
	DEFAULT("META-INF/loader/spring-boot-loader.jar"),

	/**
	 * The classic loader implementation as used with Spring Boot 3.1 and earlier.
	 */
	CLASSIC("META-INF/loader/spring-boot-loader-classic.jar");

	private final String jarResourceName;

	LoaderImplementation(String jarResourceName) {
		this.jarResourceName = jarResourceName;
	}

	/**
	 * Return the name of the nested resource that can be loaded from the tools jar.
	 * @return the jar resource name
	 */
	public String getJarResourceName() {
		return this.jarResourceName;
	}

}
