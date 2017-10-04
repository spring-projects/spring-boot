/*
 * Copyright 2012-2016 the original author or authors.
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

/**
 * A resolver for artifacts' Maven coordinates, allowing group id, artifact id, or version
 * to be obtained from a module identifier. A module identifier may be in the form
 * {@code groupId:artifactId:version}, in which case coordinate resolution simply extracts
 * the relevant piece from the identifier. Alternatively the identifier may be in the form
 * {@code artifactId}, in which case coordinate resolution uses implementation-specific
 * metadata to resolve the groupId and version.
 *
 * @author Andy Wilkinson
 */
public interface ArtifactCoordinatesResolver {

	/**
	 * Gets the group id of the artifact identified by the given {@code module}. Returns
	 * {@code null} if the artifact is unknown to the resolver.
	 * @param module The id of the module
	 * @return The group id of the module
	 */
	String getGroupId(String module);

	/**
	 * Gets the artifact id of the artifact identified by the given {@code module}.
	 * Returns {@code null} if the artifact is unknown to the resolver.
	 * @param module The id of the module
	 * @return The artifact id of the module
	 */
	String getArtifactId(String module);

	/**
	 * Gets the version of the artifact identified by the given {@code module}. Returns
	 * {@code null} if the artifact is unknown to the resolver.
	 * @param module The id of the module
	 * @return The version of the module
	 */
	String getVersion(String module);

}
