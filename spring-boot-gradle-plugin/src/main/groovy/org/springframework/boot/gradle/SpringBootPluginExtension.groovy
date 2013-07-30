/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot.gradle

/**
 * Gradle DSL Extension for 'Spring Boot'.
 *
 * @author Phillip Webb
 */
public class SpringBootPluginExtension {

	/**
	 * The main class that should be run. If not specified the value from the
	 * MANIFEST will be used, or if no manifest entry is the archive will be
	 * searched for a suitable class.
	 */
	String mainClass

	/**
	 * The name of the provided configuration. If not specified 'providedRuntime' will
	 * be used.
	 */
	String providedConfiguration

	/**
	 * If the original source archive should be backed-up before being repackaged.
	 */
	boolean backupSource = true;
}
