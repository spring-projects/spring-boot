/*
 * Copyright 2012-2026 the original author or authors.
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

package org.springframework.boot.context.properties.migrator;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the properties migrator.
 *
 * @author Akshay Dubey
 */
@ConfigurationProperties(prefix = "spring.tools.properties-migrator")
class PropertiesMigratorProperties {

	/**
	 * Level at which to fail application startup when property migration issues are
	 * detected. When set to 'error', the application will fail if unsupported properties
	 * are found. When set to 'warning', the application will fail if either deprecated or
	 * unsupported properties are found.
	 */
	private FailureLevel failOn = FailureLevel.NONE;

	public FailureLevel getFailOn() {
		return this.failOn;
	}

	public void setFailOn(FailureLevel failOn) {
		this.failOn = failOn;
	}

}
