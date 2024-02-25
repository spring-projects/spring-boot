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

package org.springframework.boot.info;

/**
 * Information about the Operating System the application is running on.
 *
 * @author Jonatan Ivanov
 * @since 2.7.0
 */
public class OsInfo {

	private final String name;

	private final String version;

	private final String arch;

	/**
	 * Constructs a new instance of the OsInfo class. Retrieves the name, version, and
	 * architecture of the operating system using the System.getProperty() method.
	 * @param name the name of the operating system
	 * @param version the version of the operating system
	 * @param arch the architecture of the operating system
	 */
	public OsInfo() {
		this.name = System.getProperty("os.name");
		this.version = System.getProperty("os.version");
		this.arch = System.getProperty("os.arch");
	}

	/**
	 * Returns the name of the OsInfo object.
	 * @return the name of the OsInfo object
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Returns the version of the operating system.
	 * @return the version of the operating system
	 */
	public String getVersion() {
		return this.version;
	}

	/**
	 * Returns the architecture of the operating system.
	 * @return the architecture of the operating system
	 */
	public String getArch() {
		return this.arch;
	}

}
