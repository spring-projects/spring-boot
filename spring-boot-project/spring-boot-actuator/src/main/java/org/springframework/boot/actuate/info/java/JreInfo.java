/*
 * Copyright 2021-2021 the original author or authors.
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

package org.springframework.boot.actuate.info.java;

/**
 * A simple DTO that holds information about the JRE (Java Runtime Environment) the application is running in.
 *
 * @author Jonatan Ivanov
 * @since 2.6.0
 */
public class JreInfo {
	private final String name;
	private final String version;

	public JreInfo() {
		this.name = System.getProperty("java.runtime.name");
		this.version = System.getProperty("java.runtime.version");
	}

	public String getName() {
		return this.name;
	}

	public String getVersion() {
		return this.version;
	}
}
