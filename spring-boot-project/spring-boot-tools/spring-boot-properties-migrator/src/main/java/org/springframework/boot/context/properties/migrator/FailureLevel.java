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

/**
 * Level at which the properties migrator should fail the application startup.
 *
 * @author Akshay Dubey
 */
public enum FailureLevel {

	/**
	 * Do not fail on any property migration issues.
	 */
	NONE,

	/**
	 * Fail when unsupported properties (errors) are detected.
	 */
	ERROR,

	/**
	 * Fail when deprecated properties (warnings) or unsupported properties (errors) are
	 * detected.
	 */
	WARNING

}
