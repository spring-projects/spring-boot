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

package org.springframework.boot.cli.compiler;

/**
 * A resolver for artifacts' Maven coordinates, allowing a group id or version to be
 * obtained from an artifact ID.
 * 
 * @author Andy Wilkinson
 */
public interface ArtifactCoordinatesResolver {

	/**
	 * Gets the group id of the artifact identified by the given {@code artifactId}.
	 * Returns {@code null} if the artifact is unknown to the resolver.
	 * @param artifactId The id of the artifact
	 * @return The group id of the artifact
	 */
	String getGroupId(String artifactId);

	/**
	 * Gets the version of the artifact identified by the given {@code artifactId}.
	 * Returns {@code null} if the artifact is unknown to the resolver.
	 * @param artifactId The id of the artifact
	 * @return The version of the artifact
	 */
	String getVersion(String artifactId);
}
