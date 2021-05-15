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

package org.springframework.boot.build.bom.bomr;

import java.util.SortedSet;

import org.springframework.boot.build.bom.bomr.version.DependencyVersion;

/**
 * Resolves the available versions for a module.
 *
 * @author Andy Wilkinson
 */
interface VersionResolver {

	/**
	 * Resolves the available versions for the module identified by the given
	 * {@code groupId} and {@code artifactId}.
	 * @param groupId module's group ID
	 * @param artifactId module's artifact ID
	 * @return the available versions
	 */
	SortedSet<DependencyVersion> resolveVersions(String groupId, String artifactId);

}
