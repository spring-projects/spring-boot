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

package org.springframework.boot.docker.compose.service.connection.oracle;

/**
 * Enumeration of supported Oracle containers.
 *
 * @author Andy Wilkinson
 */
enum OracleContainer {

	FREE("gvenzl/oracle-free", "freepdb1"),

	XE("gvenzl/oracle-xe", "xepdb1");

	private final String imageName;

	private final String defaultDatabase;

	/**
	 * Constructs a new OracleContainer object with the specified image name and default
	 * database.
	 * @param imageName the name of the Oracle image to be used for the container
	 * @param defaultDatabase the name of the default database to be used for the
	 * container
	 */
	OracleContainer(String imageName, String defaultDatabase) {
		this.imageName = imageName;
		this.defaultDatabase = defaultDatabase;
	}

	/**
	 * Returns the name of the image.
	 * @return the name of the image
	 */
	String getImageName() {
		return this.imageName;
	}

	/**
	 * Returns the default database of the OracleContainer.
	 * @return the default database of the OracleContainer
	 */
	String getDefaultDatabase() {
		return this.defaultDatabase;
	}

}
